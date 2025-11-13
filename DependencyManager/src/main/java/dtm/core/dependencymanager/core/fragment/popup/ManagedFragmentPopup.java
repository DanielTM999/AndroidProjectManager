package dtm.core.dependencymanager.core.fragment.popup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import dtm.core.dependencymanager.containers.DependencyContainerStorage;
import dtm.core.dependencymanager.core.DependencyContainer;
import dtm.core.dependencymanager.core.InjectionStrategy;
import dtm.core.dependencymanager.exceptions.NewInstanceException;
import java.util.function.Supplier;

public abstract class ManagedFragmentPopup extends ViewManagedFragmentPopup{

    private final DependencyContainer dependencyContainer;

    public ManagedFragmentPopup(){
        this.dependencyContainer = DependencyContainerStorage.getInstance();
    }

    protected ManagedFragmentPopup(int idLayout) {
        super(idLayout);
        this.dependencyContainer = DependencyContainerStorage.getInstance();
    }
    protected ManagedFragmentPopup(DependencyContainer dependencyContainer, int idLayout) {
        super(idLayout);
        this.dependencyContainer = dependencyContainer;
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
    protected <T> T newInstance(Class<T> reference) throws NewInstanceException {
        return dependencyContainer.newInstance(reference);
    }

    protected <T> T newInstanceOr(Class<T> reference, T defaultValue){
        try {
            return dependencyContainer.newInstance(reference);
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
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        preInjection();
        dependencyContainer.injectDependencies(this);
        return view;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState, int idLayout) {
        View view = super.onCreateView(inflater, container, savedInstanceState, idLayout);
        preInjection();
        dependencyContainer.injectDependencies(this);
        return view;
    }

    @Override
    public <T extends ViewBinding> View onCreateView(@NonNull Supplier<T> action){
        View view = super.onCreateView(action);
        preInjection();
        dependencyContainer.injectDependencies(this);
        return view;
    }

}
