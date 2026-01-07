package dtm.core.dependencymanager.core.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import dtm.dependencymanager.annotations.EnableInheritedViewInjection;
import dtm.core.dependencymanager.internal.AppElementsMapperStorage;
import dtm.dependencymanager.annotations.ViewElement;
import dtm.core.dependencymanager.core.AppElementsMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class ViewManagedFragment extends ContextManagedFragment {

    protected AppElementsMapper appElementsMapper;
    private boolean loadFragmentView = false;

    protected ViewManagedFragment(int idLayout){
        this.idLayout = idLayout;
    }

    protected ViewManagedFragment(){
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
            throw new RuntimeException("Id: '"+idName+"' não encontrado");
        }

        return id;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadFragmentView = true;
        injectIdView(view);
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState, int idLayout) {
        View view = inflater.inflate(idLayout, container, false);
        injectIdView(view);
        loadFragmentView = true;
        return view;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(idLayout, container, false);
        injectIdView(view);
        loadFragmentView = true;
        return view;
    }

    @Override
    public <T extends ViewBinding> View onCreateView(@NonNull Supplier<T> action){
        viewBinding = action.get();
        View view = viewBinding.getRoot();
        injectIdView(view);
        loadFragmentView = true;
        return view;
    }

    public boolean isFragmentLoad(){
        return loadFragmentView;
    }

    public void waitForFragmentToLoad(Runnable action) {
        if(isFragmentLoad()) {
            action.run();
        }else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isFragmentLoad()) {
                        action.run();
                    } else {
                        handler.postDelayed(this, 100);
                    }
                }
            }, 100);
        }
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
