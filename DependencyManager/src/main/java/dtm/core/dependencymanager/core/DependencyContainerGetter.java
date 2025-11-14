package dtm.core.dependencymanager.core;

import java.util.List;
import dtm.core.dependencymanager.core.prototypes.Dependency;
import dtm.core.dependencymanager.exceptions.NewInstanceException;
import dtm.core.dependencymanager.core.prototypes.Dependency;

public interface DependencyContainerGetter {
    /**
     * Obtém uma instância da dependência associada à classe de referência.
     *
     * @param <T>       tipo da dependência esperada
     * @param reference classe que representa o tipo da dependência
     * @return instância da dependência
     */
    <T> T getDependency(Class<T> reference);

    /**
     * Obtém uma instância da dependência associada à classe de referência e qualificadora específica.
     *
     * @param <T>       tipo da dependência esperada
     * @param reference classe que representa o tipo da dependência
     * @param qualifier qualificadora que diferencia múltiplas implementações da mesma classe
     * @return instância da dependência qualificada
     */
    <T> T getDependency(Class<T> reference, String qualifier);

    /**
     * Cria uma nova instância da classe especificada, injetando as dependências automaticamente.
     *
     * @param <T>            tipo da instância a ser criada
     * @param referenceClass  classe da instância a ser criada
     * @return nova instância da classe com dependências injetadas
     * @throws NewInstanceException se ocorrer erro na criação da instância
     */
    <T> T newInstance(Class<T> referenceClass) throws NewInstanceException;


    /**
     * Injeta as dependências necessárias na instância fornecida.
     *
     * @param instance objeto onde as dependências serão injetadas
     */
    void injectDependencies(Object instance);

    /**
     * Retorna a lista de dependências registradas no contêiner.
     *
     * @return lista com as dependências registradas
     */
    List<Dependency> getRegisteredDependencies();

}
