package dtm.core.dependencymanager.containers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import dtm.core.dependencymanager.core.InjectionStrategy;
import dtm.core.dependencymanager.internal.DependencyObject;
import dtm.core.dependencymanager.internal.Lazy;
import dtm.core.dependencymanager.internal.LazyObject;
import dtm.core.dependencymanager.internal.ServiceBeen;
import dtm.core.dependencymanager.internal.StaticContainer;
import dtm.core.dependencymanager.annotations.*;
import dtm.core.dependencymanager.core.DependencyContainer;
import dtm.core.dependencymanager.core.FunctionRegistrationResult;
import dtm.core.dependencymanager.core.activity.ContextHolderActivity;
import dtm.core.dependencymanager.exceptions.*;
import dtm.core.dependencymanager.core.prototypes.Dependency;
import dtm.core.dependencymanager.core.prototypes.LazyDependency;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class DependencyContainerStorage implements DependencyContainer {
    private static final String TAG = "DependencyContainer";
    private final ExecutorService mainExecutor;
    private final Map<Class<?>, List<Dependency>> dependencyContainer;
    private final Set<Class<?>> loadedClasses;
    private final List<ServiceBeen> serviceBeensDefinition;
    private final List<Set<ServiceBeen>> serviceBeensDefinitionLayer;
    private final AtomicBoolean loaded;
    private final AtomicReference<InjectionStrategy> injectionStrategy;
    private boolean childrenRegistration;
    private boolean log;
    private final boolean processInlayer = true;

    private DependencyContainerStorage(){
        this.loaded = new AtomicBoolean(false);
        this.childrenRegistration = false;
        this.injectionStrategy = new AtomicReference<>(InjectionStrategy.ADAPTIVE);
        this.dependencyContainer = new ConcurrentHashMap<>();
        this.loadedClasses = ConcurrentHashMap.newKeySet();
        this.serviceBeensDefinition = Collections.synchronizedList(new ArrayList<>());
        this.serviceBeensDefinitionLayer = Collections.synchronizedList(new ArrayList<>());
        this.mainExecutor = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
    }

    public static DependencyContainerStorage getInstance(){
        if(StaticContainer.getContainerStorage() == null){
            StaticContainer.setContainerStorage(new DependencyContainerStorage());
        }

        return StaticContainer.getContainerStorage();
    }

    @Override
    public boolean isLoaded() {
        return loaded.get();
    }

    @Override
    public void load() throws InvalidClassRegistrationException{
        if(isLoaded()) return;
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
        try{
            return (T)createObject(referenceClass);
        }catch (Exception e){
            throw new NewInstanceException(e.getMessage(), referenceClass, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> referenceClass, Object... args) throws NewInstanceException {
        throwIfUnload();
        try{
            return (T)createObject(referenceClass, args, true);
        } catch (RuntimeException e){
            Throwable th = (e.getCause() != null) ? e.getCause() : e;
            if(th instanceof NewInstanceException newInstanceException){
                throw newInstanceException;
            }
            throw new NewInstanceException(th.getMessage(), referenceClass, e);
        }
    }

    @Override
    public void registerDependency(@NonNull Object dependency, @NonNull String qualifier) throws InvalidClassRegistrationException {
        registerDependencyInternal(dependency, qualifier);
    }

    @Override
    public void registerDependency(@NonNull Object dependency) throws InvalidClassRegistrationException {
        registerDependencyInternal(dependency);
    }

    @Override
    public void registerDependency(@NonNull Class<?> dependency) throws InvalidClassRegistrationException {
        if(!isLoaded()){
            loadedClasses.add(dependency);
        }
    }

    @Override
    public <T> void registerDependency(FunctionRegistrationResult<T> action) throws InvalidClassRegistrationException {
        registerDependencyInternal(action);
    }

    @Override
    public void unRegisterDependency(Class<?> clazzDependency) {
        throwIfUnload();
        if(!dependencyContainer.containsKey(clazzDependency)) return;
        List<Dependency> dependencyList = new ArrayList<>(dependencyContainer.get(clazzDependency));

        for (Dependency dependencyObj : dependencyList){
            for(Class<?> clazz : dependencyObj.getDependencyClassInstanceTypes()){
                dependencyContainer.remove(clazz);
            }
        }
    }

    @Override
    public void enableChildrenRegistration() {
        this.childrenRegistration = true;
    }

    @Override
    public void disableChildrenRegistration() {
        this.childrenRegistration = false;
    }


    @Override
    public void enableLog() {
        this.log = true;
    }

    @Override
    public void disableLog() {
        this.log = false;
    }

    @Override
    public List<Dependency> getRegisteredDependencies() {
        return dependencyContainer.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void setInjectionStrategy(InjectionStrategy injectionStrategy) {
        this.injectionStrategy.set((injectionStrategy != null) ? injectionStrategy : InjectionStrategy.ADAPTIVE);
    }

    private void registerSubTypes(@NonNull Class<?> clazz, @NonNull List<Dependency> listOfDependency){
        if (clazz.equals(Object.class) || clazz.isInterface()) {
            return;
        }
        Class<?> superClass = clazz.getSuperclass();
        Class<?>[] interfaces = clazz.getInterfaces();

        if (superClass != null && !superClass.equals(Object.class) && !superClass.isInterface()) {
            dependencyContainer.put(superClass, listOfDependency);
        }

        for(Class<?> interfaceObj : interfaces){
            if (!interfaceObj.equals(Object.class)) {
                dependencyContainer.put(interfaceObj, listOfDependency);
                if (log) {
                    Log.d("registerSubTypes", "Registrado tipo: " + interfaceObj.getName() +
                            " com dependências: " + listOfDependency.stream()
                            .map(d -> d.getDependencyClass().getName())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse(""));
                }
            }
        }

    }

    private void registerAutoInject(@NonNull Class<?> clazz) throws InvalidClassRegistrationException{
        List<Class<?>> listOfRegistration = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Inject.class))
                .map(Field::getType)
                .collect(Collectors.toList());

        for(Class<?> subClass : listOfRegistration){
            if(!dependencyContainer.containsKey(subClass)){
                registerDependency(subClass);
            }
        }
    }

    private Object createObject(@NonNull Class<?> clazz){
        return createObject(clazz, new Object[0], false);
    }

    private Object createObject(@NonNull Class<?> clazz, Object[] extraConstructorArgs, boolean throwError){
        try {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            List<Parameter> failedParams = new ArrayList<>();
            for (Constructor<?> constructor : constructors) {
                Parameter[] parameterTypes = constructor.getParameters();
                Object[] resolvedArgs = tryResolveConstructorArgs(parameterTypes, extraConstructorArgs, failedParams);

                if (resolvedArgs != null) {
                    constructor.setAccessible(true);
                    Object instance = constructor.newInstance(resolvedArgs);
                    injectDependenciesInternal(Objects.requireNonNull(instance), false);
                    return instance;
                }
            }
            String message;
            if (!failedParams.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("Falha ao instanciar " + clazz.getName() + ". Parâmetros não resolvidos:\n");
                for (Parameter p : failedParams) {
                    errorMsg.append("- ").append(p.getName()).append(" : ").append(p.getType().getName()).append("\n");
                }
                message = errorMsg.toString();
            }else{
                message = "Sem construtor aplicável encontrado para " + clazz.getName();
            }
            throw new NewInstanceException(message, clazz);
        } catch (Exception e) {
            String message = "Erro ao criar Objeto "+clazz+" ==> cause: "+e.getMessage();
            if(log) Log.e("createObjectDependency", message, e);
            if(throwError) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    private Object[] tryResolveConstructorArgs(Parameter[] parameters, Object[] extraArgs, List<Parameter> failedParams) {
        Object[] args = new Object[parameters.length];

        List<Object> extras = new ArrayList<>();
        if (extraArgs != null) {
            Collections.addAll(extras, extraArgs);
        }

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> paramType = parameter.getType();

            Object matchedExtra = null;
            Iterator<Object> iterator = extras.iterator();
            while (iterator.hasNext()) {
                Object candidate = iterator.next();
                if (candidate != null && paramType.isAssignableFrom(candidate.getClass())) {
                    matchedExtra = candidate;
                    iterator.remove();
                    break;
                }
            }

            if (matchedExtra != null) {
                args[i] = matchedExtra;
            } else {
                Object injected = getDependecyObjectByParam(parameter);
                if (injected == null) {
                    if (failedParams != null) {
                        failedParams.add(parameter);
                    }
                    return null;
                }
                args[i] = injected;
            }
        }

        return args;
    }

    private Supplier<Object> createActivationFunction(@NonNull Class<?> clazz){
        return () -> {
            return createObject(clazz);
        };
    }

    private boolean isSingleton(@NonNull Class<?> clazz){
        return clazz.isAnnotationPresent(Singleton.class);
    }

    private String getQualifierName(@NonNull Class<?> clazz){
        if(clazz.isAnnotationPresent(Qualifier.class)){
            Qualifier qualifierAnnotation = clazz.getAnnotation(Qualifier.class);
            return (Objects.requireNonNull(qualifierAnnotation).qualifier() == null || qualifierAnnotation.qualifier().isEmpty()) ? "default" : qualifierAnnotation.qualifier();
        } else {
            return  "default";
        }
    }

    private String getQualifierName(@NonNull Field variable) {
        if (variable.isAnnotationPresent(Qualifier.class)) {
            Qualifier qualifierAnnotation = variable.getAnnotation(Qualifier.class);
            return (Objects.requireNonNull(qualifierAnnotation).qualifier() == null || qualifierAnnotation.qualifier().isEmpty()) ? "default" : qualifierAnnotation.qualifier();
        }else if(variable.isAnnotationPresent(Inject.class)){
            Inject inject = variable.getAnnotation(Inject.class);
            return (Objects.requireNonNull(inject).qualifier() == null || inject.qualifier().isEmpty()) ? "default" : inject.qualifier();
        }else {
            return  "default";
        }
    }

    private String getQualifierName(@NonNull Parameter variable){
        if(variable.isAnnotationPresent(Qualifier.class)){
            Qualifier qualifierAnnotation = variable.getAnnotation(Qualifier.class);
            return (Objects.requireNonNull(qualifierAnnotation).qualifier() == null || qualifierAnnotation.qualifier().isEmpty()) ? "default" : qualifierAnnotation.qualifier();
        } else {
            return  "default";
        }
    }

    private Object createWithConstructor(@NonNull Class<?> clazz, @NonNull Constructor<?>[] constructors){
        try{
            Constructor<?> chosenConstructor = Arrays.stream(constructors)
                    .max(Comparator.comparingInt(Constructor::getParameterCount))
                    .orElseThrow(() -> new DependencyContainerException("Nenhum construtor encontrado para " + clazz.getName()));

            Parameter[] parameters = chosenConstructor.getParameters();
            Object[] args = Arrays.stream(parameters)
                    .map(this::getDependecyObjectByParam)
                    .toArray();


            return chosenConstructor.newInstance(args);
        }catch (Exception e){
            try{
                return createWithOutConstructor(clazz);
            }catch (Exception ignored){
                return null;
            }
        }
    }

    private Object createWithOutConstructor(@NonNull Class<?> clazz) throws Exception{
        return clazz.getDeclaredConstructor().newInstance();
    }

    private void injectVariable(Field variable, Object instance){
       try{
           final LazyObject lazyObject = extractType(variable);
           final Class<?> clazzVariable = lazyObject.getClazz();
           final boolean isLazy = lazyObject.isLazy();

           if(!variable.isAccessible()){
               variable.setAccessible(true);
           }

           if(isClassOfContext(clazzVariable)){
               if(!isLazy){
                   variable.set(instance, ContextHolderActivity.getCurrentContext());
               }else{
                   Supplier<Context> actionInject = ContextHolderActivity::getCurrentContext;
                   variable.set(instance, Lazy.of(actionInject));
               }
           }else{
               if(!isLazy){
                   Object targetInstance = getObjectToInjectVariable(variable, clazzVariable);
                   variable.set(instance, targetInstance);
               }else {
                   Supplier<Object> getDependecyLazy = () -> {
                       try {
                           return getObjectToInjectVariable(variable, clazzVariable);
                       }catch(Exception e){
                           String message = "Erro ao definir variavel "+variable.getName()+" ==> cause: "+e.getMessage();
                           if(log) Log.e("createObjectDependency", message, e);
                          return null;
                       }
                   };
                   variable.set(instance, Lazy.of(getDependecyLazy));
               }
           }
       }catch (Exception e){
           String message = "Erro ao definir variavel "+variable.getName()+" ==> cause: "+e.getMessage();
           if(log) Log.e("createObjectDependency", message, e);
       }

    }

    private Object getObjectToInjectVariable(Field variable, Class<?> clazzVariable) throws Exception{
        final String qualifierName = getQualifierName(variable);
        List<Dependency> listOfDependency = dependencyContainer.getOrDefault(clazzVariable, Collections.emptyList());
        if(listOfDependency.isEmpty() && childrenRegistration){
            try {
                registerDependency(clazzVariable);
            } catch (InvalidClassRegistrationException e) {
                throw new DependencyContainerRuntimeException(e);
            }
            listOfDependency = dependencyContainer.getOrDefault(clazzVariable, Collections.emptyList());
        }
        Dependency dependencyObject = listOfDependency.stream().filter(d -> d.getQualifier().equals(qualifierName)).findFirst().orElseThrow(() -> new DependencyContainerException("Dependencia não encontrada para: "+clazzVariable));
        return dependencyObject.getDependency();
    }

    private void filterServiceClass(){
        final int threshold = 50;
        final int total = loadedClasses.size();
        final Map<Class<?>, Set<Class<?>>> dependencyGraph = new ConcurrentHashMap<>();

        Log.i(TAG, "Iniciando análise de dependências. Total de classes encontradas: " + total);

        if (total < threshold) {
            processDependencyWithParallelStream(dependencyGraph);
        } else {
            processDependencyWithExecutorService(dependencyGraph);
        }

        if(processInlayer){
            List<Set<Class<?>>> classLayers = groupByDependencyLayer(loadedClasses, dependencyGraph);
            Log.i(TAG, "Estratégia Layer: " + classLayers.size() + " camadas de dependência geradas.");
            int order = 0;
            for (Set<Class<?>> classSet : classLayers) {
                int layerOrder = order;
                Set<ServiceBeen> layer = ConcurrentHashMap.newKeySet();
                String classesInLayer = classSet.stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", "));
                Log.d(TAG, "Camada [" + layerOrder + "]: Contém " + classSet.size() + " classes -> [" + classesInLayer + "]");

                classSet.parallelStream().forEach(clazz -> {
                    layer.add(new ServiceBeen(clazz, layerOrder));
                });

                serviceBeensDefinitionLayer.add(layer);
                order++;
            }
        }else{
            Set<Class<?>> ordered = topologicalSort(loadedClasses, dependencyGraph);

            String topologicalList = ordered.stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(" -> "));
            Log.i(TAG, "Estratégia Topológica: " + ordered.size() + " classes. Ordem: [" + topologicalList + "]");
            int order = 0;
            for (Class<?> clazz : ordered) {
                serviceBeensDefinition.add(new ServiceBeen(clazz, order++));
            }
        }

    }

    private void registerDependencyInternal(@NonNull Object dependency) throws InvalidClassRegistrationException{
        registerDependencyInternal(dependency, "default");
    }

    private void registerDependencyInternal(@NonNull Object dependency, @NonNull String qualifier) throws InvalidClassRegistrationException{
        try{
            final Class<?> clazz = dependency.getClass();
            if(dependencyContainer.containsKey(clazz)) return;
            final List<Dependency> listOfDependency = dependencyContainer.getOrDefault(clazz, new ArrayList<Dependency>());
            final boolean containsQualifier = Objects.requireNonNull(listOfDependency).stream().anyMatch(d -> d.getQualifier().equals(qualifier));
            if(containsQualifier){
                throw new DependencyContainerException("Qualificador '"+qualifier+"' ja registrado para a dependencia: "+clazz);
            }
            DependencyObject dependencyObject = new DependencyObject(clazz, qualifier, true, () -> {return dependency;}, dependency);
            listOfDependency.add(dependencyObject);
            dependencyContainer.put(clazz, listOfDependency);
            registerSubTypes(clazz, listOfDependency);
        } catch (Exception e) {
            throw new InvalidClassRegistrationException(
                    "Erro ao criar a dependencia: " + dependency.getClass()+ " ==> causa: "+e.getMessage(),
                    dependency.getClass(),
                    e
            );
        }
    }

    private void registerDependencyInternal(@NonNull FunctionRegistrationResult<?> action) throws InvalidClassRegistrationException{
        final Class<?> referenceClass = action.getReferenceClass();
        final String qualifier = (action.getQualifier().isEmpty()) ? "default" : action.getQualifier();
        try{
            final Supplier<?> activatorFunction = action.getFunction();
            if(dependencyContainer.containsKey(referenceClass)) return;
            final List<Dependency> listOfDependency = dependencyContainer.getOrDefault(referenceClass, new ArrayList<Dependency>());
            final boolean containsQualifier = Objects.requireNonNull(listOfDependency).stream().anyMatch(d -> d.getQualifier().equals(qualifier));

            if(containsQualifier){
                throw new DependencyContainerException("Qualificador '"+qualifier+"' ja registrado para a dependencia: "+referenceClass);
            }
            DependencyObject dependencyObject = new DependencyObject(referenceClass, qualifier, true, activatorFunction, activatorFunction);
            listOfDependency.add(dependencyObject);
            dependencyContainer.put(referenceClass, listOfDependency);
            registerSubTypes(referenceClass, listOfDependency);
        }catch (Exception e) {
            throw new InvalidClassRegistrationException(
                    "Erro ao criar a dependencia: " + referenceClass+ " ==> causa: "+e.getMessage(),
                    referenceClass,
                    e
            );
        }
    }

    private void registerDependencyInternal(@NonNull Class<?> dependency, final Set<Class<?>> registeringClasses) throws InvalidClassRegistrationException{
        try{
            if(dependencyContainer.containsKey(dependency)) return;
            if(dependency.isEnum() || dependency.isInterface()){
                throw new DependencyContainerException("Registre uma classe concreta");
            }
            if (registeringClasses.contains(dependency)) {
                throw new InvalidClassRegistrationException("Dependência circular detectada: " + dependency.getName(), dependency);
            }
            registeringClasses.add(dependency);

            final String qualifier = getQualifierName(dependency);
            final List<Dependency> listOfDependency = dependencyContainer.getOrDefault(dependency, new ArrayList<Dependency>());
            final boolean containsQualifier = Objects.requireNonNull(listOfDependency).stream().anyMatch(d -> d.getQualifier().equals(qualifier));
            if(containsQualifier){
                throw new DependencyContainerException("Qualificador '"+qualifier+"' ja registrado para a dependencia: "+dependency);
            }
            if(childrenRegistration){
                registerAutoInject(dependency);
            }
            DependencyObject dependencyObject = isSingleton(dependency)
                    ? new DependencyObject(dependency, qualifier, true, null, createObject(dependency))
                    : new DependencyObject(dependency, qualifier, false, createActivationFunction(dependency), null);
            listOfDependency.add(dependencyObject);
            dependencyContainer.put(dependency, listOfDependency);
            registerSubTypes(dependency, listOfDependency);
            if (log) {
                StringBuilder sb = new StringBuilder("listOfDependency: [");
                for (Dependency d : listOfDependency) {
                    sb.append(d.getDependencyClass().getName()).append(", ");
                }
                if (!listOfDependency.isEmpty()) {
                    sb.setLength(sb.length() - 2);
                }
                sb.append("]");
                Log.d("registerDependency", sb.toString());
            }
        } catch (Exception e) {
            throw new InvalidClassRegistrationException(
                    "Erro ao criar a dependencia: " + dependency.getClass()+ " ==> causa: "+e.getMessage(),
                    dependency.getClass(),
                    e
            );
        }
    }

    private void loadBeens() throws InvalidClassRegistrationException{
        if(processInlayer){
            loadBeensInlayer();
        }else{
            loadBeensTopological();
        }
    }

    private void loadBeensTopological() throws InvalidClassRegistrationException{
        for (ServiceBeen service: new ArrayList<>(serviceBeensDefinition)){
            registerDependencyInternal(service.getClazz(), new HashSet<>());
        }
    }

    private void loadBeensInlayer() throws InvalidClassRegistrationException{
        for (Set<ServiceBeen> layer : serviceBeensDefinitionLayer) {
            loadBeensInlayer(layer);
        }
    }

    private void loadBeensInlayer(Set<ServiceBeen> layer) throws InvalidClassRegistrationException{
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        for (ServiceBeen serviceBean : layer) {
            CompletableFuture<?> task = CompletableFuture.runAsync(() -> {
                try {
                    registerDependencyInternal(serviceBean.getClazz(), new HashSet<>());
                } catch (InvalidClassRegistrationException e) {
                    throw new RuntimeException(e);
                }
            }, mainExecutor);
            tasks.add(task);
        }

        try {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
        }catch (Exception e){
            if(e instanceof RuntimeException runtimeException){
                Throwable cause = runtimeException.getCause();
                if (cause instanceof InvalidClassRegistrationException invalidClassRegistrationException) {
                    throw invalidClassRegistrationException;
                }

                throw new DependencyInjectionException((cause != null) ? cause : runtimeException);
            }

            throw new DependencyInjectionException(e);

        }
    }

    private void throwIfUnload(){
        if(!isLoaded()) throw new UnloadError("unload: DependencyContainer");
    }

    public Set<Class<?>> getDependecyClassList(Class<?> clazz, Set<Class<?>> serviceLoadedClass){
        Set<Class<?>> dependencies = ConcurrentHashMap.newKeySet();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                Class<?> fieldType = field.getType();
                dependencies.addAll(isServiceDependency(fieldType, serviceLoadedClass, field));
            }
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            for (Parameter param : constructor.getParameters()) {
                dependencies.addAll(isServiceDependency(param.getType(), serviceLoadedClass, param));
            }
        }

        return dependencies;
    }

    private Set<Class<?>> isServiceDependency(Class<?> type, Set<Class<?>> serviceLoadedClass, Object extra) {
        Set<Class<?>> dependencies = ConcurrentHashMap.newKeySet();

        if(type.isInterface() || Modifier.isAbstract(type.getModifiers())){
            for (Class<?> serviceClass : serviceLoadedClass) {
                if (type.isAssignableFrom(serviceClass) && !serviceClass.isInterface() && !Modifier.isAbstract(serviceClass.getModifiers())) {
                    String serviceQualifier = getQualifierName(serviceClass);
                    String qualifierElement = "default";
                    if(extra instanceof Field field){
                        qualifierElement = getQualifierName(field);
                    }else if(extra instanceof Parameter parameter){
                        qualifierElement = getQualifierName(parameter);
                    }
                    if (serviceQualifier.equalsIgnoreCase(qualifierElement)){
                        dependencies.add(serviceClass);
                    }

                }
            }
        }else{
            dependencies.add(type);
        }

        return dependencies;
    }

    private void processDependencyWithParallelStream(Map<Class<?>, Set<Class<?>>> dependencyGraph) {
        loadedClasses.parallelStream()
                .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                .forEach(clazz -> {
                    Set<Class<?>> dependencies = getDependecyClassList(clazz, loadedClasses);
                    dependencyGraph.put(clazz, dependencies);
                });
    }

    private void processDependencyWithExecutorService(Map<Class<?>, Set<Class<?>>> dependencyGraph) {
        final int threads = Runtime.getRuntime().availableProcessors();
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        List<CompletableFuture<Void>> tasks = loadedClasses.stream()
                .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                .map(clazz -> CompletableFuture.runAsync(() -> {
                    Set<Class<?>> dependencies = getDependecyClassList(clazz, loadedClasses);
                    dependencyGraph.put(clazz, dependencies);
                }, executorService))
                .collect(Collectors.toList());

        try {
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
            allTasks.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new DependencyContainerRuntimeException("Erro ao processar uma classe de dependecia", e.getCause());
        } finally {
            executorService.shutdown();
        }
    }

    private Set<Class<?>> topologicalSort(Set<Class<?>> classes, Map<Class<?>, Set<Class<?>>> graph) {
        Set<Class<?>> ordered = new HashSet<>();
        Set<Class<?>> visited = new HashSet<>();
        Set<Class<?>> visiting = new HashSet<>();

        for (Class<?> clazz : classes) {
            if (!visited.contains(clazz)) {
                visit(clazz, graph, visited, visiting, ordered);
            }
        }

        return ordered;
    }

    private void visit(
            Class<?> clazz,
            @NonNull Map<Class<?>, Set<Class<?>>> graph,
            Set<Class<?>> visited,
            Set<Class<?>> visiting,
            Set<Class<?>> ordered
    ){
        if (visiting.contains(clazz)) {
            throw new IllegalStateException("Ciclo de dependência detectado em: " + clazz.getName());
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

    private boolean isClassOfContext(Class<?> clazz){
        return Context.class.isAssignableFrom(clazz);
    }

    private LazyObject extractType(Field field){
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

    private LazyObject extractType(Parameter parameter){
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

    private Object getDependecyObjectByParam(Parameter parameter){
        final LazyObject lazyObject = extractType(parameter);
        final Class<?> clazzVariable = lazyObject.getClazz();
        final boolean isLazy = lazyObject.isLazy();

        if(isClassOfContext(clazzVariable)){
            if(!isLazy){
                return ContextHolderActivity.getCurrentContext();
            }
            Supplier<Context> actionInject = ContextHolderActivity::getCurrentContext;
            return Lazy.of(actionInject);
        }else{
            if(!isLazy){
                return getDependencyInternal(clazzVariable, false);
            }else{
                return Lazy.of(() -> getDependencyInternal(clazzVariable, false));
            }
        }
    }

    private List<Field> getInjectableFields(Class<?> clazz){
        List<Field> listOfRegistration = new ArrayList<>();

        Class<?> current = clazz;
        while (current != null && !current.equals(Object.class)) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Inject.class)) {
                    listOfRegistration.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return listOfRegistration;
    }

    private void injectDependenciesParallel(Object instance, List<Field> listOfRegistration){
        final List<CompletableFuture<?>> tasks = new ArrayList<>();
        for (Field variable : listOfRegistration) {

            tasks.add(CompletableFuture.runAsync(() -> {
                if (log) {
                    Log.d("DependencyInjection", "Injetando variável: " + variable.getName());
                }
                injectVariable(variable, instance);

                if (log) {
                    Log.d("DependencyInjection", "Variável injetada com sucesso: " + variable.getName());
                }
            }, mainExecutor));
        }

        try {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            if(log) Log.e("DependencyInjection", "Erro ao injetar dependências em paralelo", e);
        }
    }

    private void injectSequential(Object instance, List<Field> listOfRegistration){
        for (Field variable : listOfRegistration) {
            injectVariable(variable, instance);
        }
    }

    private boolean isParallelInjection(int injectionSize){
        return (injectionStrategy.get() == InjectionStrategy.ADAPTIVE)
                ? injectionSize > 10
                : InjectionStrategy.PARALLEL == injectionStrategy.get();
    }

    private List<Set<Class<?>>> groupByDependencyLayer(
            Set<Class<?>> serviceLoadedClass,
            Map<Class<?>, Set<Class<?>>> dependencyGraph) {

        List<Set<Class<?>>> layers = new ArrayList<>();
        Set<Class<?>> processed = new HashSet<>();


        Set<Class<?>> currentLayer = serviceLoadedClass.stream()
                .filter(c -> dependencyGraph.getOrDefault(c, Set.of()).isEmpty())
                .collect(Collectors.toSet());

        while (!currentLayer.isEmpty()) {
            layers.add(currentLayer);
            processed.addAll(currentLayer);

            currentLayer = serviceLoadedClass.stream()
                    .filter(c -> !processed.contains(c))
                    .filter(c -> {
                        Set<Class<?>> deps = dependencyGraph.getOrDefault(c, Set.of());
                        return processed.containsAll(deps);
                    })
                    .collect(Collectors.toSet());
        }

        return layers;

    }

    private <T> T getDependencyInternal(Class<T> reference, boolean throwUnload) {

        return getDependencyInternal(reference, getQualifierName(reference), throwUnload);
    }

    private <T> T getDependencyInternal(Class<T> reference, String referenceName, boolean throwUnload) {
        if(throwUnload) throwIfUnload();
        try{
            final List<Dependency> listOfDependency = dependencyContainer.getOrDefault(reference, Collections.emptyList());
            final Dependency dependencyObject = Objects.requireNonNull(listOfDependency).stream().filter(d -> d.getQualifier().equals(referenceName)).findFirst().orElseThrow();
            Object instance = dependencyObject.getDependency();
            return reference.cast(instance);
        }catch (Exception e){
            return null;
        }
    }
    
    private void injectDependenciesInternal(Object instance, boolean throwUnload) {
        if(throwUnload) throwIfUnload();
        final Class<?> clazz = instance.getClass();
        List<Field> listOfRegistration = getInjectableFields(clazz);

        if(isParallelInjection(listOfRegistration.size())){
            injectDependenciesParallel(instance, listOfRegistration);
        }else{
            injectSequential(instance, listOfRegistration);
        }
    } 
    
}
