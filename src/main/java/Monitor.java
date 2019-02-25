import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class Monitor {

    private final Set<Integer> uniqueCodes = Collections.synchronizedSet(new HashSet<>());
    private final AtomicInteger repeatedCodesPerRun = new AtomicInteger();
    private final AtomicInteger uniqueCodesPerRun = new AtomicInteger();

    public Monitor() {
    }

    public synchronized boolean add(int code) {
        if (uniqueCodes.add(code)) {
            uniqueCodesPerRun.incrementAndGet();
            return true;
        } else {
            repeatedCodesPerRun.incrementAndGet();
            return false;
        }
    }

    public synchronized void printReport() {
        System.out.printf("Received %d unique numbers, %d duplicates. Unique total: %d%n",
                uniqueCodesPerRun.intValue(), repeatedCodesPerRun.intValue(), uniqueCodes.size());
        uniqueCodesPerRun.set(0);
        repeatedCodesPerRun.set(0);
    }

}

