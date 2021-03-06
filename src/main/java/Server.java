import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Singleton responsible for socket receiving connections
 * accepts 5 client connections enforced by thread pool
 */
public class Server {

    public static final String TERMINATE_SIGNAL = "terminate";

    private static Server serverInstance;

    private final ExecutorService connectionExecutor =
            Executors.newFixedThreadPool(5);

    private final AtomicBoolean isShutdownInitiated = new AtomicBoolean(false);

    private final Monitor monitor;
    private final Repository repository;

    public synchronized static Server getInstance() {
        if (Server.serverInstance == null) {
            Server.serverInstance = new Server();
        }
        return Server.serverInstance;
    }

    private Server() {
        this.monitor = new Monitor();
        this.repository = new Repository(monitor);
        receiveConnectionsLoop();
    }

    /**
     * accepts connections on port 4000
     * when shutdown is initialized it stops processing
     */
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

    /**
     * reads input stream and delegate persistence of file
     * when shutdown is initialized it stops processing
     */
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
                    if (repository.isRequestInvalid(receivedData)) {
                        break;
                    }
                    repository.save(receivedData);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * attempts a clean shutdown of all thread pools
     */
    public void shutdown() {
        this.isShutdownInitiated.set(true);
        try {
            this.connectionExecutor.shutdown();
            this.connectionExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            repository.shutdown();
            monitor.shutdown();
            System.exit(0);
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