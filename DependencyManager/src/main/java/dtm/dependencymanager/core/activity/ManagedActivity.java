package dtm.dependencymanager.core.activity;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import dtm.dependencymanager.containers.DependencyContainerStorage;
import dtm.dependencymanager.core.DependencyContainer;
import dtm.dependencymanager.core.InjectionStrategy;
import dtm.dependencymanager.exceptions.InvalidClassRegistrationException;
import dtm.dependencymanager.exceptions.NewInstanceException;
import dtm.dependencymanager.core.prototypes.Dependency;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public abstract class ManagedActivity extends ViewManagedActivity {
    private final DependencyContainer dependencyContainer;
    private boolean parallelStart;
    private final AtomicBoolean isReady;

    protected ManagedActivity() {
        this.dependencyContainer = DependencyContainerStorage.getInstance();
        this.parallelStart = false;
        this.isReady = new AtomicBoolean(false);
    }
    protected ManagedActivity(DependencyContainer dependencyContainer) {
        this.dependencyContainer = dependencyContainer;
        this.parallelStart = false;
        this.isReady = new AtomicBoolean(false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        preInit();
        super.onCreate(savedInstanceState, persistentState);
        startInjection();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        preInit();
        super.onCreate(savedInstanceState);
        startInjection();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, @LayoutRes Integer idLayout){
        preInit();
        super.onCreate(savedInstanceState);
        if(idLayout != null){
            setContentView(idLayout);
        }
        startInjection();
    }

    @Override
    protected void preInit(){
        try {
            load();
        } catch (InvalidClassRegistrationException e) {
            Log.e("InvalidClassRegistrationException", "preInit: erro ao registrar classe"+e.getReferenceClass(), e);
        }
    }

    @Override
    public boolean isActivityLoad() {
        return super.isActivityLoad() && isReady.get();
    }

    protected void enableParallelStart(){
        this.parallelStart = true;
    }

    protected boolean isReady(){
        return this.isReady.get();
    }

    protected DependencyContainer getDependencyContainer(){
        return dependencyContainer;
    }

    protected List<Dependency> getRegisteredDependencies(){
        return this.dependencyContainer.getRegisteredDependencies();
    }

    protected void registerService(Class<?> clazz) throws InvalidClassRegistrationException{
        dependencyContainer.registerDependency(clazz);
    }

    protected void registerService(Object instance) throws InvalidClassRegistrationException{
        dependencyContainer.registerDependency(instance);
    }

    protected void registerService(Object instance, String qualifier) throws InvalidClassRegistrationException{
        dependencyContainer.registerDependency(instance, qualifier);
    }

    protected <T> T newInstance(Class<T> reference) throws NewInstanceException {
        return dependencyContainer.newInstance(reference);
    }

    protected <T> T newInstance(Class<T> reference, Object... args) throws NewInstanceException {
        return dependencyContainer.newInstance(reference, args);
    }

    protected <T> T newInstanceOr(Class<T> reference, T defaultValue){
        try {
            return dependencyContainer.newInstance(reference);
        } catch (NewInstanceException e) {
            return defaultValue;
        }
    }

    protected <T> T newInstanceOr(Class<T> reference, T defaultValue, Object... args){
        try {
            return dependencyContainer.newInstance(reference, args);
        } catch (NewInstanceException e) {
            return defaultValue;
        }
    }

    protected void load() throws InvalidClassRegistrationException{
        if(!dependencyContainer.isLoaded()){
            dependencyContainer.load();
        }
    }

    protected boolean isLoaded(){
        return dependencyContainer.isLoaded();
    }

    protected void unRegisterService(Class<?> clazz){
        dependencyContainer.unRegisterDependency(clazz);
    }

    protected void enableChildrenRegistration(){
        dependencyContainer.enableChildrenRegistration();
    }
    protected void disableChildrenRegistration(){
        dependencyContainer.disableChildrenRegistration();
    }

    protected void setInjectionStrategy(InjectionStrategy injectionStrategy){
        dependencyContainer.setInjectionStrategy(injectionStrategy);
    }

    protected void preInjection(){}

    protected void onLoad(){};

    private void startInjection(){
        if(parallelStart){
            CompletableFuture.runAsync(() -> {
                bindViewElement();
                preInjection();
                dependencyContainer.injectDependencies(ManagedActivity.this);
                this.isReady.set(true);
            });
        }else{
            bindViewElement();
            preInjection();
            dependencyContainer.injectDependencies(ManagedActivity.this);
            this.isReady.set(true);
        }
        onLoadCallback();
    }

    private void onLoadCallback(){
       runAsync(() -> {
           while (!isReady() && !isActivityLoad()){
               LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
           }
           runOnUiThread(this::onLoad);
       });
    }
}
