package dtm.dependencymanager.core;

public interface DependencyContainerConfigurator {
    /**
     * Habilita o registro automático de dependências filhas.
     *
     * Quando habilitado, o contêiner também registra dependências associadas
     * às subclasses ou dependências relacionadas automaticamente.
     */
    void enableChildrenRegistration();
    /**
     * Desabilita o registro automático de dependências filhas.
     */
    void disableChildrenRegistration();

    /**
     * Define a estratégia de injeção de dependências utilizada pelo contêiner.
     *
     * A estratégia controla como o contêiner executa o processo de injeção:
     * - Dependendo do modo selecionado, a injeção pode ser executada de forma
     *   paralela, sequencial ou adaptativa conforme o volume de dependências.
     *
     * @param strategy A estratégia de injeção a ser utilizada pelo contêiner.
     *                 Não deve ser {@code null}.
     */
    void setInjectionStrategy(InjectionStrategy strategy);

    void enableLog();

    void disableLog();
}
