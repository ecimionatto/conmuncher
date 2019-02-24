import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Server {

    public static final String NUMBERS_LOG = "numbers.log";
    public static final String TERMINATE_SIGNAL = "terminate";

    private static Server serverInstance;

    private final Set<Integer> uniqueCodes = Collections.synchronizedSet(new HashSet<>());
    private final AtomicInteger repeatedCodesPerRun = new AtomicInteger();
    private final AtomicInteger uniqueCodesPerRun = new AtomicInteger();

    private final ExecutorService connectionExecutor =
            Executors.newFixedThreadPool(5);

    private final ScheduledExecutorService reportExecutor =
            Executors.newSingleThreadScheduledExecutor();

    private AtomicBoolean shutdown = new AtomicBoolean(false);

    public synchronized static Server getInstance() {
        if (Server.serverInstance == null) {
            Server.serverInstance = new Server();
        }
        return Server.serverInstance;
    }

    private Server() {
        cleanUp();
        scheduleReport();
        receiveConnectionsLoop();
    }

    private void receiveConnectionsLoop() {
        try (ServerSocket serverSocket = new ServerSocket(4000)) {
            while (!this.shutdown.get()) {
                Socket socket = serverSocket.accept();
                connectionExecutor.execute(() -> processRequestLoop(socket));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void scheduleReport() {
        reportExecutor.scheduleAtFixedRate(() -> printReport(), 10, 10, TimeUnit.SECONDS);
    }

    private void printReport() {
        System.out.printf("Received %d unique numbers, %d duplicates. Unique total: %d%n",
                uniqueCodesPerRun.intValue(), repeatedCodesPerRun.intValue(), uniqueCodes.size());
        repeatedCodesPerRun.set(0);
        uniqueCodesPerRun.set(0);
    }

    public static void cleanUp() {
        try {
            File numbersLog = new File(NUMBERS_LOG);
            if (numbersLog.exists()) {
                if (!numbersLog.delete()) {
                    throw new IllegalStateException("could not delete number.log");
                }
            }
            if (!numbersLog.createNewFile()) {
                throw new IllegalStateException("could not create number.log");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void processRequestLoop(final Socket socket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), Charset.defaultCharset()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (TERMINATE_SIGNAL.equals(inputLine)) {
                    this.shutdown();
                    break;
                } else {
                    final List<String> receivedData = Arrays.stream(inputLine.split(System.lineSeparator()))
                            .collect(Collectors.toList());
                    if (isRequestInvalid(receivedData)) {
                        break;
                    }
                    save(receivedData);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isRequestInvalid(final List<String> content) {
        return content.stream().anyMatch(code -> {
            if (code != null) {
                return !Pattern.matches("[0-9]+", code) || code.length() != 9;
            } else {
                return true;
            }
        });
    }

    public void shutdown() {
        this.shutdown = new AtomicBoolean(true);
        this.connectionExecutor.shutdown();
        this.reportExecutor.shutdown();
        try {
            this.connectionExecutor.awaitTermination(1, TimeUnit.MINUTES);
            this.reportExecutor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private synchronized void save(final List<String> content) {
        content.forEach(code -> {
            if (uniqueCodes.add(Integer.parseInt(code))) {
                uniqueCodesPerRun.incrementAndGet();
                try {
                    Files.write(Paths.get(NUMBERS_LOG),
                            (code + System.lineSeparator()).getBytes(),
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                repeatedCodesPerRun.incrementAndGet();
            }
        });
    }

    public static void terminate() {
        if (Server.serverInstance != null) {
            serverInstance.shutdown();
        }
    }

    public static void main(String[] args) {
        Server.getInstance();
    }

}