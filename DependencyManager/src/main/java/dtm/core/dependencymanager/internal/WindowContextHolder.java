package dtm.core.dependencymanager.internal;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import dtm.core.dependencymanager.core.activity.ContextHolderActivity;
import dtm.core.dependencymanager.core.window.WindowEventListenerClient;

public final class WindowContextHolder {
    private static Set<WeakReference<WindowEventListenerClient>> windowEventListenerClientRefs = ConcurrentHashMap.newKeySet();
    private static WeakReference<ContextHolderActivity> currentActivityRef = new WeakReference<>(null);
    private static WeakReference<Context> currentContextRef = new WeakReference<>(null);
    private static final Map<Class<? extends FragmentActivity>, List<WeakReference<Fragment>>> fragmentActivityMap = new ConcurrentHashMap<>();


    public static void setCurrentActivityRef(ContextHolderActivity currentActivity){
        currentActivityRef = new WeakReference<>(currentActivity);
        currentContextRef = new WeakReference<>(currentActivity);
        if(currentActivity != null){
            windowEventListenerClientRefs.add(new WeakReference<>(currentActivity));
        }
    }
    public static void setCurrentContextRef(Context currentContext){
        currentContextRef = new WeakReference<>(currentContext);
        if(currentContext instanceof WindowEventListenerClient c){
            windowEventListenerClientRefs.add(new WeakReference<>(c));
        }
    }

    public static ContextHolderActivity getCurrentActivity() {
        return currentActivityRef.get();
    }
    public static Context getCurrentContext() {
        return currentContextRef.get();
    }

    public static void registerFragment(FragmentActivity activity, Fragment fragment) {
        if (fragment == null) return;

        Class<? extends FragmentActivity> activityClass =
                (activity == null) ? UnattachedActivityFragment.class : activity.getClass();

        if(fragment instanceof WindowEventListenerClient c){
            windowEventListenerClientRefs.add(new WeakReference<>(c));
        }

        fragmentActivityMap.compute(activityClass, (cls, list) -> {
            if (list == null) {
                list = Collections.synchronizedList(new ArrayList<>());
            }
            list.removeIf(ref -> ref.get() == null);

            list.add(new WeakReference<>(fragment));
            return list;
        });
    }
    public static List<Fragment> getFragmentsForActivity() {
        return fragmentActivityMap.values()
                .stream()
                .flatMap(Collection::stream)
                .map(WeakReference::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    public static List<Fragment> getFragmentsForActivity(Class<? extends FragmentActivity> activityClass) {
        List<WeakReference<Fragment>> refs = fragmentActivityMap.get(activityClass);
        if (refs == null) return Collections.emptyList();
        return refs
                .stream()
                .map(WeakReference::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<WindowEventListenerClient> getWindowEventListenerClientList(){
        return windowEventListenerClientRefs.stream()
                .map(WeakReference::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private static class UnattachedActivityFragment extends AppCompatActivity{}

}
