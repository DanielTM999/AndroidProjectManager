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

import lombok.NonNull;

public abstract class ContextHolderActivity extends ContextActivity {

    private static WeakReference<ContextHolderActivity> activityRef = new WeakReference<>(null);
    private static WeakReference<Context> contextRef = new WeakReference<>(null);

    @Override
    protected void onCreate(Bundle savedInstanceState, @LayoutRes Integer idLayout){
        super.onCreate(savedInstanceState);
        if(idLayout != null){
            setContentView(idLayout);
        }
        activityRef = new WeakReference<>(this);
        contextRef = new WeakReference<>(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        activityRef = new WeakReference<>(this);
        contextRef = new WeakReference<>(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        activityRef = new WeakReference<>(this);
        contextRef = new WeakReference<>(this);
    }


    @SuppressWarnings("unchecked")
    public static <T extends ContextHolderActivity> T getCurrentActivity(){
        ContextHolderActivity activity = activityRef.get();
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
        ContextHolderActivity activity = activityRef.get();
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
        return contextRef.get();
    }

    public static Context getCurrentContextOrThrow(){
        Context context = contextRef.get();
        if(context == null){
            throw new NoCurrentContextException("Sem contexto");
        }
        return context;
    }

    public static void setCurrentContext(@NonNull Context context){
        contextRef = new WeakReference<>(context);
    }

}
