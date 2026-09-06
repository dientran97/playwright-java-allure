package framework.locator;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.core.PageManager;

/**
 * A selector with a business name.
 *
 * <p>Page objects declare their elements once as {@code NameLocator} constants and pass them to
 * {@code PlaywrightActions}, which uses {@link #getName()} to write readable Allure steps
 * ("Click Login Button") and {@link #resolve()} to reach the element.</p>
 *
 * <pre>{@code
 * private static final NameLocator LOGIN_BUTTON = new NameLocator("Login Button", "#loginbtn");
 * ...
 * PlaywrightActions.click(LOGIN_BUTTON);
 * }</pre>
 *
 * <p>A {@code NameLocator} keeps the <em>selector</em>, not a {@link Locator} bound to a page.
 * The element is looked up on the page that is active at the moment the action runs, which is what
 * makes the same page object work after a switch to another tab or to another browser. When a
 * Playwright {@link Locator} is already at hand (for instance one built with {@code getByRole})
 * it can still be wrapped with {@link #NameLocator(String, Locator)}; such an instance stays bound
 * to the page it was created from.</p>
 */
public final class NameLocator {

    private final String name;
    private final String locator;
    private final Locator boundLocator;
    private final String frameSelector;
    private final Integer index;

    /**
     * Creates a named selector.
     *
     * @param name    business name of the element, shown in the Allure report
     * @param locator any Playwright selector: css, xpath, {@code text=}, {@code role=}, ...
     */
    public NameLocator(final String name, final String locator) {
        this(name, locator, null, null, null);
    }

    /**
     * Wraps an already built Playwright locator.
     *
     * @param name    business name of the element, shown in the Allure report
     * @param locator the Playwright locator, bound to the page it was created from
     */
    public NameLocator(final String name, final Locator locator) {
        this(name, locator == null ? null : locator.toString(), locator, null, null);
    }

    private NameLocator(final String name, final String locator, final Locator boundLocator,
                        final String frameSelector, final Integer index) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("A NameLocator must have a name");
        }
        if (locator == null || locator.trim().isEmpty()) {
            throw new IllegalArgumentException("A NameLocator must have a locator ('" + name + "')");
        }
        this.name = name.trim();
        this.locator = locator;
        this.boundLocator = boundLocator;
        this.frameSelector = frameSelector;
        this.index = index;
    }

    /**
     * Convenience factory, equivalent to the constructor.
     *
     * @param name    business name of the element
     * @param locator Playwright selector
     * @return the new named locator
     */
    public static NameLocator of(final String name, final String locator) {
        return new NameLocator(name, locator);
    }

    /**
     * @return the business name of the element, i.e. {@code Login Button}
     */
    public String getName() {
        return name;
    }

    /**
     * @return the raw selector, i.e. {@code #loginbtn}
     */
    public String getLocator() {
        return locator;
    }

    /**
     * @return the selector of the iframe this element lives in, or {@code null}
     */
    public String getFrameSelector() {
        return frameSelector;
    }

    /**
     * Builds a copy of this locator with {@code %s} style placeholders replaced, which is how
     * dynamic elements (a row of a table, a menu entry, ...) are described.
     *
     * <pre>{@code
     * private static final NameLocator ROW = new NameLocator("Row %s", "//tr[td[text()='%s']]");
     * PlaywrightActions.click(ROW.format("Invoice 42"));
     * }</pre>
     *
     * @param arguments values injected into both the name and the selector
     * @return a new locator, this instance is left untouched
     */
    public NameLocator format(final Object... arguments) {
        return new NameLocator(formatSafely(name, arguments), formatSafely(locator, arguments),
                null, frameSelector, index);
    }

    /**
     * Builds a copy of this locator that is searched inside an iframe.
     *
     * @param iframeSelector selector of the {@code iframe} element
     * @return a new locator scoped to that frame
     */
    public NameLocator inFrame(final String iframeSelector) {
        return new NameLocator(name, locator, null, iframeSelector, index);
    }

    /**
     * Builds a copy of this locator that points at one element of a matching set.
     *
     * @param elementIndex zero based index of the wanted element
     * @return a new locator restricted to that element
     */
    public NameLocator nth(final int elementIndex) {
        return new NameLocator(name + " [" + elementIndex + "]", locator, boundLocator,
                frameSelector, elementIndex);
    }

    /**
     * Builds a copy of this locator with another business name.
     *
     * @param newName the name to show in the report
     * @return a new locator with the same selector
     */
    public NameLocator withName(final String newName) {
        return new NameLocator(newName, locator, boundLocator, frameSelector, index);
    }

    /**
     * Resolves the element on the page that is currently active, i.e. on the tab or the browser
     * the framework was last switched to.
     *
     * @return the Playwright locator ready to be acted upon
     */
    public Locator resolve() {
        return resolve(PageManager.page());
    }

    /**
     * Resolves the element on an explicit page.
     *
     * @param page the page to search in
     * @return the Playwright locator ready to be acted upon
     */
    public Locator resolve(final Page page) {
        if (boundLocator != null) {
            return index == null ? boundLocator : boundLocator.nth(index);
        }
        final Locator resolved;
        if (frameSelector == null) {
            resolved = page.locator(locator);
        } else {
            final FrameLocator frame = page.frameLocator(frameSelector);
            resolved = frame.locator(locator);
        }
        return index == null ? resolved : resolved.nth(index);
    }

    private static String formatSafely(final String template, final Object... arguments) {
        try {
            return String.format(template, arguments);
        } catch (RuntimeException e) {
            return template;
        }
    }

    @Override
    public String toString() {
        return name + " (" + locator + ")";
    }
}
