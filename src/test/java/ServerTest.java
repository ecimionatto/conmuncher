import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerTest {
    public static final String INVALID = "invalid";

    private Server server;

    @Before
    public void up() {
        CompletableFuture.runAsync(() -> this.server = new Server());
    }

    @After
    public void down() {

        Optional.ofNullable(server).ifPresent(Server::shutdown);
    }

    /*

    The Application must accept input from at most 5 concurrent clients on TCP/IP
port 4000.
2. Input lines presented to the Application via its socket must either be composed of
exactly nine decimal digits (e.g.: 314159265 or 007007009) immediately followed
by a server­native newline sequence; or a termination sequence as detailed in
#9, below.
3. Numbers presented to the Application must include leading zeros as necessary
to ensure they are each 9 decimal digits.
4. The log file, to be named "numbers.log”, must be created anew and/or cleared
when the Application starts.
5. Only numbers may be written to the log file. Each number must be followed by a
server­native newline sequence.
6. No duplicate numbers may be written to the log file.
7. Any data that does not conform to a valid line of input should be discarded and
the client connection terminated immediately and without comment.

8. Every 10 seconds, the Application must print a report to standard output:
i. The difference since the last report of the count of new unique numbers
that have been received.
ii. The difference since the last report of the count of new duplicate numbers
that have been received.

iii. The total number of unique numbers received for this run of the
Application.
iv. Example text for #8: Received 50 unique numbers, 2 duplicates. Unique
total: 567231

9. If any connected client writes a single line with only the word "terminate" followed
by a server­native newline sequence, the Application must disconnect all clients
and perform a clean shutdown as quickly as possible.
10.Clearly state all of the assumptions you made in completing the Application.
     */

    @Test
    public void shouldAcceptMaxOf5CncurrentConnctions() throws IOException, InterruptedException {

        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 200);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 200);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 200);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 200);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 200);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 200);

        TimeUnit.SECONDS.sleep(5);

        final BufferedReader reader = new BufferedReader(new FileReader(Server.NUMBERS_LOG));
        assertThat(reader.readLine(), CoreMatchers.notNullValue());
        assertThat(reader.readLine(), CoreMatchers.notNullValue());
        assertThat(reader.readLine(), CoreMatchers.notNullValue());
        assertThat(reader.readLine(), CoreMatchers.notNullValue());
        assertThat(reader.readLine(), CoreMatchers.notNullValue());
        assertThat(reader.readLine(), CoreMatchers.nullValue());
    }

    private void sendMessagesRandomMessages(final Socket socket, final int numberOfMessages, final int blockSeconds) {
        CompletableFuture.runAsync( () -> {

            try (PrintWriter printWriter = new PrintWriter(
                    socket.getOutputStream(),
                    true)) {
                IntStream.range(0, numberOfMessages).forEach(i ->
                        printWriter.println(randomCode()));
                TimeUnit.SECONDS.sleep(blockSeconds);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendRandomRequestsAsNewClient(Socket socket) {
        try {
            sendMessage(socket, randomCode());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String localAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }


    @Ignore
    @Test
    public void shouldAcceptRequestsOn4000() throws IOException, InterruptedException {
        Socket clientSocket = new Socket(localAddress(), 4000);
        final String code1 = randomCode();

        PrintWriter printWriter = sendMessage(clientSocket, code1);
        final String code2 = randomCode();
        printWriter.println(code2);
        final String code3 = randomCode();
        printWriter.println(code3);

        printWriter.close();
        clientSocket.close();

        TimeUnit.SECONDS.sleep(1);

        final BufferedReader reader = new BufferedReader(new FileReader(Server.NUMBERS_LOG));
        assertThat(reader.readLine(), CoreMatchers.equalTo(code1));
        assertThat(reader.readLine(), CoreMatchers.equalTo(code2));
        assertThat(reader.readLine(), CoreMatchers.equalTo(code3));
        assertThat(reader.readLine(), CoreMatchers.nullValue());

    }

    private PrintWriter sendMessage(final Socket clientSocket, final String code1) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        printWriter.println(code1);
        return printWriter;
    }

    @Ignore
    @Test
    public void shouldTerminateWhenRequestsIsInvalid() throws IOException, InterruptedException {
        Socket clientSocket = new Socket(localAddress(), 4000);
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);

        final String code1 = randomCode();
        printWriter.println(code1);
        final String code2 = "invalid";
        printWriter.println(code2);
        final String code3 = randomCode();
        printWriter.println(code3);

        printWriter.close();
        clientSocket.close();

        TimeUnit.SECONDS.sleep(1);

        final BufferedReader reader = new BufferedReader(new FileReader(Server.NUMBERS_LOG));
        assertThat(reader.readLine(), CoreMatchers.equalTo(code1));
        assertThat(reader.readLine(), CoreMatchers.equalTo(INVALID));
        assertThat(reader.readLine(), CoreMatchers.nullValue());

    }

    private String randomCode() {
        return String.format("%09d", Math.abs(new Random().nextInt(1000000000)));
    }

    public void shouldAcceptMaxOf5ConcurrentRequests() {

    }

    public void shoudlAccept9DecimalDigits() {

    }

    public void shoudlAccept9DecimalDigitsFollowedByNewLine() {

    }

    public void shoudlTerminateClientConnectionIfPayladIsInvalid() {

    }

    public void shoudlCreateMNumbersLogFile() {

    }

}