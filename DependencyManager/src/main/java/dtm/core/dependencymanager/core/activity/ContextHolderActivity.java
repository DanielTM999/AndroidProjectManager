package dtm.core.dependencymanager.core.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import dtm.core.dependencymanager.exceptions.InvalidContextActivity;
import dtm.core.dependencymanager.exceptions.NoCurrentActivityException;
import dtm.core.dependencymanager.exceptions.NoCurrentContextException;

import java.lang.ref.WeakReference;
import java.util.List;

import dtm.core.dependencymanager.internal.WindowContextHolder;
import lombok.NonNull;

public abstract class ContextHolderActivity extends ContextActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState, @LayoutRes Integer idLayout){
        super.onCreate(savedInstanceState);
        if(idLayout != null){
            setContentView(idLayout);
        }
        WindowContextHolder.setCurrentActivityRef(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        WindowContextHolder.setCurrentActivityRef(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        WindowContextHolder.setCurrentActivityRef(this);
    }


    @SuppressWarnings("unchecked")
    public static <T extends ContextHolderActivity> T getCurrentActivity(){
        ContextHolderActivity activity = WindowContextHolder.getCurrentActivity();
        if(activity == null){
            return null;
        }
        try {
            return (T) activity;
        } catch (ClassCastException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ContextHolderActivity> T getCurrentActivityOrTrow(){
        ContextHolderActivity activity = WindowContextHolder.getCurrentActivity();
        if (activity == null) {
            throw new NoCurrentActivityException("Sem próxima atividade");
        }
        try {
            return (T) activity;
        } catch (ClassCastException e) {
            throw new InvalidContextActivity("Tentativa de conversão inválida: " + e.getMessage(), e);
        }
    }

    public static Context getCurrentContext(){
        return WindowContextHolder.getCurrentContext();
    }

    public static Context getCurrentContextOrThrow(){
        Context context = WindowContextHolder.getCurrentContext();
        if(context == null){
            throw new NoCurrentContextException("Sem contexto");
        }
        return context;
    }

    public static void setCurrentContext(@NonNull Context context){
        WindowContextHolder.setCurrentContextRef(context);
    }

}
