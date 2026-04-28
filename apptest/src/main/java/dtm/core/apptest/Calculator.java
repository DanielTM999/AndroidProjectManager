package dtm.core.apptest;

import dtm.dependencymanager.annotations.Qualifier;
import dtm.dependencymanager.annotations.Service;

/**
 * Exercita a correção do bug em que o qualificador era ignorado em parâmetros
 * de construtor. Esta classe deve receber o Greeter "en" via construtor.
 */
@Service
public class Calculator {

    private final Greeter greeter;

    public Calculator(@Qualifier(qualifier = "en") Greeter greeter) {
        this.greeter = greeter;
    }

    public String describe(int a, int b) {
        return greeter.greet("user") + " result = " + (a + b);
    }

    public String greeterLanguage() {
        return greeter.language();
    }
}
