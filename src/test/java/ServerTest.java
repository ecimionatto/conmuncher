import org.junit.After;
import org.junit.Before;
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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.CompletableFuture.runAsync;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerTest {

    @Before
    public void up() {
        runAsync(() -> Server.getInstance());
    }

    @After
    public void teardown() throws InterruptedException {
        Server.cleanUp();
        Server.terminate();
    }

    /*
        9. If any connected client writes a single line with only the word "terminate" followed
        by a server­native newline sequence, the Application must disconnect all clients
        and perform a clean shutdown as quickly as possible.
        10.Clearly state all of the assumptions you made in completing the Application.
     */


    @Test
    public void should1PrintReportToStdoutEvery10Secs_SingleRun() throws InterruptedException, IOException {
        final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        String repeatedCode = randomCode();
        try (final PrintWriter printWriter = new PrintWriter(
                new Socket(localAddress(), 4000).getOutputStream(),
                true)) {
            printWriter.println(randomCode() + System.lineSeparator() + repeatedCode + System.lineSeparator() + randomCode());
            printWriter.println(repeatedCode);
            printWriter.println(repeatedCode + System.lineSeparator() + randomCode());
            printWriter.println(randomCode());
        }

        TimeUnit.SECONDS.sleep(10);

        assertThat(outContent.toString(),
                equalTo("Received 5 unique numbers, 2 duplicates. Unique total: 5" + System.lineSeparator()));

        outContent.close();

    }

    @Test
    public void should2AcceptMaxOf5ConcurrentConnctions() throws IOException, InterruptedException {

        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 3);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 3);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 2);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 2);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 1);
        sendMessagesRandomMessages(new Socket(localAddress(), 4000), 1, 0);

        TimeUnit.SECONDS.sleep(1);

        try (final BufferedReader reader = new BufferedReader(new FileReader(Server.NUMBERS_LOG))) {
            assertThat(reader.readLine(), notNullValue());
            assertThat(reader.readLine(), notNullValue());
            assertThat(reader.readLine(), notNullValue());
            assertThat(reader.readLine(), notNullValue());
            assertThat(reader.readLine(), notNullValue());
            assertThat(reader.readLine(), nullValue());
        }

    }

    //2. Input lines presented to the Application via its socket must either be composed of
    //exactly nine decimal digits (e.g.: 314159265 or 007007009) immediately followed
    //by a server­native newline sequence; or a termination sequence as detailed in
    //#9, below.

    @Test
    public void should3AcceptExactlyNineDecimalsOrTermination_WhenLineSeparator() throws IOException, InterruptedException {

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

        try (BufferedReader reader = new BufferedReader(new FileReader(Server.NUMBERS_LOG))) {
            assertThat(reader.readLine(), equalTo(randomCode1));
            assertThat(reader.readLine(), equalTo(randomCode2));
            assertThat(reader.readLine(), equalTo(randomCode3));
            assertThat(reader.readLine(), equalTo(randomCode4));
            assertThat(reader.readLine(), equalTo(randomCode5));
            assertThat(reader.readLine(), equalTo(randomCode6));
            assertThat(reader.readLine(), nullValue());
        }
    }

    @Test
    public void should4AcceptExactlyNineDecimalsOrTermination_WhenInvalid10Digits_ThenEndConnection() throws IOException, InterruptedException {
        assertThatInvalidCodeIsNotAccepted("1234567890");
    }

    @Test
    public void should5AcceptExactlyNineDecimalsOrTermination_WhenInvalid8Digits_ThenEndConnection() throws IOException, InterruptedException {
        assertThatInvalidCodeIsNotAccepted("12345678");
    }

    @Test
    public void should6AcceptExactlyNineDecimalsOrTermination_WhenInvalidChars_ThenEndConnection() throws IOException, InterruptedException {
        assertThatInvalidCodeIsNotAccepted("123DF6789");
    }

    @Test
    public void should7NotPrintDuplicatesInTheLogFile() throws IOException, InterruptedException {

        String randomCode1 = randomCode();
        String repeatedCode = randomCode();
        String randomCode3 = randomCode();
        String randomCode4 = randomCode();

        try (PrintWriter printWriter = new PrintWriter(
                new Socket(localAddress(), 4000).getOutputStream(),
                true)) {

            printWriter.println(randomCode1 + System.lineSeparator() + repeatedCode + System.lineSeparator() + randomCode3);
            printWriter.println(repeatedCode);
            printWriter.println(repeatedCode + System.lineSeparator() + randomCode4);
        }

        TimeUnit.SECONDS.sleep(1);

        try (BufferedReader reader = new BufferedReader(new FileReader(Server.NUMBERS_LOG))) {
            assertThat(reader.readLine(), equalTo(randomCode1));
            assertThat(reader.readLine(), equalTo(repeatedCode));
            assertThat(reader.readLine(), equalTo(randomCode3));
            assertThat(reader.readLine(), equalTo(randomCode4));
            assertThat(reader.readLine(), nullValue());
        }
    }

    /*
        8. Every 10 seconds, the Application must print a report to standard output:
        i. The difference since the last report of the count of new unique numbers
        that have been received.
        ii. The difference since the last report of the count of new duplicate numbers
        that have been received.
        iii. The total number of unique numbers received for this run of the
        Application.
        iv. Example text for #8: Received 50 unique numbers, 2 duplicates. Unique
        total: 567231
    */

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

    private void assertThatInvalidCodeIsNotAccepted(final String invalidCode) throws IOException, InterruptedException {
        try (PrintWriter printWriter = new PrintWriter(new Socket(localAddress(), 4000).getOutputStream(), true)) {

            printWriter.println(randomCode());
            String lastValidCode = randomCode();
            printWriter.println(lastValidCode);
            printWriter.println(invalidCode);
            printWriter.println(randomCode());
            printWriter.close();

            TimeUnit.SECONDS.sleep(2);

            final BufferedReader reader = new BufferedReader(new FileReader(Server.NUMBERS_LOG));
            moveToLineThatMatchesCode(lastValidCode, reader);

            assertThat(reader.readLine(), nullValue());
        }

    }

    private int moveToLineThatMatchesCode(final String lastValidCode, final BufferedReader reader) throws IOException {
        int lenght = 0;
        try {
            do {
                lenght++;
            }
            while (!reader.readLine().equals(lastValidCode));
        } catch (NullPointerException e) {
            fail("could not found code: " + lastValidCode);
        }
        return lenght;
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

}