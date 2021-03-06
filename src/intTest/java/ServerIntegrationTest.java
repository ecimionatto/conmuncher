import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * This integration test class should be executed in totality,
 * <p>
 * individual tests are expected to fail because of reporting assertions
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerIntegrationTest {

    @BeforeClass
    public static void waitForOperations() throws InterruptedException {
        Server.terminate();
        TimeUnit.SECONDS.sleep(15);
    }

    @Before
    public void up() throws InterruptedException {
        Repository.cleanUp();
        runAsync(Server::getInstance);
        TimeUnit.MILLISECONDS.sleep(300);
    }

    @After
    public void teardown() {
        Server.terminate();
    }

    @Test
    public void should1AcceptMaxOf5ConcurrentConnctions() throws IOException, InterruptedException {
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 3);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 3);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 2);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 2);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 1);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 0);

        TimeUnit.SECONDS.sleep(1);

        try (final BufferedReader reader = new BufferedReader(new FileReader(Repository.NUMBERS_LOG))) {
            assertThat(reader.lines().count(), equalTo(5L));
        }

        assertReport(6, 0, 6);

    }

    /**
     * 2. Input lines presented to the Application via its socket must either be composed of
     * exactly nine decimal digits (e.g.: 314159265 or 007007009) immediately followed
     * by a server­native newline sequence; or a termination sequence as detailed in
     * #9, below.
     **/

    @Test
    public void should2AcceptExactlyNineDecimalsOrTermination_UsingLineSeparator() throws IOException, InterruptedException {

        String randomCode1 = randomCode();
        String randomCode2 = randomCode();
        String randomCode3 = randomCode();
        String randomCode4 = randomCode();
        String randomCode5 = randomCode();
        String randomCode6 = randomCode();

        try (final PrintWriter printWriter = new PrintWriter(
                new Socket(localAddress(), 4000).getOutputStream(),
                true)) {

            printWriter.println(randomCode1 + System.lineSeparator() + randomCode2 + System.lineSeparator() + randomCode3);
            printWriter.println(randomCode4);
            printWriter.println(randomCode5 + System.lineSeparator() + randomCode6);
        }
        TimeUnit.SECONDS.sleep(2);

        try (BufferedReader reader = new BufferedReader(new FileReader(Repository.NUMBERS_LOG))) {
            List<String> lines = reader.lines().collect(toList());
            assertThat(lines.size(), equalTo(6));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode1)));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode2)));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode3)));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode4)));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode5)));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode6)));
        }

        assertReport(6, 0, 12);

    }

    @Test
    public void should3AcceptExactlyNineDecimalsOrTermination_WhenInvalid10Digits_ThenEndConnection() throws IOException, InterruptedException {
        assertThatConnectionIsTerminatedAfterCode("1234567890");
        //2 valid codes are sent before invalid
        assertReport(2, 0, 14);
    }

    @Test
    public void should4AcceptExactlyNineDecimalsOrTermination_WhenInvalid8Digits_ThenEndConnection() throws IOException, InterruptedException {
        assertThatConnectionIsTerminatedAfterCode("12345678");
        //2 valid codes are sent before invalid
        assertReport(2, 0, 16);
    }

    @Test
    public void should5AcceptExactlyNineDecimalsOrTermination_WhenInvalidChars_ThenEndConnection() throws IOException, InterruptedException {
        assertThatConnectionIsTerminatedAfterCode("123DF6789");
        //2 valid codes are sent before invalid
        assertReport(2, 0, 18);
    }


    /**
     * 8. Every 10 seconds, the Application must print a report to standard output:
     * i. The difference since the last report of the count of new unique numbers
     * that have been received.
     * ii. The difference since the last report of the count of new duplicate numbers
     * that have been received.
     * iii. The total number of unique numbers received for this run of the
     * Application.
     * iv. Example text for #8: Received 50 unique numbers, 2 duplicates. Unique
     * total: 567231
     */

    @Test
    public void should6NotPrintDuplicatesInTheLogFile() throws IOException, InterruptedException {

        final String randomCode1 = randomCode();
        final String repeatedCode = randomCode();
        final String randomCode3 = randomCode();
        final String randomCode4 = randomCode();

        try (PrintWriter printWriter = new PrintWriter(
                new Socket(localAddress(), 4000).getOutputStream(),
                true)) {

            printWriter.println(randomCode1 + System.lineSeparator() + repeatedCode + System.lineSeparator() + randomCode3);
            printWriter.println(repeatedCode);
            printWriter.println(repeatedCode + System.lineSeparator() + randomCode4);
        }

        TimeUnit.SECONDS.sleep(1);

        try (BufferedReader reader = new BufferedReader(new FileReader(Repository.NUMBERS_LOG))) {
            List<String> lines = reader.lines().collect(toList());
            assertThat(lines.size(), equalTo(4));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode1)));
            assertTrue(lines.stream().anyMatch(line -> line.equals(repeatedCode)));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode3)));
            assertTrue(lines.stream().anyMatch(line -> line.equals(randomCode4)));
        }

        assertReport(4, 2, 22);

    }

    /**
     * 9. If any connected client writes a single line with only the word "terminate" followed
     * by a server­native newline sequence, the Application must disconnect all clients
     * and perform a clean shutdown as quickly as possible.
     * 10.Clearly state all of the assumptions you made in completing the Application.
     */
    @Test(expected = java.net.SocketException.class)
    public void should7TerminateAndStopReceivingConnections() throws IOException, InterruptedException {
        try (Socket socket = new Socket(localAddress(), 4000)) {
            try (PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true)) {
                printWriter.println("terminate" + System.lineSeparator());
            }
            socket.connect(new InetSocketAddress(localAddress(), 4000), 1000);
        }
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

    private void assertThatConnectionIsTerminatedAfterCode(final String invalidCode) throws IOException, InterruptedException {
        try (PrintWriter printWriter = new PrintWriter(new Socket(localAddress(), 4000).getOutputStream(), true)) {
            String validCode = randomCode();
            printWriter.println(validCode);
            String lastValidCode = randomCode();
            printWriter.println(lastValidCode);
            printWriter.println(invalidCode);
            printWriter.println(randomCode());

            TimeUnit.SECONDS.sleep(1);

            try (BufferedReader reader = new BufferedReader(new FileReader(Repository.NUMBERS_LOG))) {
                List<String> lines = reader.lines().collect(toList());
                assertThat(lines.size(), equalTo(2));
                assertTrue(lines.stream().anyMatch(line -> line.equals(validCode)));
                assertTrue(lines.stream().anyMatch(line -> line.equals(lastValidCode)));
            }
        }

    }

    private void sendMessagesRandomMessages(final Socket socket, final int numberOfMessages, final int blockSeconds) {
        runAsync(() -> {
            try (PrintWriter printWriter = new PrintWriter(
                    socket.getOutputStream(),
                    true)) {
                IntStream.range(0, numberOfMessages).forEach(i ->
                        printWriter.println(randomCode()));
                TimeUnit.SECONDS.sleep(blockSeconds);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void assertReport(final int uniques, final int duplicated, final int total) throws IOException, InterruptedException {
        try (final ByteArrayOutputStream outContent = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(outContent));
            TimeUnit.SECONDS.sleep(9);
            assertThat(outContent.toString(),
                    equalTo("Received " + uniques + " unique numbers, " + duplicated + " duplicates. Unique total: " + total + System.lineSeparator()));
        }
    }

}