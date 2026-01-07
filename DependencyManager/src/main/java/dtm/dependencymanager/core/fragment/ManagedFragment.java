package dtm.dependencymanager.core.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import dtm.dependencymanager.containers.DependencyContainerStorage;
import dtm.dependencymanager.core.DependencyContainer;
import dtm.dependencymanager.core.InjectionStrategy;
import dtm.dependencymanager.exceptions.InvalidClassRegistrationException;
import dtm.dependencymanager.exceptions.NewInstanceException;

import java.util.function.Supplier;


public abstract class ManagedFragment extends ViewManagedFragment {
    private final DependencyContainer dependencyContainer;
    protected boolean loadFragmentDependencies = false;

    public ManagedFragment(){
        this.dependencyContainer = DependencyContainerStorage.getInstance();
    }

    protected ManagedFragment(int idLayout) {
        super(idLayout);
        this.dependencyContainer = DependencyContainerStorage.getInstance();
    }
    protected ManagedFragment(DependencyContainer dependencyContainer, int idLayout) {
        super(idLayout);
        this.dependencyContainer = dependencyContainer;
    }

    protected void unRegisterService(Class<?> clazz){
        dependencyContainer.unRegisterDependency(clazz);
    }

    protected void registerService(Class<?> clazz) throws InvalidClassRegistrationException {
        dependencyContainer.registerDependency(clazz);
    }

    protected void registerService(Object instance) throws InvalidClassRegistrationException{
        dependencyContainer.registerDependency(instance);
    }

    protected void registerService(Object instance, String qualifier) throws InvalidClassRegistrationException{
        dependencyContainer.registerDependency(instance, qualifier);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        preInit();
        super.onViewCreated(view, savedInstanceState);
        injectIdView(view);
        preInjection();
        dependencyContainer.injectDependencies(this);
        loadFragmentDependencies = true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        preInjection();
        dependencyContainer.injectDependencies(this);
        loadFragmentDependencies = true;
        return view;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState, int idLayout) {
        View view = super.onCreateView(inflater, container, savedInstanceState, idLayout);
        preInjection();
        dependencyContainer.injectDependencies(this);
        loadFragmentDependencies = true;
        return view;
    }

    @Override
    public <T extends ViewBinding> View onCreateView(@NonNull Supplier<T> action){
        View view = super.onCreateView(action);
        preInjection();
        dependencyContainer.injectDependencies(this);
        loadFragmentDependencies = true;
        return view;
    }

    @Override
    public boolean isFragmentLoad(){
        return super.isFragmentLoad() && loadFragmentDependencies;
    }

}
