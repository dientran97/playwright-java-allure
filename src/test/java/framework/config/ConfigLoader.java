package framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads {@code .properties} files used by the framework (environment files and user files).
 *
 * <p>A file is looked up on the test classpath first (i.e. {@code target/test-classes}) and,
 * when it is not there, directly under {@code src/test/resources}. That double lookup is what
 * makes the framework behave identically when it is launched by Maven and when a single test
 * class is launched from the IDE.</p>
 *
 * <p>Values may contain {@code ${placeholder}} references. A placeholder is resolved against,
 * in order: another key of the same file, a JVM system property, then an OS environment
 * variable. Unresolved placeholders are left untouched.</p>
 */
public final class ConfigLoader {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private static final String RESOURCES_ROOT = "src/test/resources";
    private static final Map<String, Properties> CACHE = new ConcurrentHashMap<>();

    private ConfigLoader() {
    }

    /**
     * Loads a properties file, caching the result for the lifetime of the JVM.
     *
     * @param resourcePath path of the file relative to {@code src/test/resources},
     *                     for example {@code environments/TEST.properties}
     * @return the parsed properties with every {@code ${placeholder}} already resolved
     * @throws IllegalStateException when the file cannot be found or cannot be parsed
     */
    public static Properties load(final String resourcePath) {
        return CACHE.computeIfAbsent(resourcePath, ConfigLoader::read);
    }

    /**
     * Tells whether a properties file exists, without loading it.
     *
     * @param resourcePath path of the file relative to {@code src/test/resources}
     * @return {@code true} when the file is available on the classpath or on disk
     */
    public static boolean exists(final String resourcePath) {
        if (CACHE.containsKey(resourcePath)) {
            return true;
        }
        try (InputStream stream = openClasspath(resourcePath)) {
            if (stream != null) {
                return true;
            }
        } catch (IOException ignored) {
            // fall through to the file system lookup
        }
        return Files.isRegularFile(onDisk(resourcePath));
    }

    /**
     * Resolves the {@code ${placeholder}} references of a single value.
     *
     * <p>Two placeholders are built in: {@code ${projectDir}} is the absolute path of the project
     * and {@code ${projectDirUri}} the same path as a {@code file:///...} uri, which is how the
     * bundled demo application is addressed in {@code LOCAL.properties}.</p>
     *
     * @param value    raw value, may be {@code null}
     * @param fallback additional key/value pairs consulted before system properties
     * @return the resolved value, or {@code null} when {@code value} was {@code null}
     */
    public static String resolvePlaceholders(final String value, final Map<Object, Object> fallback) {
        if (value == null || value.indexOf('$') < 0) {
            return value;
        }
        final Matcher matcher = PLACEHOLDER.matcher(value);
        final StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            final String key = matcher.group(1);
            String replacement = builtIn(key);
            if (replacement == null && fallback != null) {
                replacement = (String) fallback.get(key);
            }
            if (replacement == null) {
                replacement = System.getProperty(key);
            }
            if (replacement == null) {
                replacement = System.getenv(key);
            }
            matcher.appendReplacement(resolved,
                    Matcher.quoteReplacement(replacement == null ? matcher.group() : replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private static String builtIn(final String key) {
        switch (key) {
            case "projectDir":
                return FrameworkPaths.projectRoot().toString();
            case "projectDirUri":
                return FrameworkPaths.projectRoot().toUri().toString();
            default:
                return null;
        }
    }

    private static Properties read(final String resourcePath) {
        final Properties raw = new Properties();
        boolean loaded = false;
        try (InputStream stream = openClasspath(resourcePath)) {
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    raw.load(reader);
                    loaded = true;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read '" + resourcePath + "' from the classpath", e);
        }
        if (!loaded) {
            final Path file = onDisk(resourcePath);
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("Configuration file not found: " + RESOURCES_ROOT + "/"
                        + resourcePath + " (also looked it up on the test classpath)");
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                raw.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read " + file, e);
            }
        }
        return resolveAll(raw);
    }

    private static Properties resolveAll(final Properties raw) {
        final Map<Object, Object> snapshot = new LinkedHashMap<>(raw);
        final Properties resolved = new Properties();
        raw.stringPropertyNames()
                .forEach(key -> resolved.setProperty(key, resolvePlaceholders(raw.getProperty(key), snapshot)));
        return resolved;
    }

    private static InputStream openClasspath(final String resourcePath) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? null : loader.getResourceAsStream(resourcePath);
    }

    private static Path onDisk(final String resourcePath) {
        return Paths.get(FrameworkPaths.projectRoot().toString(), RESOURCES_ROOT, resourcePath);
    }
}
