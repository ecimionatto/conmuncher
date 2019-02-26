import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitor object is responsible for atomically incrementing counters used by report statements
 */
public final class Monitor {

    private final ScheduledExecutorService reportExecutor =
            Executors.newSingleThreadScheduledExecutor();

    private final Set<Integer> uniqueCodes = Collections.synchronizedSet(new HashSet<>());
    private final AtomicInteger repeatedCodesPerRun = new AtomicInteger();
    private final AtomicInteger uniqueCodesPerRun = new AtomicInteger();

    public Monitor() {
        reportExecutor.scheduleAtFixedRate(() -> this.printReport(),
                10, 10, TimeUnit.SECONDS);
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

    public void shutdown() {
        this.reportExecutor.shutdown();
        try {
            this.reportExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}

