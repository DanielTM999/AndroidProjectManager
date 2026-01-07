package dtm.dependencymanager.core.fragment.popup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import dtm.dependencymanager.annotations.EnableInheritedViewInjection;
import dtm.dependencymanager.internal.AppElementsMapperStorage;
import dtm.dependencymanager.annotations.ViewElement;
import dtm.dependencymanager.core.AppElementsMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class ViewManagedFragmentPopup extends ContextManagedFragmentPopup {


    protected AppElementsMapper appElementsMapper;
    protected ViewBinding viewBinding;
    protected int idLayout;

    protected ViewManagedFragmentPopup(int idLayout){
        this.idLayout = idLayout;
    }

    protected ViewManagedFragmentPopup(){
    }

    protected void preInit(){}

    protected void preInjection(){}

    protected void injectIdView(View root){
        try{
            this.appElementsMapper = AppElementsMapperStorage.getInstance();
            List<Field> viewFields = getInjectableViewsFields();

            for (Field variable : viewFields) {
                int id = getId(variable);
                View view;
                if(root == null){
                    view = requireActivity().findViewById(id);
                }else{
                    view = root.findViewById(id);
                }
                if(!variable.isAccessible()){
                    variable.setAccessible(true);
                }
                variable.set(this, view);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected int getId(Field variable) {
        ViewElement viewElement = variable.getAnnotation(ViewElement.class);
        final String viewElementId = viewElement.id();
        final String idName = (viewElementId == null || viewElementId.isEmpty()) ? variable.getName() : viewElementId;
        Integer id = appElementsMapper.getViewIdByName(idName);
        if(id == null){
            throw new RuntimeException("Id: "+idName+"não encontrado");
        }

        return id;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        injectIdView(view);
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(idLayout, container, false);
        injectIdView(view);
        return view;
    }

    protected View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState, int idLayout) {
        View view = inflater.inflate(idLayout, container, false);
        injectIdView(view);
        return view;
    }

    public <T extends ViewBinding> View onCreateView(@NonNull Supplier<T> action){
        viewBinding = action.get();
        View view = viewBinding.getRoot();
        injectIdView(view);
        return view;
    }

    private List<Field> getInjectableViewsFields(){
        Class<?> type = getClass();
        List<Field> viewFields = new ArrayList<>();
        EnableInheritedViewInjection cfg = type.getAnnotation(EnableInheritedViewInjection.class);
        int levels = cfg != null ? Math.max(cfg.levels(), 0) : 0;
        int currentLevel = 0;
        while (type != null && type != Object.class && currentLevel <= levels) {
            for (Field f : type.getDeclaredFields()) {
                if (f.isAnnotationPresent(ViewElement.class)) {
                    viewFields.add(f);
                }
            }
            type = type.getSuperclass();
            currentLevel++;
        }

        return viewFields;
    }

}
