package framework.assertions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import framework.config.FrameworkConfig;
import framework.core.PageManager;
import framework.locator.NameLocator;
import io.qameta.allure.Step;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The verification vocabulary of the framework.
 *
 * <p>The class holds the checks themselves; what happens when one of them fails is decided by the
 * subclass through {@link #onFailure(AssertionError)}: {@link CustomAssertions} stops the test case
 * immediately, {@link SoftAssertions} records the failure and lets the test case go on.</p>
 *
 * <p>Every check is an Allure step and returns a boolean, so a test can branch on the outcome of a
 * soft verification.</p>
 *
 * <p>The {@code @Step} templates address their arguments by position ({@code {0}}, {@code {1}}) and
 * not by name. Allure fills the name keys from the parameter names the AspectJ weaver reads out of
 * the class file, which some JDK and compiler combinations do not publish - the report then shows
 * the raw {@code {locator.name}} instead of the element. The index keys are built from the argument
 * values themselves, so they always resolve.</p>
 */
public abstract class AbstractAssertions {

    /**
     * Decides what to do with a failed verification.
     *
     * @param error the error describing the failure
     */
    protected abstract void onFailure(AssertionError error);

    /**
     * Verifies that a condition is true.
     *
     * @param condition   the condition to check
     * @param description what the condition means, shown in the report
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {1}")
    public boolean assertTrue(final boolean condition, final String description) {
        return AssertionEngine.verify(description, true, () -> condition, Boolean.TRUE::equals, this::onFailure);
    }

    /**
     * Verifies that a condition is false.
     *
     * @param condition   the condition to check
     * @param description what the condition means, shown in the report
     * @return {@code true} when the verification passed
     */
    @Step("Verify that NOT {1}")
    public boolean assertFalse(final boolean condition, final String description) {
        return AssertionEngine.verify(description, false, () -> condition, Boolean.FALSE::equals, this::onFailure);
    }

    /**
     * Verifies that two values are equal.
     *
     * @param actual      the value produced by the application
     * @param expected    the value the test case expects
     * @param description what is being compared, shown in the report
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {2} is '{1}'")
    public boolean assertEquals(final Object actual, final Object expected, final String description) {
        return AssertionEngine.verify(description, expected, () -> actual,
                value -> Objects.equals(value, expected), this::onFailure);
    }

    /**
     * Verifies that two values differ.
     *
     * @param actual      the value produced by the application
     * @param unexpected  the value the test case does not want
     * @param description what is being compared, shown in the report
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {2} is not '{1}'")
    public boolean assertNotEquals(final Object actual, final Object unexpected, final String description) {
        return AssertionEngine.verify(description, "anything but " + unexpected, () -> actual,
                value -> !Objects.equals(value, unexpected), this::onFailure);
    }

    /**
     * Verifies that a text contains a fragment.
     *
     * @param actual      the text produced by the application
     * @param expectedPart the fragment that must be present
     * @param description  what is being checked, shown in the report
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {2} contains '{1}'")
    public boolean assertContains(final String actual, final String expectedPart, final String description) {
        return AssertionEngine.verify(description, "a text containing '" + expectedPart + "'",
                () -> actual, value -> value != null && value.contains(expectedPart), this::onFailure);
    }

    /**
     * Verifies that an element is visible, waiting for it up to the configured timeout.
     *
     * @param locator the element that must be visible
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {0.name} is displayed")
    public boolean assertVisible(final NameLocator locator) {
        return AssertionEngine.verify(locator.getName() + " is displayed", "displayed",
                () -> state(locator, WaitForSelectorState.VISIBLE) ? "displayed" : "not displayed",
                "displayed"::equals, this::onFailure);
    }

    /**
     * Verifies that an element is not visible, waiting for it to disappear up to the configured
     * timeout.
     *
     * @param locator the element that must be hidden
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {0.name} is not displayed")
    public boolean assertNotVisible(final NameLocator locator) {
        return AssertionEngine.verify(locator.getName() + " is not displayed", "not displayed",
                () -> state(locator, WaitForSelectorState.HIDDEN) ? "not displayed" : "displayed",
                "not displayed"::equals, this::onFailure);
    }

    /**
     * Verifies that an element is enabled.
     *
     * @param locator the element that must be enabled
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {0.name} is enabled")
    public boolean assertEnabled(final NameLocator locator) {
        return AssertionEngine.verify(locator.getName() + " is enabled", true,
                () -> locator.resolve().isEnabled(), Boolean.TRUE::equals, this::onFailure);
    }

    /**
     * Verifies that a checkbox or a radio button is ticked.
     *
     * @param locator the checkbox or the radio button
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {0.name} is checked")
    public boolean assertChecked(final NameLocator locator) {
        return AssertionEngine.verify(locator.getName() + " is checked", true,
                () -> locator.resolve().isChecked(), Boolean.TRUE::equals, this::onFailure);
    }

    /**
     * Verifies the exact text of an element, ignoring the surrounding blanks.
     *
     * @param locator      the element to read
     * @param expectedText the text the element must show
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {0.name} shows '{1}'")
    public boolean assertText(final NameLocator locator, final String expectedText) {
        return AssertionEngine.verify("the text of " + locator.getName(), expectedText,
                () -> text(locator), value -> Objects.equals(value, expectedText), this::onFailure);
    }

    /**
     * Verifies that the text of an element contains a fragment.
     *
     * @param locator      the element to read
     * @param expectedPart the fragment the element must show
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {0.name} contains '{1}'")
    public boolean assertTextContains(final NameLocator locator, final String expectedPart) {
        return AssertionEngine.verify("the text of " + locator.getName(),
                "a text containing '" + expectedPart + "'", () -> text(locator),
                value -> value != null && value.contains(expectedPart), this::onFailure);
    }

    /**
     * Verifies how many elements match a locator.
     *
     * @param locator       the elements to count
     * @param expectedCount the number of elements the test case expects
     * @return {@code true} when the verification passed
     */
    @Step("Verify that {0.name} matches {1} element(s)")
    public boolean assertCount(final NameLocator locator, final int expectedCount) {
        return AssertionEngine.verify("the number of " + locator.getName(), expectedCount,
                () -> locator.resolve().count(), value -> value == expectedCount, this::onFailure);
    }

    /**
     * Verifies the title of the active page.
     *
     * @param expectedTitle the title the page must have
     * @return {@code true} when the verification passed
     */
    @Step("Verify that the page title is '{0}'")
    public boolean assertPageTitle(final String expectedTitle) {
        return AssertionEngine.verify("the page title", expectedTitle,
                () -> PageManager.page().title(), value -> Objects.equals(value, expectedTitle),
                this::onFailure);
    }

    /**
     * Verifies that the url of the active page contains a fragment.
     *
     * @param expectedPart the fragment the url must contain
     * @return {@code true} when the verification passed
     */
    @Step("Verify that the url contains '{0}'")
    public boolean assertUrlContains(final String expectedPart) {
        return AssertionEngine.verify("the current url", "a url containing '" + expectedPart + "'",
                () -> PageManager.page().url(), value -> value != null && value.contains(expectedPart),
                this::onFailure);
    }

    /**
     * Reports a failure without comparing anything, for a branch a test case must never reach.
     *
     * @param message why the test case is failing
     * @return always {@code false}
     */
    @Step("Fail: {0}")
    public boolean fail(final String message) {
        return AssertionEngine.verify(message, "no failure", () -> "failed", "no failure"::equals,
                this::onFailure);
    }

    private static String text(final NameLocator locator) {
        final Locator resolved = locator.resolve();
        resolved.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(FrameworkConfig.defaultTimeoutMs()));
        return resolved.innerText().trim();
    }

    private static boolean state(final NameLocator locator, final WaitForSelectorState expected) {
        return waitQuietly(() -> {
            locator.resolve().waitFor(new Locator.WaitForOptions()
                    .setState(expected)
                    .setTimeout(FrameworkConfig.defaultTimeoutMs()));
            return true;
        });
    }

    private static boolean waitQuietly(final Supplier<Boolean> wait) {
        try {
            return Boolean.TRUE.equals(wait.get());
        } catch (RuntimeException e) {
            return false;
        }
    }
}
