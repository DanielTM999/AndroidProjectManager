package dtm.dependencymanager.core;

import dtm.dependencymanager.exceptions.InvalidClassRegistrationException;

public interface DependencyContainer extends
        DependencyContainerGetter,
        DependencyContainerRegistor,
        DependencyContainerConfigurator
{
    /**
     * Indica se o contêiner está carregado e pronto para uso.
     *
     * @return true se o contêiner está carregado; false caso contrário.
     */
    boolean isLoaded();

    /**
     * Carrega o contêiner, registrando todas as dependências necessárias.
     *
     * @throws InvalidClassRegistrationException se ocorrer erro ao registrar alguma dependência.
     */
    void load() throws InvalidClassRegistrationException;
}
