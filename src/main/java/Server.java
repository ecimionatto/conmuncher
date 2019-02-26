import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Server {

    public static final String TERMINATE_SIGNAL = "terminate";

    private static Server serverInstance;

    private final ExecutorService connectionExecutor =
            Executors.newFixedThreadPool(5);

    private final ScheduledExecutorService reportExecutor =
            Executors.newSingleThreadScheduledExecutor();

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
        scheduleReport();
        receiveConnectionsLoop();
    }

    private void receiveConnectionsLoop() {

        try (AsynchronousServerSocketChannel server
                     = AsynchronousServerSocketChannel.open()) {

            final InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), 4000);
            ;
            try (AsynchronousServerSocketChannel serverSocketChannel = server.bind(inetSocketAddress)) {

                Future<AsynchronousSocketChannel> socket = serverSocketChannel.accept();
                while (!this.isShutdownInitiated.get()) {
                    AsynchronousSocketChannel socketChannel = null;
                    try {
                        socketChannel = socket.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new IllegalStateException(e);
                    }
                    if ((socketChannel != null) && (socketChannel.isOpen())) {
                        processRequestLoop(socketChannel);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void scheduleReport() {
        reportExecutor.scheduleAtFixedRate(() -> monitor.printReport(),
                10, 10, TimeUnit.SECONDS);
    }

    private void processRequestLoop(final AsynchronousSocketChannel socket) {
        while (socket.isOpen() && !this.isShutdownInitiated.get()) {

            ByteBuffer buffer = ByteBuffer.allocate(10);
            Future<Integer> read = socket.read(buffer);
            try {
                read.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }

            String inputLine = new String(buffer.array());
            if (!processData(inputLine)){
                buffer.clear();
                break;
            }
            buffer.clear();
        }
//        }
//
//
//        try (BufferedReader in = new BufferedReader(socket.re
//                new InputStreamReader(socket.getInputStream(), Charset.defaultCharset()))) {
//            String inputLine;
//            while ((inputLine = in.readLine()) != null && !this.isShutdownInitiated.get()) {
//                if (processData(inputLine)) break;
//            }
    }

    private boolean processData(final String inputLine) {
        if (TERMINATE_SIGNAL.equals(inputLine)) {
            this.shutdown();
            return false;
        } else {
            final List<String> receivedData = Arrays.stream(inputLine.split(System.lineSeparator()))
                    .filter(line -> line != null && !line.isEmpty())
                    .collect(Collectors.toList());
            if (repository.isRequestInvalid(receivedData)) {
                return false;
            }
            repository.save(receivedData);
        }
        return true;
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

    public static void terminate() {
        if (Server.serverInstance != null) {
            serverInstance.shutdown();
        }
    }

    public static void main(String[] args) {
        Server.getInstance();
    }

}