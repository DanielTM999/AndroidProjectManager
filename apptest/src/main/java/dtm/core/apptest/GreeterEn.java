package dtm.core.apptest;

import dtm.dependencymanager.annotations.Qualifier;
import dtm.dependencymanager.annotations.Service;

@Service
@Qualifier(qualifier = "en")
public class GreeterEn implements Greeter {
    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    @Override
    public String language() {
        return "en";
    }
}
