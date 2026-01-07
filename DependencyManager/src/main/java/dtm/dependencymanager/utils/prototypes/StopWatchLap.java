package dtm.dependencymanager.utils.prototypes;

public interface StopWatchLap {

    /**
     * Tempo Inicial: Tempo do início (Start).
     */
    long getStartTime();

    /**
     * Tempo Do Lap: Tempo do Lap
     */
    long getLapTime();

    /**
     * @return Duração apenas desta volta (Delta).
     * (TotalTime - LapStartTime).
     */
    long getLapTimeDuration();

    /**
     * Tempo do Último Lap ate o momento
     */
    long getLapStartTime();

    /**
     * @return Tempo total decorrido desde o Start até este Lap.
     */
    long getTotalTime();

    String getTag();
}
