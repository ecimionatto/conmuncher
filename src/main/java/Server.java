import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Server {

    public static final String NUMBERS_LOG = "numbers.log";
    final ServerSocket serverSocket;
    final Map<String, List<Code>> requestMap = new ConcurrentHashMap<>();
    private final List<String> codes = new ArrayList<>();

    private final ThreadPoolExecutor fixedSizePoolExecutor =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(5);


    public Server() {
        cleanUp();
        try {
            serverSocket = new ServerSocket(4000);
            while (true) {
                Socket socket = serverSocket.accept();
                fixedSizePoolExecutor.execute(() -> processRequest(socket));
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    private void cleanUp() {
        File numbersLog = new File(NUMBERS_LOG);
        if (numbersLog.exists()) {
            numbersLog.delete();
        }
        try {
            numbersLog.createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean processRequest(final Socket socket) {

        try {

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if ("TERMINATE".equals(inputLine)) {
                    out.println("bye");
                    break;
                } else {
                    final List<String> codes = Arrays.stream(
                            inputLine.split(
                                    System.lineSeparator())).collect(Collectors.toList());
                    System.out.println(codes);
                    writeToFile(codes);
//                    if (isRequestInvalid(codes)) {
//                        break;
//                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return true;
    }

    private boolean isRequestInvalid(final List<String> content) {
        return content.stream().anyMatch(code ->
                Pattern.matches("[a-zA-Z]+", code) == false || code.length() != 9);
    }

    public void shutdown() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private synchronized void writeToFile(final List<String> content) {
        //try (BufferedWriter writer = new BufferedWriter(new FileWriter(NUMBERS_LOG))) {
        content.forEach(code ->
        {
            try {
                Files.write(Paths.get(NUMBERS_LOG),
                        (code + System.lineSeparator()).getBytes(),
                        StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


//        } catch (IOException e) {
//            throw new IllegalStateException(e);
//        }
    }

    public static void main(String[] args) {
        new Server();
    }

}


