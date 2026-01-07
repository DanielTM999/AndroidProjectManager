package dtm.dependencymanager.core;

import androidx.annotation.NonNull;

import java.util.function.Supplier;

public interface FunctionRegistrationResult<T> {
    @NonNull
    Supplier<T> getFunction();
    @NonNull
    Class<T> getReferenceClass();

    @NonNull
    String getQualifier();
}
