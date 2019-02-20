import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Server {

    public static final String NUMBERS_LOG = "numbers.log";
    final ServerSocket serverSocket;
    final Map<String, List<Code>> requestMap = new ConcurrentHashMap<>();

    public Server() {
        cleanUp();
        try {
            serverSocket = new ServerSocket(4000);
            final Socket socket = serverSocket.accept();
            while (processRequest(socket)) {
            }
            socket.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    private void cleanUp() {
        if (new File(NUMBERS_LOG).exists()) new File(NUMBERS_LOG).delete();
    }

    private boolean processRequest(final Socket socket) {
        final List<String> codes = parseRequest(socket);
        if (!codes.isEmpty()) {
            //TODO optimize
            writeToFile(codes);
        }

        if (!isRequestValid(codes)) return false;

        final String remoteAddress = socket.getRemoteSocketAddress().toString();

        if (isMaxNumberOfConnections(remoteAddress)) return true;

        mapRequest(codes, remoteAddress);

        return false;
    }

    private List<String> parseRequest(final Socket socket) {
        try {
            return new BufferedReader(new InputStreamReader(socket.getInputStream()))
                    .lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    private boolean isRequestValid(final List<String> collect) {
        if (collect.stream().anyMatch
                (code -> Pattern.matches("[a-zA-Z]+", code) == false || code.length() != 9)) {
            return false;
        }
        return true;
    }

    private boolean isMaxNumberOfConnections(final String remoteAddress) {
        if (requestMap.containsKey(remoteAddress) && requestMap.get(remoteAddress).size() > 5) {
            return true;
        }
        return false;
    }

    private void mapRequest(final List<String> codes, final String remoteAddress) {
        if (requestMap.containsKey(remoteAddress)) {
            requestMap.get(remoteAddress).addAll(
                    mapToCode(remoteAddress, codes));
        } else {
            requestMap.put(remoteAddress, mapToCode(remoteAddress, codes));
        }
    }

    private List<Code> mapToCode(final String remoteAddress, final List<String> codes) {
        return codes.stream().map(
                code -> new Code(remoteAddress, code, false)).collect(Collectors.toList());
    }

    public void shutdown() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void writeToFile(final List<String> content) {
        try (FileWriter fileWriter = new FileWriter(NUMBERS_LOG)) {
            PrintWriter printWriter = new PrintWriter(fileWriter);
            content.forEach(code -> printWriter.append(code + "\n"));
            printWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server();
    }

}


