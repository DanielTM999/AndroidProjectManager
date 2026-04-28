package dtm.dependencymanager.containers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import dtm.dependencymanager.core.InjectionStrategy;
import dtm.dependencymanager.exceptions.DependencyContainerException;
import dtm.dependencymanager.exceptions.DependencyContainerRuntimeException;
import dtm.dependencymanager.exceptions.DependencyInjectionException;
import dtm.dependencymanager.exceptions.InvalidClassRegistrationException;
import dtm.dependencymanager.exceptions.NewInstanceException;
import dtm.dependencymanager.exceptions.UnloadError;
import dtm.dependencymanager.internal.DependencyObject;
import dtm.dependencymanager.internal.Lazy;
import dtm.dependencymanager.internal.LazyObject;
import dtm.dependencymanager.internal.ServiceBeen;
import dtm.dependencymanager.internal.StaticContainer;
import dtm.dependencymanager.core.DependencyContainer;
import dtm.dependencymanager.core.FunctionRegistrationResult;
import dtm.dependencymanager.core.activity.ContextHolderActivity;
import dtm.dependencymanager.core.prototypes.Dependency;
import dtm.dependencymanager.core.prototypes.LazyDependency;
import dtm.dependencymanager.annotations.Inject;
import dtm.dependencymanager.annotations.Qualifier;
import dtm.dependencymanager.annotations.Singleton;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class DependencyContainerStorage implements DependencyContainer {
    private static final String TAG = "DependencyContainer";
    private static final String DEFAULT_QUALIFIER = "default";

    private final ExecutorService mainExecutor;
    private final ConcurrentMap<Class<?>, ConcurrentMap<String, Dependency>> dependencyContainer;
    private final Set<Class<?>> loadedClasses;
    private final List<ServiceBeen> serviceBeensDefinition;
    private final List<Set<ServiceBeen>> serviceBeensDefinitionLayer;
    private final AtomicBoolean loaded;
    private final AtomicReference<InjectionStrategy> injectionStrategy;

    private final ConcurrentMap<Class<?>, List<Field>> injectableFieldsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, String> classQualifierCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Field, String> fieldQualifierCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Parameter, String> paramQualifierCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Boolean> singletonCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Constructor<?>[]> constructorsCache = new ConcurrentHashMap<>();

    private volatile boolean childrenRegistration;
    private volatile boolean log;
    private static final boolean PROCESS_IN_LAYER = true;

    private DependencyContainerStorage() {
        this.loaded = new AtomicBoolean(false);
        this.childrenRegistration = false;
        this.injectionStrategy = new AtomicReference<>(InjectionStrategy.ADAPTIVE);
        this.dependencyContainer = new ConcurrentHashMap<>();
        this.loadedClasses = ConcurrentHashMap.newKeySet();
        this.serviceBeensDefinition = Collections.synchronizedList(new ArrayList<>());
        this.serviceBeensDefinitionLayer = Collections.synchronizedList(new ArrayList<>());
        int pool = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.mainExecutor = Executors.newFixedThreadPool(pool, r -> {
            Thread t = new Thread(r, "DI-Worker-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }

    public static DependencyContainerStorage getInstance() {
        return StaticContainer.getOrCreate(DependencyContainerStorage::new);
    }

    public ExecutorService getExecutor() {
        return mainExecutor;
    }

    @Override
    public boolean isLoaded() {
        return loaded.get();
    }

    @Override
    public void load() throws InvalidClassRegistrationException {
        if (isLoaded()) return;
        filterServiceClass();
        loadBeens();
        loaded.set(true);
    }

    @Override
    public void injectDependencies(Object instance) {
        injectDependenciesInternal(instance, true);
    }

    @Override
    public <T> T getDependency(Class<T> reference) {
        return getDependency(reference, getQualifierName(reference));
    }

    @Override
    public <T> T getDependency(Class<T> reference, String referenceName) {
        return getDependencyInternal(reference, referenceName, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> referenceClass) throws NewInstanceException {
        throwIfUnload();
        try {
            return (T) createObject(referenceClass);
        } catch (Exception e) {
            throw new NewInstanceException(e.getMessage(), referenceClass, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> referenceClass, Object... args) throws NewInstanceException {
        throwIfUnload();
        try {
            return (T) createObject(referenceClass, args, true);
        } catch (RuntimeException e) {
            Throwable th = (e.getCause() != null) ? e.getCause() : e;
            if (th instanceof NewInstanceException nie) throw nie;
            throw new NewInstanceException(th.getMessage(), referenceClass, e);
        }
    }

    @Override
    public void registerDependency(@NonNull Object dependency, @NonNull String qualifier) throws InvalidClassRegistrationException {
        registerInstance(dependency, qualifier, false);
    }

    @Override
    public void registerDependency(Object dependency, String qualifier, boolean replace) throws InvalidClassRegistrationException {
        registerInstance(dependency, qualifier, replace);
    }

    @Override
    public void registerDependency(@NonNull Object dependency) throws InvalidClassRegistrationException {
        registerInstance(dependency, DEFAULT_QUALIFIER, false);
    }

    @Override
    public void registerDependency(Object dependency, boolean replace) throws InvalidClassRegistrationException {
        registerInstance(dependency, DEFAULT_QUALIFIER, replace);
    }

    @Override
    public void registerDependency(@NonNull Class<?> dependency) throws InvalidClassRegistrationException {
        registerClass(dependency, false);
    }

    @Override
    public void registerDependency(Class<?> dependency, boolean replace) throws InvalidClassRegistrationException {
        registerClass(dependency, replace);
    }

    @Override
    public <T> void registerDependency(FunctionRegistrationResult<T> action) throws InvalidClassRegistrationException {
        registerFunction(action, false);
    }

    @Override
    public void overrideDependency(Object dependency) throws InvalidClassRegistrationException {
        registerInstance(dependency, DEFAULT_QUALIFIER, true);
    }

    @Override
    public void overrideDependency(Object dependency, String qualifier) throws InvalidClassRegistrationException {
        registerInstance(dependency, qualifier, true);
    }

    @Override
    public void overrideDependency(Class<?> dependency) throws InvalidClassRegistrationException {
        registerClass(dependency, true);
    }

    @Override
    public <T> void overrideDependency(FunctionRegistrationResult<T> registrationFunction) throws InvalidClassRegistrationException {
        registerFunction(registrationFunction, true);
    }

    @Override
    public void unRegisterDependency(Class<?> clazzDependency) {
        throwIfUnload();
        Map<String, Dependency> existing = dependencyContainer.get(clazzDependency);
        if (existing == null) return;
        Set<Dependency> toRemove = new HashSet<>(existing.values());
        for (Dependency dep : toRemove) {
            removeAllReferences(dep);
        }
    }

    private void removeAllReferences(Dependency dep) {
        Class<?> clazz = dep.getDependencyClass();
        String qualifier = dep.getQualifier();
        Set<Class<?>> keysToVisit = new HashSet<>();
        keysToVisit.add(clazz);
        keysToVisit.addAll(dep.getDependencyClassInstanceTypes());
        for (Class<?> key : keysToVisit) {
            ConcurrentMap<String, Dependency> bucket = dependencyContainer.get(key);
            if (bucket == null) continue;
            bucket.remove(qualifier, dep);
            if (bucket.isEmpty()) {
                dependencyContainer.remove(key, bucket);
            }
        }
    }

    @Override
    public void enableChildrenRegistration() { this.childrenRegistration = true; }

    @Override
    public void disableChildrenRegistration() { this.childrenRegistration = false; }

    @Override
    public void enableLog() { this.log = true; }

    @Override
    public void disableLog() { this.log = false; }

    @Override
    public List<Dependency> getRegisteredDependencies() {
        Set<Dependency> distinct = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (ConcurrentMap<String, Dependency> bucket : dependencyContainer.values()) {
            distinct.addAll(bucket.values());
        }
        return new ArrayList<>(distinct);
    }

    @Override
    public void setInjectionStrategy(InjectionStrategy strategy) {
        this.injectionStrategy.set((strategy != null) ? strategy : InjectionStrategy.ADAPTIVE);
    }

    private void registerInstance(@NonNull Object dependency, @NonNull String qualifier, boolean replace) throws InvalidClassRegistrationException {
        try {
            final Class<?> clazz = dependency.getClass();
            final String q = normalizeQualifier(qualifier);
            DependencyObject newDep = new DependencyObject(clazz, q, true, () -> dependency, dependency);
            putDependency(clazz, q, newDep, replace);
        } catch (DependencyContainerRuntimeException dce) {
            throw new InvalidClassRegistrationException(dce.getMessage(), dependency.getClass(), dce);
        } catch (Exception e) {
            throw new InvalidClassRegistrationException(
                    "Erro ao registrar dependencia: " + dependency.getClass() + " ==> " + e.getMessage(),
                    dependency.getClass(), e);
        }
    }

    private void registerClass(@NonNull Class<?> dependency, boolean replace) throws InvalidClassRegistrationException {
        if (!isLoaded()) {
            loadedClasses.add(dependency);
            return;
        }
        try {
            Object instance = newInstance(dependency);
            registerInstance(instance, getQualifierName(dependency), replace);
        } catch (NewInstanceException e) {
            throw new InvalidClassRegistrationException(e.getMessage(), e.getReferenceClass(), e);
        }
    }

    private void registerFunction(@NonNull FunctionRegistrationResult<?> action, boolean replace) throws InvalidClassRegistrationException {
        final Class<?> referenceClass = action.getReferenceClass();
        final String q = normalizeQualifier(action.getQualifier());
        try {
            final Supplier<?> activator = action.getFunction();
            DependencyObject dep = new DependencyObject(referenceClass, q, true, activator, null);
            putDependency(referenceClass, q, dep, replace);
        } catch (DependencyContainerRuntimeException dce) {
            throw new InvalidClassRegistrationException(dce.getMessage(), referenceClass, dce);
        } catch (Exception e) {
            throw new InvalidClassRegistrationException(
                    "Erro ao registrar dependencia: " + referenceClass + " ==> " + e.getMessage(),
                    referenceClass, e);
        }
    }

    private void registerLoadedClass(@NonNull Class<?> dependency, @NonNull Set<Class<?>> registeringClasses) throws InvalidClassRegistrationException {
        try {
            if (dependency.isEnum() || dependency.isInterface()) {
                throw new DependencyContainerRuntimeException("Registre uma classe concreta: " + dependency);
            }
            final String qualifier = getQualifierName(dependency);
            ConcurrentMap<String, Dependency> bucket = dependencyContainer.get(dependency);
            if (bucket != null && bucket.containsKey(qualifier)) return;

            if (registeringClasses.contains(dependency)) {
                throw new InvalidClassRegistrationException("Dependência circular detectada: " + dependency.getName(), dependency);
            }
            registeringClasses.add(dependency);

            if (childrenRegistration) {
                registerAutoInject(dependency);
            }

            DependencyObject newDep = isSingleton(dependency)
                    ? new DependencyObject(dependency, qualifier, true, null, createObject(dependency))
                    : new DependencyObject(dependency, qualifier, false, createActivationFunction(dependency), null);
            putDependency(dependency, qualifier, newDep, false);
        } catch (DependencyContainerRuntimeException dce) {
            throw new InvalidClassRegistrationException(dce.getMessage(), dependency, dce);
        } catch (InvalidClassRegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidClassRegistrationException(
                    "Erro ao criar a dependencia: " + dependency + " ==> causa: " + e.getMessage(),
                    dependency, e);
        }
    }

    private void putDependency(Class<?> clazz, String qualifier, DependencyObject dep, boolean replace) {
        ConcurrentMap<String, Dependency> bucket = dependencyContainer
                .computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        Dependency previous = bucket.get(qualifier);
        if (previous != null && !replace) {
            throw new DependencyContainerRuntimeException(
                    "Qualificador '" + qualifier + "' ja registrado para a dependencia: " + clazz);
        }
        if (previous != null) {
            removeAllReferences(previous);
            bucket = dependencyContainer.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        }
        bucket.put(qualifier, dep);
        registerSubTypes(clazz, qualifier, dep);
    }

    private void registerSubTypes(@NonNull Class<?> clazz, @NonNull String qualifier, @NonNull Dependency dep) {
        if (clazz.equals(Object.class) || clazz.isInterface()) return;

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class) && !superClass.isInterface()) {
            dependencyContainer
                    .computeIfAbsent(superClass, k -> new ConcurrentHashMap<>())
                    .putIfAbsent(qualifier, dep);
        }
        for (Class<?> ifc : clazz.getInterfaces()) {
            if (!ifc.equals(Object.class)) {
                dependencyContainer
                        .computeIfAbsent(ifc, k -> new ConcurrentHashMap<>())
                        .putIfAbsent(qualifier, dep);
                if (log) {
                    Log.d(TAG, "registerSubTypes -> " + ifc.getName() + "[" + qualifier + "] = " + clazz.getName());
                }
            }
        }
    }

    private void registerAutoInject(@NonNull Class<?> clazz) throws InvalidClassRegistrationException {
        for (Field f : getInjectableFields(clazz)) {
            Class<?> type = extractType(f).getClazz();
            if (type == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) continue;
            if (!dependencyContainer.containsKey(type)) {
                registerClass(type, false);
            }
        }
    }

    private Object createObject(@NonNull Class<?> clazz) {
        return createObject(clazz, new Object[0], false);
    }

    private Object createObject(@NonNull Class<?> clazz, Object[] extraConstructorArgs, boolean throwError) {
        try {
            Constructor<?>[] constructors = constructorsCache.computeIfAbsent(clazz, Class::getDeclaredConstructors);
            Constructor<?>[] sorted = constructors.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(b.getParameterCount(), a.getParameterCount()));

            List<Parameter> failedParams = new ArrayList<>();
            for (Constructor<?> constructor : sorted) {
                List<Parameter> localFails = new ArrayList<>();
                Object[] resolvedArgs = tryResolveConstructorArgs(constructor.getParameters(), extraConstructorArgs, localFails);
                if (resolvedArgs != null) {
                    constructor.setAccessible(true);
                    Object instance = constructor.newInstance(resolvedArgs);
                    injectDependenciesInternal(Objects.requireNonNull(instance), false);
                    return instance;
                }
                failedParams = localFails;
            }
            String message = !failedParams.isEmpty()
                    ? "Falha ao instanciar " + clazz.getName() + ". Parâmetros não resolvidos: "
                        + failedParams.stream().map(p -> p.getName() + ":" + p.getType().getName()).collect(Collectors.joining(", "))
                    : "Sem construtor aplicável encontrado para " + clazz.getName();
            throw new NewInstanceException(message, clazz);
        } catch (NewInstanceException e) {
            if (log) Log.e(TAG, "createObject failed: " + clazz, e);
            if (throwError) throw new RuntimeException(e);
            return null;
        } catch (Exception e) {
            if (log) Log.e(TAG, "Erro ao criar Objeto " + clazz + " ==> " + e.getMessage(), e);
            if (throwError) throw new RuntimeException(e);
            return null;
        }
    }

    private Object[] tryResolveConstructorArgs(Parameter[] parameters, Object[] extraArgs, List<Parameter> failedParams) {
        Object[] args = new Object[parameters.length];
        List<Object> extras = new ArrayList<>();
        if (extraArgs != null) Collections.addAll(extras, extraArgs);

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> paramType = parameter.getType();

            Object matchedExtra = null;
            for (int j = 0; j < extras.size(); j++) {
                Object cand = extras.get(j);
                if (cand != null && paramType.isAssignableFrom(cand.getClass())) {
                    matchedExtra = cand;
                    extras.remove(j);
                    break;
                }
            }

            if (matchedExtra != null) {
                args[i] = matchedExtra;
            } else {
                Object injected = getDependecyObjectByParam(parameter);
                if (injected == null) {
                    if (failedParams != null) failedParams.add(parameter);
                    return null;
                }
                args[i] = injected;
            }
        }
        return args;
    }

    private Supplier<Object> createActivationFunction(@NonNull Class<?> clazz) {
        return () -> createObject(clazz);
    }

    private boolean isSingleton(@NonNull Class<?> clazz) {
        return singletonCache.computeIfAbsent(clazz, c -> c.isAnnotationPresent(Singleton.class));
    }

    private String normalizeQualifier(String q) {
        return (q == null || q.isEmpty()) ? DEFAULT_QUALIFIER : q;
    }

    private String getQualifierName(@NonNull Class<?> clazz) {
        return classQualifierCache.computeIfAbsent(clazz, c -> {
            if (c.isAnnotationPresent(Qualifier.class)) {
                Qualifier q = c.getAnnotation(Qualifier.class);
                return (q == null || q.qualifier() == null || q.qualifier().isEmpty()) ? DEFAULT_QUALIFIER : q.qualifier();
            }
            return DEFAULT_QUALIFIER;
        });
    }

    private String getQualifierName(@NonNull Field variable) {
        return fieldQualifierCache.computeIfAbsent(variable, f -> {
            if (f.isAnnotationPresent(Qualifier.class)) {
                Qualifier q = f.getAnnotation(Qualifier.class);
                return (q == null || q.qualifier() == null || q.qualifier().isEmpty()) ? DEFAULT_QUALIFIER : q.qualifier();
            }
            if (f.isAnnotationPresent(Inject.class)) {
                Inject inj = f.getAnnotation(Inject.class);
                return (inj == null || inj.qualifier() == null || inj.qualifier().isEmpty()) ? DEFAULT_QUALIFIER : inj.qualifier();
            }
            return DEFAULT_QUALIFIER;
        });
    }

    private String getQualifierName(@NonNull Parameter variable) {
        return paramQualifierCache.computeIfAbsent(variable, p -> {
            if (p.isAnnotationPresent(Qualifier.class)) {
                Qualifier q = p.getAnnotation(Qualifier.class);
                return (q == null || q.qualifier() == null || q.qualifier().isEmpty()) ? DEFAULT_QUALIFIER : q.qualifier();
            }
            if (p.isAnnotationPresent(Inject.class)) {
                Inject inj = p.getAnnotation(Inject.class);
                return (inj == null || inj.qualifier() == null || inj.qualifier().isEmpty()) ? DEFAULT_QUALIFIER : inj.qualifier();
            }
            return DEFAULT_QUALIFIER;
        });
    }

    private List<Field> getInjectableFields(Class<?> clazz) {
        return injectableFieldsCache.computeIfAbsent(clazz, c -> {
            List<Field> list = new ArrayList<>();
            Class<?> current = c;
            while (current != null && !current.equals(Object.class)) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Inject.class)) {
                        if (!field.isAccessible()) field.setAccessible(true);
                        list.add(field);
                    }
                }
                current = current.getSuperclass();
            }
            return Collections.unmodifiableList(list);
        });
    }

    private void injectVariable(Field variable, Object instance) {
        try {
            final LazyObject lazyObject = extractType(variable);
            final Class<?> clazzVariable = lazyObject.getClazz();
            final boolean isLazy = lazyObject.isLazy();

            if (!variable.isAccessible()) variable.setAccessible(true);

            if (isClassOfContext(clazzVariable)) {
                if (!isLazy) {
                    variable.set(instance, ContextHolderActivity.getCurrentContext());
                } else {
                    Supplier<Context> actionInject = ContextHolderActivity::getCurrentContext;
                    variable.set(instance, Lazy.of(actionInject));
                }
            } else {
                if (!isLazy) {
                    Object targetInstance = getObjectToInjectVariable(variable, clazzVariable);
                    variable.set(instance, targetInstance);
                } else {
                    Supplier<Object> getDependecyLazy = () -> {
                        try {
                            return getObjectToInjectVariable(variable, clazzVariable);
                        } catch (Exception e) {
                            if (log) Log.e(TAG, "Erro lazy ao injetar " + variable.getName(), e);
                            return null;
                        }
                    };
                    variable.set(instance, Lazy.of(getDependecyLazy));
                }
            }
        } catch (Exception e) {
            if (log) Log.e(TAG, "Erro ao definir variavel " + variable.getName() + " ==> " + e.getMessage(), e);
        }
    }

    private Object getObjectToInjectVariable(Field variable, Class<?> clazzVariable) throws Exception {
        final String qualifierName = getQualifierName(variable);
        Dependency dep = lookupDependency(clazzVariable, qualifierName);
        if (dep == null && childrenRegistration) {
            try {
                registerClass(clazzVariable, false);
            } catch (InvalidClassRegistrationException e) {
                throw new DependencyContainerRuntimeException(e);
            }
            dep = lookupDependency(clazzVariable, qualifierName);
        }
        if (dep == null) {
            throw new DependencyContainerException(
                    "Dependencia não encontrada para: " + clazzVariable + "[" + qualifierName + "]");
        }
        return dep.getDependency();
    }

    private Dependency lookupDependency(Class<?> clazz, String qualifier) {
        ConcurrentMap<String, Dependency> bucket = dependencyContainer.get(clazz);
        if (bucket == null) return null;
        return bucket.get(qualifier);
    }

    private void filterServiceClass() {
        final int threshold = 50;
        final int total = loadedClasses.size();
        final Map<Class<?>, Set<Class<?>>> dependencyGraph = new ConcurrentHashMap<>();

        Log.i(TAG, "Iniciando análise de dependências. Total: " + total);

        if (total < threshold) {
            processDependencyWithParallelStream(dependencyGraph);
        } else {
            processDependencyWithExecutorService(dependencyGraph);
        }

        if (PROCESS_IN_LAYER) {
            List<Set<Class<?>>> classLayers = groupByDependencyLayer(loadedClasses, dependencyGraph);
            Log.i(TAG, "Camadas geradas: " + classLayers.size());
            int order = 0;
            for (Set<Class<?>> classSet : classLayers) {
                int layerOrder = order;
                Set<ServiceBeen> layer = ConcurrentHashMap.newKeySet();
                if (log) {
                    String classesInLayer = classSet.stream().map(Class::getSimpleName).collect(Collectors.joining(", "));
                    Log.d(TAG, "Camada [" + layerOrder + "]: " + classSet.size() + " -> [" + classesInLayer + "]");
                }
                for (Class<?> clazz : classSet) {
                    layer.add(new ServiceBeen(clazz, layerOrder));
                }
                serviceBeensDefinitionLayer.add(layer);
                order++;
            }
        } else {
            Set<Class<?>> ordered = topologicalSort(loadedClasses, dependencyGraph);
            int order = 0;
            for (Class<?> clazz : ordered) {
                serviceBeensDefinition.add(new ServiceBeen(clazz, order++));
            }
        }
    }

    private void loadBeens() throws InvalidClassRegistrationException {
        if (PROCESS_IN_LAYER) {
            loadBeensInlayer();
        } else {
            loadBeensTopological();
        }
    }

    private void loadBeensTopological() throws InvalidClassRegistrationException {
        for (ServiceBeen service : new ArrayList<>(serviceBeensDefinition)) {
            registerLoadedClass(service.getClazz(), new HashSet<>());
        }
    }

    private void loadBeensInlayer() throws InvalidClassRegistrationException {
        for (Set<ServiceBeen> layer : serviceBeensDefinitionLayer) {
            loadBeensInlayer(layer);
        }
    }

    private void loadBeensInlayer(Set<ServiceBeen> layer) throws InvalidClassRegistrationException {
        if (layer.isEmpty()) return;
        List<CompletableFuture<?>> tasks = new ArrayList<>(layer.size());
        for (ServiceBeen serviceBean : layer) {
            tasks.add(CompletableFuture.runAsync(() -> {
                try {
                    registerLoadedClass(serviceBean.getClazz(), new HashSet<>());
                } catch (InvalidClassRegistrationException e) {
                    throw new RuntimeException(e);
                }
            }, mainExecutor));
        }
        try {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                Throwable rcc = re.getCause();
                if (rcc instanceof InvalidClassRegistrationException icre) throw icre;
                throw new DependencyInjectionException((rcc != null) ? rcc : re);
            }
            throw new DependencyInjectionException(e);
        }
    }

    private void throwIfUnload() {
        if (!isLoaded()) throw new UnloadError("unload: DependencyContainer");
    }

    public Set<Class<?>> getDependecyClassList(Class<?> clazz, Set<Class<?>> serviceLoadedClass) {
        Set<Class<?>> dependencies = new HashSet<>();

        for (Field field : getInjectableFields(clazz)) {
            dependencies.addAll(isServiceDependency(field.getType(), serviceLoadedClass, field));
        }
        Constructor<?>[] ctors = constructorsCache.computeIfAbsent(clazz, Class::getDeclaredConstructors);
        for (Constructor<?> constructor : ctors) {
            for (Parameter param : constructor.getParameters()) {
                dependencies.addAll(isServiceDependency(param.getType(), serviceLoadedClass, param));
            }
        }
        return dependencies;
    }

    private Set<Class<?>> isServiceDependency(Class<?> type, Set<Class<?>> serviceLoadedClass, Object extra) {
        Set<Class<?>> dependencies = new HashSet<>();
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            String requestedQualifier = DEFAULT_QUALIFIER;
            if (extra instanceof Field f) requestedQualifier = getQualifierName(f);
            else if (extra instanceof Parameter p) requestedQualifier = getQualifierName(p);

            for (Class<?> serviceClass : serviceLoadedClass) {
                if (type.isAssignableFrom(serviceClass)
                        && !serviceClass.isInterface()
                        && !Modifier.isAbstract(serviceClass.getModifiers())) {
                    String serviceQualifier = getQualifierName(serviceClass);
                    if (serviceQualifier.equalsIgnoreCase(requestedQualifier)) {
                        dependencies.add(serviceClass);
                    }
                }
            }
        } else {
            dependencies.add(type);
        }
        return dependencies;
    }

    private void processDependencyWithParallelStream(Map<Class<?>, Set<Class<?>>> dependencyGraph) {
        loadedClasses.parallelStream()
                .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                .forEach(clazz -> dependencyGraph.put(clazz, getDependecyClassList(clazz, loadedClasses)));
    }

    private void processDependencyWithExecutorService(Map<Class<?>, Set<Class<?>>> dependencyGraph) {
        List<CompletableFuture<Void>> tasks = loadedClasses.stream()
                .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                .map(clazz -> CompletableFuture.runAsync(
                        () -> dependencyGraph.put(clazz, getDependecyClassList(clazz, loadedClasses)),
                        mainExecutor))
                .collect(Collectors.toList());
        try {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new DependencyContainerRuntimeException("Erro ao processar grafo de dependências",
                    e.getCause() != null ? e.getCause() : e);
        }
    }

    private Set<Class<?>> topologicalSort(Set<Class<?>> classes, Map<Class<?>, Set<Class<?>>> graph) {
        Set<Class<?>> ordered = new LinkedHashSet<>();
        Set<Class<?>> visited = new HashSet<>();
        Set<Class<?>> visiting = new HashSet<>();
        for (Class<?> clazz : classes) {
            if (!visited.contains(clazz)) visit(clazz, graph, visited, visiting, ordered);
        }
        return ordered;
    }

    private void visit(Class<?> clazz,
                       @NonNull Map<Class<?>, Set<Class<?>>> graph,
                       Set<Class<?>> visited,
                       Set<Class<?>> visiting,
                       Set<Class<?>> ordered) {
        if (visiting.contains(clazz)) {
            throw new DependencyContainerRuntimeException("Ciclo de dependência detectado em: " + clazz.getName());
        }
        if (visited.contains(clazz)) return;
        visiting.add(clazz);
        for (Class<?> dep : graph.getOrDefault(clazz, Collections.emptySet())) {
            visit(dep, graph, visited, visiting, ordered);
        }
        visiting.remove(clazz);
        visited.add(clazz);
        ordered.add(clazz);
    }

    private boolean isClassOfContext(Class<?> clazz) {
        return Context.class.isAssignableFrom(clazz);
    }

    private LazyObject extractType(Field field) {
        Class<?> fieldType = field.getType();
        if (LazyDependency.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length == 1 && typeArgs[0] instanceof Class) {
                    return new LazyObject((Class<?>) typeArgs[0], true);
                }
            }
        }
        return new LazyObject(fieldType, false);
    }

    private LazyObject extractType(Parameter parameter) {
        Class<?> fieldType = parameter.getType();
        Type genericType = parameter.getParameterizedType();
        if (LazyDependency.class.isAssignableFrom(fieldType)) {
            if (genericType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length == 1 && typeArgs[0] instanceof Class) {
                    return new LazyObject((Class<?>) typeArgs[0], true);
                }
            }
        }
        return new LazyObject(fieldType, false);
    }

    private Object getDependecyObjectByParam(Parameter parameter) {
        final LazyObject lazyObject = extractType(parameter);
        final Class<?> clazzVariable = lazyObject.getClazz();
        final boolean isLazy = lazyObject.isLazy();
        final String qualifier = getQualifierName(parameter);

        if (isClassOfContext(clazzVariable)) {
            if (!isLazy) return ContextHolderActivity.getCurrentContext();
            Supplier<Context> actionInject = ContextHolderActivity::getCurrentContext;
            return Lazy.of(actionInject);
        }
        if (!isLazy) {
            return getDependencyInternal(clazzVariable, qualifier, false);
        }
        return Lazy.of(() -> getDependencyInternal(clazzVariable, qualifier, false));
    }

    private void injectDependenciesParallel(Object instance, List<Field> listOfRegistration) {
        List<CompletableFuture<?>> tasks = new ArrayList<>(listOfRegistration.size());
        for (Field variable : listOfRegistration) {
            tasks.add(CompletableFuture.runAsync(() -> injectVariable(variable, instance), mainExecutor));
        }
        try {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            if (log) Log.e(TAG, "Erro ao injetar dependências em paralelo", e);
        }
    }

    private void injectSequential(Object instance, List<Field> listOfRegistration) {
        for (Field variable : listOfRegistration) {
            injectVariable(variable, instance);
        }
    }

    private boolean isParallelInjection(int injectionSize) {
        InjectionStrategy strategy = injectionStrategy.get();
        if (strategy == InjectionStrategy.ADAPTIVE) return injectionSize > 10;
        return InjectionStrategy.PARALLEL == strategy && injectionSize > 1;
    }

    private List<Set<Class<?>>> groupByDependencyLayer(
            Set<Class<?>> serviceLoadedClass,
            Map<Class<?>, Set<Class<?>>> dependencyGraph) {

        Map<Class<?>, Integer> inDegree = new HashMap<>(serviceLoadedClass.size());
        Map<Class<?>, Set<Class<?>>> reverse = new HashMap<>(serviceLoadedClass.size());

        for (Class<?> clazz : serviceLoadedClass) {
            inDegree.putIfAbsent(clazz, 0);
            reverse.putIfAbsent(clazz, new HashSet<>());
        }
        for (Class<?> clazz : serviceLoadedClass) {
            Set<Class<?>> deps = dependencyGraph.getOrDefault(clazz, Collections.emptySet());
            for (Class<?> dep : deps) {
                if (!serviceLoadedClass.contains(dep)) continue;
                reverse.computeIfAbsent(dep, k -> new HashSet<>()).add(clazz);
                inDegree.merge(clazz, 1, Integer::sum);
            }
        }

        List<Set<Class<?>>> layers = new ArrayList<>();
        Set<Class<?>> currentLayer = inDegree.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));

        int processed = 0;
        while (!currentLayer.isEmpty()) {
            layers.add(currentLayer);
            processed += currentLayer.size();
            Set<Class<?>> next = new HashSet<>();
            for (Class<?> done : currentLayer) {
                for (Class<?> consumer : reverse.getOrDefault(done, Collections.emptySet())) {
                    int remaining = inDegree.merge(consumer, -1, Integer::sum);
                    if (remaining == 0) next.add(consumer);
                }
            }
            currentLayer = next;
        }

        if (processed < serviceLoadedClass.size()) {
            Set<Class<?>> unresolved = new HashSet<>(serviceLoadedClass);
            unresolved.removeAll(layers.stream().flatMap(Set::stream).collect(Collectors.toSet()));
            throw new DependencyContainerRuntimeException(
                    "Ciclo de dependência detectado entre: "
                            + unresolved.stream().map(Class::getName).collect(Collectors.joining(", ")));
        }
        return layers;
    }

    private <T> T getDependencyInternal(Class<T> reference, String referenceName, boolean throwUnload) {
        if (throwUnload) throwIfUnload();
        try {
            String q = normalizeQualifier(referenceName);
            Dependency dep = lookupDependency(reference, q);
            if (dep == null) {
                if (log) Log.w(TAG, "Dependência não encontrada: " + reference + "[" + q + "]");
                return null;
            }
            Object instance = dep.getDependency();
            return reference.cast(instance);
        } catch (ClassCastException cce) {
            if (log) Log.e(TAG, "ClassCast em getDependency: " + reference, cce);
            return null;
        } catch (Exception e) {
            if (log) Log.e(TAG, "Erro em getDependency: " + reference, e);
            return null;
        }
    }

    private void injectDependenciesInternal(Object instance, boolean throwUnload) {
        if (throwUnload) throwIfUnload();
        final Class<?> clazz = instance.getClass();
        List<Field> listOfRegistration = getInjectableFields(clazz);
        if (listOfRegistration.isEmpty()) return;
        if (isParallelInjection(listOfRegistration.size())) {
            injectDependenciesParallel(instance, listOfRegistration);
        } else {
            injectSequential(instance, listOfRegistration);
        }
    }
}
