import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class MonitorTest {

    @Test
    public void shouldAddUnique_ReturnTrue_ReportUniques_2Runs() throws IOException, InterruptedException {
        final Monitor monitor = new Monitor();
        assertTrue(monitor.add(randomCode()));
        assertTrue(monitor.add(randomCode()));
        int repeated = randomCode();
        assertTrue(monitor.add(repeated));
        assertFalse(monitor.add(repeated));
        assertTrue(monitor.add(randomCode()));
        assertTrue(monitor.add(randomCode()));
        assertReport(monitor, 5, 1, 5);

        assertTrue(monitor.add(randomCode()));
        assertTrue(monitor.add(randomCode()));
        assertFalse(monitor.add(repeated));
        assertFalse(monitor.add(repeated));
        assertTrue(monitor.add(randomCode()));
        assertTrue(monitor.add(randomCode()));
        assertReport(monitor, 4, 2, 9);
    }

    private int randomCode() {
        return Math.abs(new Random().nextInt(1000000000));
    }

    private void assertReport(final Monitor monitor, final int uniques, final int duplicated, final int total) throws IOException, InterruptedException {
        try (final ByteArrayOutputStream outContent = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(outContent));
            monitor.printReport();
            assertThat(outContent.toString(),
                    equalTo("Received " + uniques + " unique numbers, " + duplicated + " duplicates. Unique total: " + total + System.lineSeparator()));
        }
    }


}