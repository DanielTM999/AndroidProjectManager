package dtm.core.apptest;

import dtm.dependencymanager.annotations.Qualifier;
import dtm.dependencymanager.annotations.Service;

@Service
@Qualifier(qualifier = "pt")
public class GreeterPt implements Greeter {
    @Override
    public String greet(String name) {
        return "Olá, " + name + "!";
    }

    @Override
    public String language() {
        return "pt";
    }
}
