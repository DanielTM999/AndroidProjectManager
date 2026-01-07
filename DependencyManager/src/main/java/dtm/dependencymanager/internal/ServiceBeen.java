package dtm.dependencymanager.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class ServiceBeen implements Comparable<ServiceBeen>{
    private Class<?> clazz;
    private long dependencyOrder;

    @Override
    public int compareTo(ServiceBeen o) {
        return Long.compare(this.dependencyOrder, o.dependencyOrder);
    }
}
