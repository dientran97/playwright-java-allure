package framework.assertions;

import framework.logging.Log;
import framework.report.ScreenshotManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Soft verifications: a failure is recorded and the test case keeps going, then
 * {@link #assertAll()} reports everything that went wrong at once.
 *
 * <p>Each thread owns one instance, reachable through {@link #current()} and exposed by
 * {@code BaseTest.softly()}. The framework calls {@link #assertAll()} automatically at the end of
 * every test method, so a forgotten {@code assertAll()} can never hide a failure; calling it
 * explicitly in the middle of a test case is still useful when the following steps only make sense
 * once the previous verifications passed.</p>
 *
 * <pre>{@code
 * softly().assertText(HomePage.USER_NAME, user.getName());
 * softly().assertVisible(HomePage.LOGOUT_BUTTON);
 * softly().assertAll();
 * }</pre>
 */
public final class SoftAssertions extends AbstractAssertions {

    private static final ThreadLocal<SoftAssertions> CURRENT = ThreadLocal.withInitial(SoftAssertions::new);

    private final List<AssertionError> failures = Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a stand alone collector. Prefer {@link #current()} unless a test case really needs
     * several independent groups of verifications.
     */
    public SoftAssertions() {
    }

    /**
     * @return the instance bound to the current thread, the one the framework verifies
     *         automatically at the end of the test method
     */
    public static SoftAssertions current() {
        return CURRENT.get();
    }

    /**
     * Empties the collector of the current thread. Called by the framework between two test cases.
     */
    public static void resetCurrent() {
        CURRENT.get().failures.clear();
        CURRENT.remove();
    }

    /**
     * Consumes the failures collected by the current thread, used by the framework listener to fail
     * a test method whose soft verifications did not pass.
     *
     * @return the combined error, or an empty optional when everything passed
     */
    public static Optional<AssertionError> takeCollectedFailure() {
        final SoftAssertions soft = CURRENT.get();
        final Optional<AssertionError> combined = soft.combine();
        soft.failures.clear();
        return combined;
    }

    @Override
    protected void onFailure(final AssertionError error) {
        failures.add(error);
    }

    /**
     * @return {@code true} when at least one soft verification has failed
     */
    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    /**
     * @return the messages of the failures collected so far, in the order they happened
     */
    public List<String> getFailureMessages() {
        synchronized (failures) {
            return failures.stream().map(Throwable::getMessage).collect(Collectors.toList());
        }
    }

    /**
     * Reports every failure collected so far and empties the collector.
     *
     * @throws AssertionError when at least one verification failed
     */
    @Step("Verify all the soft assertions")
    public void assertAll() {
        final Optional<AssertionError> combined = combine();
        failures.clear();
        if (combined.isPresent()) {
            Log.fail("{}", combined.get().getMessage());
            Allure.addAttachment("Soft assertion failures", "text/plain",
                    combined.get().getMessage(), ".txt");
            throw combined.get();
        }
        Log.pass("All the soft assertions passed");
    }

    private Optional<AssertionError> combine() {
        synchronized (failures) {
            if (failures.isEmpty()) {
                return Optional.empty();
            }
            final StringBuilder message = new StringBuilder(failures.size() + " soft assertion(s) failed:");
            int index = 1;
            for (final AssertionError failure : failures) {
                message.append(System.lineSeparator())
                        .append(index++).append(") ").append(failure.getMessage());
            }
            final AssertionError combined = new AssertionError(message.toString());
            // every failure it aggregates was photographed when it happened
            ScreenshotManager.markAlreadyCaptured(combined);
            return Optional.of(combined);
        }
    }
}
