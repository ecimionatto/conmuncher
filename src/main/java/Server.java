import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Server {

    public static final String NUMBERS_LOG = "numbers.log";
    public static final String TERMINATE_SIGNAL = "terminate";

    private static Server serverInstance;

    private final ExecutorService connectionExecutor =
            Executors.newFixedThreadPool(5);

    private final ScheduledExecutorService reportExecutor =
            Executors.newSingleThreadScheduledExecutor();

    private final AtomicBoolean isShutdownInitiated = new AtomicBoolean(false);

    private final Monitor monitor = new Monitor();

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
            while (!this.isShutdownInitiated.get()) {
                Socket socket = serverSocket.accept();
                connectionExecutor.execute(() -> processRequestLoop(socket));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void scheduleReport() {
        reportExecutor.scheduleAtFixedRate(() -> monitor.printReport(),
                10, 10, TimeUnit.SECONDS);
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
            while ((inputLine = in.readLine()) != null && !this.isShutdownInitiated.get()) {
                if (TERMINATE_SIGNAL.equals(inputLine)) {
                    this.shutdown();
                    break;
                } else {
                    final List<String> receivedData = Arrays.stream(inputLine.split(System.lineSeparator()))
                            .filter(line -> line != null && !line.isEmpty())
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
        this.isShutdownInitiated.set(true);
        try {
            this.connectionExecutor.shutdown();
            this.connectionExecutor.awaitTermination(15, TimeUnit.SECONDS);
            this.reportExecutor.shutdown();
            this.reportExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            System.exit(0);
        }
    }

    private void save(final List<String> content) {
        Path path = Paths.get(NUMBERS_LOG);
        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.APPEND)) {
            content.forEach(code -> {
                if (monitor.add(Integer.parseInt(code))) {
                    try {
                        writer.write(code);
                        writer.newLine();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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