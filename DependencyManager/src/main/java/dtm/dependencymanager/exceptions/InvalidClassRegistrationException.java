package dtm.dependencymanager.exceptions;

public class InvalidClassRegistrationException extends Exception{
    private final Class<?> referenceClass;

    public InvalidClassRegistrationException(String message, Class<?> referenceClass, Throwable th){
        super(message, th);
        this.referenceClass = referenceClass;
    }
    public InvalidClassRegistrationException(String message, Class<?> referenceClass){
        super(message);
        this.referenceClass = referenceClass;
    }

    public Class<?> getReferenceClass(){
        return this.referenceClass;
    }
}
