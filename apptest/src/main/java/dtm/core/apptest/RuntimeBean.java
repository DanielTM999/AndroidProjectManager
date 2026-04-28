package dtm.core.apptest;

/**
 * Bean que NÃO é anotado com @Service — só existe se for registrado
 * manualmente em runtime via registerDependency / overrideDependency.
 */
public class RuntimeBean {

    private final String label;

    public RuntimeBean(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
