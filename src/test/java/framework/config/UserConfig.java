package framework.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Credentials and profile data of one test user, read from
 * {@code src/test/resources/users/<user>.properties}.
 *
 * <p>The file is selected with {@code -Duser=<name>} on the command line (see {@code run.sh}) and
 * defaults to {@link FrameworkConfig#DEFAULT_USER}. A password may be stored in clear text or
 * Base64 encoded, in which case the file must also declare {@code password.encoded=true}.</p>
 */
public final class UserConfig {

    private static final Map<String, UserConfig> CACHE = new ConcurrentHashMap<>();

    private final String userKey;
    private final Properties properties;

    private UserConfig(final String userKey, final Properties properties) {
        this.userKey = userKey;
        this.properties = properties;
    }

    /**
     * @return the user selected for the current run, i.e. the one behind {@code -Duser=...}
     */
    public static UserConfig current() {
        return of(FrameworkConfig.userName());
    }

    /**
     * Loads any user file, which is handy when a test case needs a second account.
     *
     * @param userKey file name without extension, i.e. {@code User2} for {@code users/User2.properties}
     * @return the cached configuration of that user
     */
    public static UserConfig of(final String userKey) {
        return CACHE.computeIfAbsent(userKey,
                key -> new UserConfig(key, ConfigLoader.load("users/" + key + ".properties")));
    }

    /**
     * @return the key of the user file, i.e. {@code User1}
     */
    public String getUserKey() {
        return userKey;
    }

    /**
     * @return the login name of the user
     */
    public String getUsername() {
        return required("username");
    }

    /**
     * @return the password of the user, already Base64 decoded when {@code password.encoded=true}
     */
    public String getPassword() {
        final String raw = required("password");
        if (!Boolean.parseBoolean(properties.getProperty("password.encoded", "false"))) {
            return raw;
        }
        try {
            return new String(Base64.getDecoder().decode(raw.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("password of '" + userKey
                    + "' is flagged as Base64 encoded but cannot be decoded", e);
        }
    }

    /**
     * @return the display name of the user, i.e. the name shown once logged in
     */
    public String getName() {
        return properties.getProperty("name", "");
    }

    /**
     * @return the e-mail address of the user
     */
    public String getEmail() {
        return properties.getProperty("email", "");
    }

    /**
     * Reads any additional key of the user file.
     *
     * @param key          property key
     * @param defaultValue value returned when the key is absent
     * @return the configured value or {@code defaultValue}
     */
    public String get(final String key, final String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private String required(final String key) {
        final String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing '" + key + "' in users/" + userKey + ".properties");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return "UserConfig{" + userKey + ", username=" + properties.getProperty("username") + "}";
    }
}
