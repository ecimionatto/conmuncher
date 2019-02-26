import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Class is responsible for received code persistence
 */
public class Repository {

    public static final String NUMBERS_LOG = "numbers.log";
    private final Path path = Paths.get(NUMBERS_LOG);
    private final Pattern pattern = Pattern.compile("[0-9]+");
    private final ExecutorService persistExecutor =
            Executors.newFixedThreadPool(8);

    private final Monitor monitor;

    /**
     * removes numbers.log file and re-recreates on new instantiations
     * @param monitor
     */
    public Repository(final Monitor monitor) {
        cleanUp();
        this.monitor = monitor;
    }

    /**
     * removes numbers.log file and re-recreates
     */
    static void cleanUp() {
        try {
            File numbersLog = new File(NUMBERS_LOG);
            if (numbersLog.exists()) {
                if (!numbersLog.delete()) {
                    throw new IllegalStateException("could not delete number.log");
                }
            }
            if (!numbersLog.createNewFile()) {
                throw new IllegalStateException("could not create number.log");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * validates is list of codes has 9 numeric digits
     * @param content List of codes
     * @return true is request is invalid, false when request is valid
     */
    public boolean isRequestInvalid(final List<String> content) {
        return content.stream().anyMatch(code -> {
            if (code != null) {
                return !pattern.matcher(code).matches() || code.length() != 9;
            } else {
                return true;
            }
        });
    }

    /**
     * This method persists list of codes to numbers log file. Monitor and nio files operations are
     * added to thread pool for performance reasons. Monitor operations are synchronized and should be
     * detached from receiving connection responsibilities
     *
     * @param content List of codes
     */
    public void save(final List<String> content) {
        persistExecutor.execute(() -> {
            try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.APPEND)) {
                content.forEach(code -> {
                    if (monitor.add(Integer.parseInt(code))) {
                        try {
                            writer.write(code);
                            writer.newLine();
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public void shutdown() {
        this.persistExecutor.shutdown();
        try {
            this.persistExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
