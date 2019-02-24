import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.CompletableFuture.runAsync;

@Ignore
public class StressTest {

    @Test
    public void stressTest() throws IOException, InterruptedException {
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 400000);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 400000);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 400000);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 400000);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 100000);

        TimeUnit.MINUTES.sleep(5);
    }

    private String localAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private String randomCode() {
        return String.format("%09d", Math.abs(new Random().nextInt(1000000000)));
    }

    private void sendMessagesRandomMessages(final Socket socket, final int numberOfMessages) {
        runAsync(() -> {
            try (PrintWriter printWriter = new PrintWriter(
                    socket.getOutputStream(),
                    true)) {
                IntStream.range(0, numberOfMessages).forEach(i ->
                        printWriter.println(randomCode()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}