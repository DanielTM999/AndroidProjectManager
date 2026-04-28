package dtm.dependencymanager.core;

import java.util.function.Supplier;
import dtm.dependencymanager.exceptions.InvalidClassRegistrationException;
import lombok.NonNull;

public interface DependencyContainerRegistor {
    /**
     * Registra uma instância de dependência com um qualificadora específica.
     *
     * @param dependency objeto da dependência a ser registrado
     * @param qualifier  string qualificadora para diferenciar múltiplas implementações
     * @throws InvalidClassRegistrationException se houver erro no registro da dependência
     */
    void registerDependency(Object dependency, String qualifier) throws InvalidClassRegistrationException;

    /**
     * Registra uma instância de dependência com um qualificadora específica.
     *
     * @param dependency objeto da dependência a ser registrado
     * @param qualifier  string qualificadora para diferenciar múltiplas implementações
     * @param replace boolean qualificadora para limpar se existir intanciass antigas
     * @throws InvalidClassRegistrationException se houver erro no registro da dependência
     */
    void registerDependency(Object dependency, String qualifier, boolean replace) throws InvalidClassRegistrationException;


    /**
     * Registra uma instância de dependência com qualificadora padrão.
     *
     * @param dependency objeto da dependência a ser registrado
     * @throws InvalidClassRegistrationException se houver erro no registro da dependência
     */
    void registerDependency(Object dependency) throws InvalidClassRegistrationException;

    /**
     * Registra uma instância de dependência com qualificadora padrão.
     *
     * @param dependency objeto da dependência a ser registrado
     * @param replace boolean qualificadora para limpar se existir intanciass antigas
     * @throws InvalidClassRegistrationException se houver erro no registro da dependência
     */
    void registerDependency(Object dependency, boolean replace) throws InvalidClassRegistrationException;


    /**
     * Registra uma classe de dependência com qualificadora padrão.
     *
     * @param dependency classe da dependência a ser registrado
     * @throws InvalidClassRegistrationException se houver erro no registro da dependência
     */
    void registerDependency(Class<?> dependency) throws InvalidClassRegistrationException;

    /**
     * Registra uma classe de dependência com qualificadora padrão.
     *
     * @param dependency classe da dependência a ser registrado
     * @param replace boolean qualificadora para limpar se existir intanciass antigas
     * @throws InvalidClassRegistrationException se houver erro no registro da dependência
     */
    void registerDependency(Class<?> dependency, boolean replace) throws InvalidClassRegistrationException;


    /**
     * Registra uma dependência através de uma função de registro personalizada.
     *
     * @param <T>                 tipo da dependência a ser registrada
     * @param registrationFunction função que provê a criação ou obtenção da dependência
     * @throws InvalidClassRegistrationException se houver erro no registro da dependência
     */
    <T> void registerDependency(FunctionRegistrationResult<T> registrationFunction) throws InvalidClassRegistrationException;

    /**
     * Remove o registro da dependência associada à classe fornecida.
     *
     * @param dependency classe da dependência a ser removida
     */
    void unRegisterDependency(Class<?> dependency);

    /**
     * Sobrescreve uma dependência em runtime, mesmo que o contêiner já esteja carregado.
     * Substitui qualquer instância previamente registrada para a mesma classe e qualificadora.
     *
     * @param dependency objeto da dependência
     * @throws InvalidClassRegistrationException se houver erro durante a sobrescrita
     */
    void overrideDependency(Object dependency) throws InvalidClassRegistrationException;

    /**
     * Sobrescreve uma dependência em runtime com uma qualificadora específica.
     *
     * @param dependency objeto da dependência
     * @param qualifier  qualificadora alvo
     * @throws InvalidClassRegistrationException se houver erro durante a sobrescrita
     */
    void overrideDependency(Object dependency, String qualifier) throws InvalidClassRegistrationException;

    /**
     * Sobrescreve uma dependência registrada por classe em runtime.
     * Cria uma nova instância e substitui a anterior.
     *
     * @param dependency classe da dependência
     * @throws InvalidClassRegistrationException se houver erro durante a sobrescrita
     */
    void overrideDependency(Class<?> dependency) throws InvalidClassRegistrationException;

    /**
     * Sobrescreve uma dependência via função de registro.
     *
     * @param registrationFunction função de registro
     * @param <T> tipo
     * @throws InvalidClassRegistrationException se houver erro durante a sobrescrita
     */
    <T> void overrideDependency(FunctionRegistrationResult<T> registrationFunction) throws InvalidClassRegistrationException;

    /**
     * Cria uma função de registro padrão a partir de uma classe de referência e uma função Supplier.
     * A qualificadora padrão é "default".
     *
     * @param <T>       tipo da dependência
     * @param reference classe da dependência
     * @param action    Supplier que cria a instância da dependência
     * @return função de registro pronta para uso
     */
    static <T> FunctionRegistrationResult<T> ofAction(@NonNull Class<T> reference, @NonNull Supplier<T> action){
        return ofAction(reference, action, "default");
    }

    /**
     * Cria uma função de registro a partir de uma classe de referência, uma função Supplier e uma qualificadora.
     *
     * @param <T>       tipo da dependência
     * @param reference classe da dependência
     * @param action    Supplier que cria a instância da dependência
     * @param qualifier qualificadora para diferenciar múltiplas implementações
     * @return função de registro pronta para uso
     */
    static <T> FunctionRegistrationResult<T> ofAction(@NonNull Class<T> reference, @NonNull Supplier<T> action, @NonNull String qualifier){
        return new FunctionRegistrationResult<T>() {

            @NonNull
            @Override
            public Supplier<T> getFunction() {
                return action;
            }

            @NonNull
            @Override
            public Class<T> getReferenceClass() {
                return reference;
            }

            @NonNull
            @Override
            public String getQualifier() {
                return (qualifier.isEmpty()) ? "default" : qualifier;
            }

        };
    }
}
