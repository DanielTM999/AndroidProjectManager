package dtm.core.apptest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dtm.dependencymanager.annotations.Inject;
import dtm.dependencymanager.annotations.Qualifier;
import dtm.dependencymanager.containers.DependencyContainerStorage;
import dtm.dependencymanager.core.DependencyContainer;
import dtm.dependencymanager.core.DependencyContainerRegistor;
import dtm.dependencymanager.core.activity.ManagedActivity;
import dtm.dependencymanager.core.prototypes.LazyDependency;

public class MainTesteActivity extends ManagedActivity {

    @Inject
    @Qualifier(qualifier = "pt")
    Greeter greeterPt;

    @Inject
    @Qualifier(qualifier = "en")
    Greeter greeterEn;

    @Inject
    Counter counter;

    @Inject
    Calculator calculator;

    @Inject
    LazyDependency<TesteService> testeServiceLazy;

    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_teste);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        output = findViewById(R.id.output);

        bind(R.id.btn_polymorphism, v -> testPolymorphism());
        bind(R.id.btn_constructor, v -> testConstructorQualifier());
        bind(R.id.btn_singleton, v -> testSingleton());
        bind(R.id.btn_lazy, v -> testLazy());
        bind(R.id.btn_override_instance, v -> testOverrideInstance());
        bind(R.id.btn_override_qualifier, v -> testOverrideQualifier());
        bind(R.id.btn_runtime_register, v -> testRuntimeRegister());
        bind(R.id.btn_unregister, v -> testUnregister());
        bind(R.id.btn_clear, v -> output.setText(""));

        log("App iniciado.");
        log("greeterPt: " + (greeterPt != null ? greeterPt.language() : "NULL"));
        log("greeterEn: " + (greeterEn != null ? greeterEn.language() : "NULL"));
        log("counter:   " + (counter != null ? counter.stamp() : "NULL"));
    }

    private void bind(int id, View.OnClickListener listener) {
        Button b = findViewById(id);
        if (b != null) b.setOnClickListener(listener);
    }

    // 1. Bug do registerSubTypes: 2 implementações da mesma interface não devem se sobrescrever
    private void testPolymorphism() {
        log("=== Polimorfismo ===");
        if (greeterPt == null || greeterEn == null) {
            log("FALHA: alguma injeção retornou null");
            return;
        }
        log(greeterPt.greet("Daniel"));
        log(greeterEn.greet("Daniel"));
        log("OK: ambas as implementações coexistem (lang=" + greeterPt.language()
                + " / " + greeterEn.language() + ")");
    }

    // 2. Bug do qualifier ignorado em construtor: Calculator deve ter recebido o Greeter "en"
    private void testConstructorQualifier() {
        log("=== Qualifier em construtor ===");
        if (calculator == null) {
            log("FALHA: calculator nulo");
            return;
        }
        String lang = calculator.greeterLanguage();
        log("Calculator usou Greeter: " + lang);
        log(calculator.describe(2, 3));
        if ("en".equals(lang)) {
            log("OK: qualifier do construtor respeitado");
        } else {
            log("FALHA: esperava 'en', veio '" + lang + "'");
        }
    }

    // 3. Singleton mantém estado e mesma instância através de injeções
    private void testSingleton() {
        log("=== Singleton ===");
        if (counter == null) {
            log("FALHA: campo counter nulo (injeção falhou)");
            return;
        }
        DependencyContainer c = DependencyContainerStorage.getInstance();
        Counter again = c.getDependency(Counter.class);
        if (again == null) {
            log("FALHA: getDependency(Counter) retornou null");
            return;
        }
        log("counter (campo): stamp=" + counter.stamp() + " value=" + counter.incrementAndGet());
        log("counter (lookup): stamp=" + again.stamp() + " value=" + again.incrementAndGet());
        if (counter == again && counter.stamp().equals(again.stamp())) {
            log("OK: mesma instância (singleton)");
        } else {
            log("FALHA: instâncias diferentes");
        }
    }

    // 4. LazyDependency
    private void testLazy() {
        log("=== Lazy ===");
        if (testeServiceLazy == null) {
            log("FALHA: lazy nulo");
            return;
        }
        log("isPresent: " + testeServiceLazy.isPresent());
        TesteService svc = testeServiceLazy.get();
        log("get -> " + (svc != null ? svc.getClass().getSimpleName() : "null"));
        svc.doNothing();
        log("OK: lazy resolvido com sucesso");
    }

    // 5. overrideDependency em runtime trocando a instância singleton
    private void testOverrideInstance() {
        log("=== overrideDependency(instância) ===");
        DependencyContainer c = DependencyContainerStorage.getInstance();
        Counter beforeOverride = c.getDependency(Counter.class);
        if (beforeOverride == null) {
            log("FALHA: getDependency(Counter) retornou null antes do override");
            return;
        }
        beforeOverride.incrementAndGet();
        beforeOverride.incrementAndGet();
        log("antes: stamp=" + beforeOverride.stamp() + " value=" + beforeOverride.get());

        Counter zerado = new Counter();
        try {
            c.overrideDependency(zerado);
        } catch (Exception e) {
            log("FALHA no override: " + e.getMessage());
            return;
        }

        Counter afterOverride = c.getDependency(Counter.class);
        if (afterOverride == null) {
            log("FALHA: getDependency(Counter) retornou null DEPOIS do override");
            return;
        }
        log("depois: stamp=" + afterOverride.stamp() + " value=" + afterOverride.get());
        if (afterOverride == zerado && afterOverride.get() == 0) {
            log("OK: container retorna a nova instância em runtime");
        } else {
            log("FALHA: instância não foi substituída");
        }
    }

    // 6. overrideDependency com qualifier — substitui Greeter "pt" sem afetar "en"
    private void testOverrideQualifier() {
        log("=== overrideDependency(qualifier) ===");
        DependencyContainer c = DependencyContainerStorage.getInstance();

        Greeter custom = new Greeter() {
            @Override public String greet(String name) { return "[CUSTOM] oi " + name; }
            @Override public String language() { return "pt-custom"; }
        };

        try {
            c.overrideDependency(custom, "pt");
        } catch (Exception e) {
            log("FALHA no override: " + e.getMessage());
            return;
        }

        Greeter ptNow = c.getDependency(Greeter.class, "pt");
        Greeter enNow = c.getDependency(Greeter.class, "en");
        log("pt -> " + (ptNow != null ? ptNow.greet("Daniel") + " (" + ptNow.language() + ")" : "null"));
        log("en -> " + (enNow != null ? enNow.greet("Daniel") + " (" + enNow.language() + ")" : "null"));
        if (ptNow == custom && enNow != null && "en".equals(enNow.language())) {
            log("OK: 'pt' substituído, 'en' intacto");
        } else {
            log("FALHA: override afetou outro qualifier");
        }
    }

    // 7. registerDependency via FunctionRegistrationResult em runtime
    private void testRuntimeRegister() {
        log("=== registerDependency em runtime ===");
        DependencyContainer c = DependencyContainerStorage.getInstance();

        try {
            c.registerDependency(DependencyContainerRegistor.ofAction(
                    RuntimeBean.class,
                    () -> new RuntimeBean("registrado-em-runtime"),
                    "default"
            ));
        } catch (Exception e) {
            // se já existir (botão clicado 2x), faça override
            log("nota: já existia, fazendo override -> " + e.getMessage());
            try {
                c.overrideDependency(DependencyContainerRegistor.ofAction(
                        RuntimeBean.class,
                        () -> new RuntimeBean("re-registrado"),
                        "default"));
            } catch (Exception ex) {
                log("FALHA no override: " + ex.getMessage());
                return;
            }
        }

        RuntimeBean bean = c.getDependency(RuntimeBean.class);
        log("RuntimeBean -> " + (bean != null ? bean.label() : "null"));
        if (bean != null) {
            log("OK: bean registrado em runtime e recuperável");
        } else {
            log("FALHA: bean não encontrado");
        }
    }

    // 8. unRegister limpa todas as referências (incluindo supertipos)
    private void testUnregister() {
        log("=== unRegisterDependency ===");
        DependencyContainer c = DependencyContainerStorage.getInstance();
        try {
            c.unRegisterDependency(RuntimeBean.class);
        } catch (Exception e) {
            log("erro no unregister: " + e.getMessage());
            return;
        }
        RuntimeBean bean = c.getDependency(RuntimeBean.class);
        log("RuntimeBean após unregister -> " + (bean != null ? bean.label() : "null"));
        if (bean == null) {
            log("OK: dependência removida");
        } else {
            log("FALHA: ainda retornou instância");
        }
    }

    private void log(String msg) {
        runOnUiThread(() -> output.append(msg + "\n"));
        android.util.Log.d("DI-Test", msg);
    }
}
