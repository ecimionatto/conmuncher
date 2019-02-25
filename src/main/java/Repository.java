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
import java.util.regex.Pattern;

public class Repository {

    public static final String NUMBERS_LOG = "numbers.log";
    private final Path path = Paths.get(NUMBERS_LOG);
    private final Pattern pattern = Pattern.compile("[0-9]+");
    private final ExecutorService persistExecutor =
            Executors.newFixedThreadPool(8);

    private final Monitor monitor;

    public Repository(final Monitor monitor) {
        cleanUp();
        this.monitor = monitor;
    }

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

    public boolean isRequestInvalid(final List<String> content) {
        return content.stream().anyMatch(code -> {
            if (code != null) {
                return !pattern.matcher(code).matches() || code.length() != 9;
            } else {
                return true;
            }
        });
    }

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
}
