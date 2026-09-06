package framework.assertions;

import framework.locator.NameLocator;

/**
 * Hard verifications: the first one that fails stops the test case.
 *
 * <p>Before the {@link AssertionError} is thrown, the framework has already written the
 * "expected / actual" evidence in the log and in the report and taken the
 * "Screenshot at the fail step" picture.</p>
 *
 * <pre>{@code
 * CustomAssertions.assertVisible(HomePage.WELCOME_MESSAGE);
 * CustomAssertions.assertText(HomePage.USER_NAME, user.getName());
 * }</pre>
 *
 * @see SoftAssertions for verifications that let the test case continue
 */
public final class CustomAssertions {

    private static final AbstractAssertions HARD = new AbstractAssertions() {
        @Override
        protected void onFailure(final AssertionError error) {
            throw error;
        }
    };

    private CustomAssertions() {
    }

    /**
     * Verifies that a condition is true.
     *
     * @param condition   the condition to check
     * @param description what the condition means
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertTrue(final boolean condition, final String description) {
        return HARD.assertTrue(condition, description);
    }

    /**
     * Verifies that a condition is false.
     *
     * @param condition   the condition to check
     * @param description what the condition means
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertFalse(final boolean condition, final String description) {
        return HARD.assertFalse(condition, description);
    }

    /**
     * Verifies that two values are equal.
     *
     * @param actual      the value produced by the application
     * @param expected    the value the test case expects
     * @param description what is being compared
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertEquals(final Object actual, final Object expected, final String description) {
        return HARD.assertEquals(actual, expected, description);
    }

    /**
     * Verifies that two values differ.
     *
     * @param actual      the value produced by the application
     * @param unexpected  the value the test case does not want
     * @param description what is being compared
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertNotEquals(final Object actual, final Object unexpected,
                                          final String description) {
        return HARD.assertNotEquals(actual, unexpected, description);
    }

    /**
     * Verifies that a text contains a fragment.
     *
     * @param actual       the text produced by the application
     * @param expectedPart the fragment that must be present
     * @param description  what is being checked
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertContains(final String actual, final String expectedPart,
                                         final String description) {
        return HARD.assertContains(actual, expectedPart, description);
    }

    /**
     * Verifies that an element is visible.
     *
     * @param locator the element that must be visible
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertVisible(final NameLocator locator) {
        return HARD.assertVisible(locator);
    }

    /**
     * Verifies that an element is not visible.
     *
     * @param locator the element that must be hidden
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertNotVisible(final NameLocator locator) {
        return HARD.assertNotVisible(locator);
    }

    /**
     * Verifies that an element is enabled.
     *
     * @param locator the element that must be enabled
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertEnabled(final NameLocator locator) {
        return HARD.assertEnabled(locator);
    }

    /**
     * Verifies that a checkbox or a radio button is ticked.
     *
     * @param locator the checkbox or the radio button
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertChecked(final NameLocator locator) {
        return HARD.assertChecked(locator);
    }

    /**
     * Verifies the exact text of an element.
     *
     * @param locator      the element to read
     * @param expectedText the text the element must show
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertText(final NameLocator locator, final String expectedText) {
        return HARD.assertText(locator, expectedText);
    }

    /**
     * Verifies that the text of an element contains a fragment.
     *
     * @param locator      the element to read
     * @param expectedPart the fragment the element must show
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertTextContains(final NameLocator locator, final String expectedPart) {
        return HARD.assertTextContains(locator, expectedPart);
    }

    /**
     * Verifies how many elements match a locator.
     *
     * @param locator       the elements to count
     * @param expectedCount the number of elements the test case expects
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertCount(final NameLocator locator, final int expectedCount) {
        return HARD.assertCount(locator, expectedCount);
    }

    /**
     * Verifies the title of the active page.
     *
     * @param expectedTitle the title the page must have
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertPageTitle(final String expectedTitle) {
        return HARD.assertPageTitle(expectedTitle);
    }

    /**
     * Verifies that the url of the active page contains a fragment.
     *
     * @param expectedPart the fragment the url must contain
     * @return {@code true}, the method throws when the verification fails
     */
    public static boolean assertUrlContains(final String expectedPart) {
        return HARD.assertUrlContains(expectedPart);
    }

    /**
     * Fails the test case on purpose.
     *
     * @param message why the test case is failing
     * @return never returns, the method always throws
     */
    public static boolean fail(final String message) {
        return HARD.fail(message);
    }
}
