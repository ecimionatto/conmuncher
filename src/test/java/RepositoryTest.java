import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepositoryTest {

    @Ignore
    @Test
    public void shouldCreateNumbersLogAndThenCleanUpAndCreateAnotherFile() {
        File file = new File(Repository.NUMBERS_LOG);
        if (file.exists()) {
            file.delete();
        }
        new Repository(null);
        assertTrue(new File(Repository.NUMBERS_LOG).exists());
    }

    @Test
    public void shouldSave() throws InterruptedException, IOException {
        Repository repository = new Repository(new Monitor());
        repository.save(Arrays.asList("123456789", "000000001"));
        repository.save(Arrays.asList("987654321"));
        TimeUnit.SECONDS.sleep(1);
        try (BufferedReader reader = new BufferedReader(new FileReader(Repository.NUMBERS_LOG))) {
            List<String> lines = reader.lines().collect(toList());
            assertThat(lines.size(), equalTo(3));
            assertTrue(lines.stream().anyMatch(line -> line.equals("123456789")));
            assertTrue(lines.stream().anyMatch(line -> line.equals("000000001")));
            assertTrue(lines.stream().anyMatch(line -> line.equals("987654321")));
        }
    }

    @Test
    public void shouldValidateCode() {
        assertFalse(new Repository(null).isRequestInvalid(Arrays.asList("123456789" , "000000001" )));
        assertFalse(new Repository(null).isRequestInvalid(Arrays.asList("123456789")));
        assertTrue(new Repository(null).isRequestInvalid(Arrays.asList("12345678")));
        assertTrue(new Repository(null).isRequestInvalid(Arrays.asList("123SS678")));
    }

}