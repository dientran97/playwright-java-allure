package framework.actions;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import framework.config.FrameworkConfig;
import framework.config.UserConfig;
import framework.core.BrowserManager;
import framework.core.FrameworkException;
import framework.core.PageManager;
import framework.locator.NameLocator;
import framework.logging.Log;
import framework.report.DownloadManager;
import framework.report.ScreenshotManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.Step;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * The keyword library of the framework: a thin, logged and reported layer on top of Playwright.
 *
 * <p>Every keyword</p>
 * <ul>
 *     <li>is an Allure {@code @Step}, so it shows up in the report as
 *         {@code <start step time>    <log level>    <step description>};</li>
 *     <li>works on the page the framework is currently switched to, so a keyword called after
 *         {@link #switchToTab(int)} or {@link #switchToBrowser(String)} acts on the new target
 *         without any page object having to know about it;</li>
 *     <li>attaches the error and a screenshot named "Screenshot at the fail step" when it fails,
 *         then rethrows a {@link FrameworkException} so the test case really fails.</li>
 * </ul>
 *
 * <p>Page objects call these keywords with {@link NameLocator}s; test classes call the high level
 * keywords of the page objects.</p>
 */
public final class PlaywrightActions {

    private PlaywrightActions() {
    }

    // ------------------------------------------------------------------ browser lifecycle

    /**
     * Opens the browser configured for the run, sets the HTTP basic credentials on the browser
     * context and navigates to the url. This is the keyword a test case starts with; the base test
     * class deliberately never opens a browser on its own.
     *
     * @param url      address to open, i.e. the {@code url} of the environment file
     * @param username user of the HTTP basic authentication dialog, {@code null} or empty to skip it
     * @param password password of the HTTP basic authentication dialog
     * @return the page that became active
     */
    @Step("Open the browser and navigate to {url}")
    public static Page openBrowserAndNavigate(final String url, final String username,
                                              @Param(mode = Parameter.Mode.MASKED) final String password) {
        return evaluate("Open the browser and navigate to " + url, () -> {
            BrowserManager.open(PageManager.DEFAULT_ALIAS, username, password);
            final Page page = PageManager.page();
            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            Log.info("Opened {} on {}", url, page.title());
            return page;
        });
    }

    /**
     * Opens the browser and navigates to the url, without HTTP basic authentication.
     *
     * @param url address to open
     * @return the page that became active
     */
    public static Page openBrowserAndNavigate(final String url) {
        return openBrowserAndNavigate(url, null, null);
    }

    /**
     * Opens the browser on the url of the active environment file, authenticated with the user
     * selected by {@code -Duser=...}.
     *
     * @return the page that became active
     */
    public static Page openBrowserAndNavigate() {
        final UserConfig user = UserConfig.current();
        return openBrowserAndNavigate(FrameworkConfig.baseUrl(), user.getUsername(), user.getPassword());
    }

    /**
     * Opens a second, independent browser and switches to it. Use it when a test case needs two
     * users side by side; come back to the first one with {@link #switchToBrowser(String)} and the
     * alias {@code default}.
     *
     * @param alias    name to register the new browser under
     * @param url      address to open
     * @param username user of the HTTP basic authentication dialog, {@code null} to skip it
     * @param password password of the HTTP basic authentication dialog
     * @return the page that became active
     */
    @Step("Open a new browser '{alias}' and navigate to {url}")
    public static Page openNewBrowserAndNavigate(final String alias, final String url,
                                                 final String username,
                                                 @Param(mode = Parameter.Mode.MASKED) final String password) {
        return evaluate("Open a new browser '" + alias + "' and navigate to " + url, () -> {
            BrowserManager.open(alias, username, password);
            final Page page = PageManager.page();
            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            return page;
        });
    }

    /**
     * Navigates the active page to another address.
     *
     * @param url address to open
     */
    @Step("Navigate to {url}")
    public static void navigateTo(final String url) {
        perform("Navigate to " + url, () -> {
            PageManager.page().navigate(url);
            PageManager.page().waitForLoadState(LoadState.DOMCONTENTLOADED);
        });
    }

    /**
     * Reloads the active page.
     */
    @Step("Reload the page")
    public static void reloadPage() {
        perform("Reload the page", () -> PageManager.page().reload());
    }

    /**
     * Goes back in the browser history of the active page.
     */
    @Step("Navigate back")
    public static void goBack() {
        perform("Navigate back", () -> PageManager.page().goBack());
    }

    /**
     * Closes every browser opened by the current test case. Called by the base test class after
     * each test method, and safe to call when nothing is open.
     */
    @Step("Close the browser")
    public static void closeBrowser() {
        if (!PageManager.sessionOrEmpty().isPresent()) {
            return;
        }
        perform("Close the browser", BrowserManager::closeAll);
    }

    // ------------------------------------------------------------------ interactions

    /**
     * Clicks an element, waiting for it to be actionable.
     *
     * @param locator the element to click
     */
    @Step("Click {locator.name}")
    public static void click(final NameLocator locator) {
        perform("Click " + locator.getName(), () -> locator.resolve().click());
    }

    /**
     * Double clicks an element.
     *
     * @param locator the element to double click
     */
    @Step("Double click {locator.name}")
    public static void doubleClick(final NameLocator locator) {
        perform("Double click " + locator.getName(), () -> locator.resolve().dblclick());
    }

    /**
     * Clicks an element even when another element covers it. Use it only when a normal
     * {@link #click(NameLocator)} cannot work, it bypasses the actionability checks.
     *
     * @param locator the element to click
     */
    @Step("Force click {locator.name}")
    public static void forceClick(final NameLocator locator) {
        perform("Force click " + locator.getName(),
                () -> locator.resolve().click(new Locator.ClickOptions().setForce(true)));
    }

    /**
     * Clears an input and types a value into it.
     *
     * @param locator the input to fill
     * @param value   the text to type
     */
    @Step("Type '{value}' into {locator.name}")
    public static void type(final NameLocator locator, final String value) {
        perform("Type '" + value + "' into " + locator.getName(), () -> locator.resolve().fill(value));
    }

    /**
     * Types a secret into an input. The value is never written to the log nor to the report.
     *
     * @param locator the input to fill
     * @param secret  the value to type, typically a password
     */
    @Step("Type the secret value into {locator.name}")
    public static void typeSecret(final NameLocator locator,
                                  @Param(mode = Parameter.Mode.MASKED) final String secret) {
        perform("Type the secret value into " + locator.getName(), () -> locator.resolve().fill(secret));
    }

    /**
     * Empties an input.
     *
     * @param locator the input to clear
     */
    @Step("Clear {locator.name}")
    public static void clear(final NameLocator locator) {
        perform("Clear " + locator.getName(), () -> locator.resolve().clear());
    }

    /**
     * Sends a keyboard key to an element, i.e. {@code Enter}, {@code Tab} or {@code Control+A}.
     *
     * @param locator the element that receives the key
     * @param key     the key to press, using the Playwright syntax
     */
    @Step("Press '{key}' on {locator.name}")
    public static void pressKey(final NameLocator locator, final String key) {
        perform("Press '" + key + "' on " + locator.getName(), () -> locator.resolve().press(key));
    }

    /**
     * Sends a keyboard key to the active page.
     *
     * @param key the key to press, using the Playwright syntax
     */
    @Step("Press '{key}'")
    public static void pressKey(final String key) {
        perform("Press '" + key + "'", () -> PageManager.page().keyboard().press(key));
    }

    /**
     * Ticks a checkbox or selects a radio button; does nothing when it is already ticked.
     *
     * @param locator the checkbox or the radio button
     */
    @Step("Check {locator.name}")
    public static void check(final NameLocator locator) {
        perform("Check " + locator.getName(), () -> locator.resolve().check());
    }

    /**
     * Unticks a checkbox; does nothing when it is already unticked.
     *
     * @param locator the checkbox
     */
    @Step("Uncheck {locator.name}")
    public static void uncheck(final NameLocator locator) {
        perform("Uncheck " + locator.getName(), () -> locator.resolve().uncheck());
    }

    /**
     * Selects an option of a {@code <select>} by its value attribute.
     *
     * @param locator the drop down list
     * @param value   the value attribute of the option
     */
    @Step("Select the value '{value}' in {locator.name}")
    public static void selectOption(final NameLocator locator, final String value) {
        perform("Select the value '" + value + "' in " + locator.getName(),
                () -> locator.resolve().selectOption(value));
    }

    /**
     * Selects an option of a {@code <select>} by the text the user sees.
     *
     * @param locator the drop down list
     * @param label   the visible text of the option
     */
    @Step("Select '{label}' in {locator.name}")
    public static void selectOptionByLabel(final NameLocator locator, final String label) {
        perform("Select '" + label + "' in " + locator.getName(),
                () -> locator.resolve().selectOption(new com.microsoft.playwright.options.SelectOption()
                        .setLabel(label)));
    }

    /**
     * Moves the mouse over an element.
     *
     * @param locator the element to hover
     */
    @Step("Hover {locator.name}")
    public static void hover(final NameLocator locator) {
        perform("Hover " + locator.getName(), () -> locator.resolve().hover());
    }

    /**
     * Scrolls the page until an element is in the viewport.
     *
     * @param locator the element to bring into view
     */
    @Step("Scroll to {locator.name}")
    public static void scrollIntoView(final NameLocator locator) {
        perform("Scroll to " + locator.getName(), () -> locator.resolve().scrollIntoViewIfNeeded());
    }

    /**
     * Attaches files to an {@code <input type="file">}.
     *
     * @param locator the file input
     * @param files   the files to attach
     */
    @Step("Upload a file into {locator.name}")
    public static void uploadFile(final NameLocator locator, final Path... files) {
        perform("Upload a file into " + locator.getName(), () -> locator.resolve().setInputFiles(files));
    }

    // ------------------------------------------------------------------ reading the page

    /**
     * Reads the visible text of an element.
     *
     * @param locator the element to read
     * @return the inner text, trimmed
     */
    @Step("Read the text of {locator.name}")
    public static String getText(final NameLocator locator) {
        return evaluate("Read the text of " + locator.getName(), () -> {
            final String text = locator.resolve().innerText().trim();
            Log.info("{} contains '{}'", locator.getName(), text);
            return text;
        });
    }

    /**
     * Reads the current value of an input.
     *
     * @param locator the input to read
     * @return the value of the field
     */
    @Step("Read the value of {locator.name}")
    public static String getValue(final NameLocator locator) {
        return evaluate("Read the value of " + locator.getName(), () -> locator.resolve().inputValue());
    }

    /**
     * Reads an attribute of an element.
     *
     * @param locator   the element to read
     * @param attribute the attribute name, i.e. {@code href}
     * @return the attribute value, or {@code null} when the attribute is absent
     */
    @Step("Read the attribute '{attribute}' of {locator.name}")
    public static String getAttribute(final NameLocator locator, final String attribute) {
        return evaluate("Read the attribute '" + attribute + "' of " + locator.getName(),
                () -> locator.resolve().getAttribute(attribute));
    }

    /**
     * Counts how many elements match a locator.
     *
     * @param locator the elements to count
     * @return the number of matching elements, {@code 0} when there is none
     */
    @Step("Count {locator.name}")
    public static int getCount(final NameLocator locator) {
        return evaluate("Count " + locator.getName(), () -> locator.resolve().count());
    }

    /**
     * Tells whether an element is visible right now, without waiting for it.
     *
     * @param locator the element to inspect
     * @return {@code true} when the element is present and visible
     */
    @Step("Is {locator.name} visible?")
    public static boolean isVisible(final NameLocator locator) {
        return evaluate("Is " + locator.getName() + " visible?", () -> {
            final boolean visible = locator.resolve().isVisible();
            Log.info("{} is {}", locator.getName(), visible ? "visible" : "not visible");
            return visible;
        });
    }

    /**
     * Tells whether an element is enabled.
     *
     * @param locator the element to inspect
     * @return {@code true} when the element is enabled
     */
    @Step("Is {locator.name} enabled?")
    public static boolean isEnabled(final NameLocator locator) {
        return evaluate("Is " + locator.getName() + " enabled?", () -> locator.resolve().isEnabled());
    }

    /**
     * Tells whether a checkbox or a radio button is ticked.
     *
     * @param locator the checkbox or the radio button
     * @return {@code true} when it is ticked
     */
    @Step("Is {locator.name} checked?")
    public static boolean isChecked(final NameLocator locator) {
        return evaluate("Is " + locator.getName() + " checked?", () -> locator.resolve().isChecked());
    }

    /**
     * @return the title of the active page
     */
    @Step("Read the page title")
    public static String getPageTitle() {
        return evaluate("Read the page title", () -> PageManager.page().title());
    }

    /**
     * @return the url of the active page
     */
    @Step("Read the current url")
    public static String getCurrentUrl() {
        return evaluate("Read the current url", () -> PageManager.page().url());
    }

    // ------------------------------------------------------------------ waits

    /**
     * Waits for an element to become visible, using the default timeout of the environment file.
     *
     * @param locator the element to wait for
     */
    @Step("Wait until {locator.name} is visible")
    public static void waitForVisible(final NameLocator locator) {
        waitForVisible(locator, FrameworkConfig.defaultTimeoutMs() / 1000);
    }

    /**
     * Waits for an element to become visible.
     *
     * @param locator        the element to wait for
     * @param timeoutSeconds how long to wait before failing
     */
    @Step("Wait up to {timeoutSeconds}s until {locator.name} is visible")
    public static void waitForVisible(final NameLocator locator, final int timeoutSeconds) {
        perform("Wait up to " + timeoutSeconds + "s until " + locator.getName() + " is visible",
                () -> locator.resolve().waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeoutSeconds * 1000.0)));
    }

    /**
     * Waits for an element to disappear.
     *
     * @param locator        the element that must vanish
     * @param timeoutSeconds how long to wait before failing
     */
    @Step("Wait up to {timeoutSeconds}s until {locator.name} is hidden")
    public static void waitForHidden(final NameLocator locator, final int timeoutSeconds) {
        perform("Wait up to " + timeoutSeconds + "s until " + locator.getName() + " is hidden",
                () -> locator.resolve().waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(timeoutSeconds * 1000.0)));
    }

    /**
     * Waits until the active page has finished loading and the network is quiet.
     */
    @Step("Wait until the page is loaded")
    public static void waitForPageLoad() {
        perform("Wait until the page is loaded",
                () -> PageManager.page().waitForLoadState(LoadState.NETWORKIDLE));
    }

    /**
     * Checks whether an element becomes visible within a delay, without failing when it does not.
     * Useful for optional elements such as a cookie banner.
     *
     * @param locator        the element to look for
     * @param timeoutSeconds how long to wait
     * @return {@code true} when the element showed up in time
     */
    @Step("Is {locator.name} displayed within {timeoutSeconds}s?")
    public static boolean isDisplayedWithin(final NameLocator locator, final int timeoutSeconds) {
        try {
            locator.resolve().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutSeconds * 1000.0));
            return true;
        } catch (RuntimeException e) {
            Log.warn("{} did not become visible within {}s", locator.getName(), timeoutSeconds);
            return false;
        }
    }

    // ------------------------------------------------------------------ screenshots

    /**
     * Takes a screenshot of the visible part of the active page, stores it under
     * {@code screenshots/<suite name>/<testcase ID>} and attaches it to the report.
     *
     * @param description short text describing the picture, it becomes part of the file name
     * @return the path of the image, or {@code null} when no browser is open
     */
    @Step("Take a screenshot: {description}")
    public static Path takeScreenshot(final String description) {
        return ScreenshotManager.capture(description, false).orElse(null);
    }

    /**
     * Takes a screenshot of the whole scrollable page, stores it under
     * {@code screenshots/<suite name>/<testcase ID>} and attaches it to the report.
     *
     * @param description short text describing the picture, it becomes part of the file name
     * @return the path of the image, or {@code null} when no browser is open
     */
    @Step("Take a full page screenshot: {description}")
    public static Path takeFullPageScreenshot(final String description) {
        return ScreenshotManager.capture(description, true).orElse(null);
    }

    // ------------------------------------------------------------------ tabs and browsers

    /**
     * Switches to another tab of the active browser. Every following keyword runs on that tab.
     *
     * @param index zero based index of the tab, in the order the tabs were opened
     * @return the page that became active
     */
    @Step("Switch to the tab number {index}")
    public static Page switchToTab(final int index) {
        return evaluate("Switch to the tab number " + index, () -> {
            final List<Page> pages = PageManager.pages();
            if (index < 0 || index >= pages.size()) {
                throw new FrameworkException("There is no tab number " + index
                        + ", the browser has " + pages.size() + " open tab(s)");
            }
            final Page page = PageManager.setActivePage(pages.get(index));
            Log.info("Now working on the tab '{}' ({})", page.title(), page.url());
            return page;
        });
    }

    /**
     * Switches to the tab whose title contains the given text.
     *
     * @param titlePart part of the title of the wanted tab
     * @return the page that became active
     */
    @Step("Switch to the tab with the title '{titlePart}'")
    public static Page switchToTabByTitle(final String titlePart) {
        return evaluate("Switch to the tab with the title '" + titlePart + "'",
                () -> switchTo(page -> page.title().contains(titlePart), "title", titlePart));
    }

    /**
     * Switches to the tab whose url contains the given text.
     *
     * @param urlPart part of the url of the wanted tab
     * @return the page that became active
     */
    @Step("Switch to the tab with the url '{urlPart}'")
    public static Page switchToTabByUrl(final String urlPart) {
        return evaluate("Switch to the tab with the url '" + urlPart + "'",
                () -> switchTo(page -> page.url().contains(urlPart), "url", urlPart));
    }

    /**
     * Switches to the most recently opened tab.
     *
     * @return the page that became active
     */
    @Step("Switch to the last opened tab")
    public static Page switchToLastTab() {
        return evaluate("Switch to the last opened tab", () -> {
            final List<Page> pages = PageManager.pages();
            return PageManager.setActivePage(pages.get(pages.size() - 1));
        });
    }

    /**
     * Clicks an element that opens a new tab and switches to that tab, waiting for it to appear.
     *
     * @param locator the link or the button that opens the new tab
     * @return the page that became active
     */
    @Step("Click {locator.name} and switch to the tab it opens")
    public static Page clickAndSwitchToNewTab(final NameLocator locator) {
        return evaluate("Click " + locator.getName() + " and switch to the tab it opens", () -> {
            final Page opened = PageManager.context().waitForPage(() -> locator.resolve().click());
            opened.waitForLoadState(LoadState.DOMCONTENTLOADED);
            final Page page = PageManager.setActivePage(opened);
            Log.info("A new tab was opened: '{}' ({})", page.title(), page.url());
            return page;
        });
    }

    /**
     * Opens a new tab in the active browser and navigates it to a url.
     *
     * @param url address to open in the new tab
     * @return the page that became active
     */
    @Step("Open a new tab on {url}")
    public static Page openNewTabAndNavigate(final String url) {
        return evaluate("Open a new tab on " + url, () -> {
            final Page opened = PageManager.context().newPage();
            opened.navigate(url);
            opened.waitForLoadState(LoadState.DOMCONTENTLOADED);
            return PageManager.setActivePage(opened);
        });
    }

    /**
     * Closes the tab in use and switches back to the first tab that is still open.
     *
     * @return the page that became active
     */
    @Step("Close the current tab")
    public static Page closeCurrentTab() {
        return evaluate("Close the current tab", () -> {
            final Page current = PageManager.page();
            current.close();
            final List<Page> remaining = PageManager.pages();
            if (remaining.isEmpty()) {
                throw new FrameworkException("The last tab of the browser has been closed");
            }
            return PageManager.setActivePage(remaining.get(0));
        });
    }

    /**
     * Switches to another browser previously opened with
     * {@link #openNewBrowserAndNavigate(String, String, String, String)}.
     *
     * @param alias alias of the wanted browser, {@code default} for the first one
     * @return the page that became active
     */
    @Step("Switch to the browser '{alias}'")
    public static Page switchToBrowser(final String alias) {
        return evaluate("Switch to the browser '" + alias + "'", () -> PageManager.switchToBrowser(alias));
    }

    // ------------------------------------------------------------------ downloads and dialogs

    /**
     * Clicks an element that starts a download and waits for the file to be saved under
     * {@code downloads/<suite name>/<testcase ID>}.
     *
     * @param locator the link or the button that starts the download
     * @return the path of the downloaded file
     */
    @Step("Download the file behind {locator.name}")
    public static Path downloadFileByClicking(final NameLocator locator) {
        return evaluate("Download the file behind " + locator.getName(), () -> {
            final Download download = PageManager.page().waitForDownload(() -> locator.resolve().click());
            return DownloadManager.save(download);
        });
    }

    /**
     * Lists the files the running test case has downloaded so far.
     *
     * @return the files present in the download folder of the test case
     */
    @Step("List the downloaded files")
    public static List<Path> getDownloadedFiles() {
        return DownloadManager.downloadedFiles();
    }

    /**
     * Prepares the answer to the next javascript dialog ({@code alert}, {@code confirm} or
     * {@code prompt}). Call it before the action that raises the dialog.
     *
     * @param accept     {@code true} to accept the dialog, {@code false} to dismiss it
     * @param promptText text to type in a {@code prompt} dialog, {@code null} for the other kinds
     */
    @Step("Answer the next dialog (accept={accept})")
    public static void answerNextDialog(final boolean accept, final String promptText) {
        perform("Answer the next dialog (accept=" + accept + ")", () ->
                PageManager.page().onceDialog(dialog -> handleDialog(dialog, accept, promptText)));
    }

    private static void handleDialog(final Dialog dialog, final boolean accept, final String promptText) {
        Log.info("Dialog '{}' with the message '{}'", dialog.type(), dialog.message());
        if (accept) {
            dialog.accept(promptText == null ? "" : promptText);
        } else {
            dialog.dismiss();
        }
    }

    // ------------------------------------------------------------------ internals

    private static Page switchTo(final java.util.function.Predicate<Page> matcher,
                                 final String criterion, final String expected) {
        for (final Page candidate : PageManager.pages()) {
            if (matcher.test(candidate)) {
                Log.info("Switched to the tab whose {} matches '{}'", criterion, expected);
                return PageManager.setActivePage(candidate);
            }
        }
        throw new FrameworkException("No open tab has a " + criterion + " containing '" + expected + "'");
    }

    /**
     * Runs a keyword that returns nothing, logging it and reporting a failure the framework way.
     *
     * @param description the keyword description, reused in the log and in the error message
     * @param action      what the keyword actually does
     */
    private static void perform(final String description, final Runnable action) {
        evaluate(description, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Runs a keyword that returns a value, logging it and reporting a failure the framework way.
     *
     * @param description the keyword description, reused in the log and in the error message
     * @param action      what the keyword actually does
     * @param <T>         type of the value the keyword produces
     * @return the value produced by the keyword
     */
    private static <T> T evaluate(final String description, final Supplier<T> action) {
        Log.info(description);
        try {
            return action.get();
        } catch (RuntimeException error) {
            // the failure is photographed against the exception that really propagates, so the
            // enclosing keywords and the TestNG listener recognise it and do not photograph it again
            final FrameworkException failure = error instanceof FrameworkException
                    ? (FrameworkException) error
                    : new FrameworkException(description + " -> FAILED: " + error.getMessage(), error);
            Log.fail("{} -> FAILED: {}", description, error.getMessage());
            Allure.addAttachment("Error - " + description, "text/plain", stackTraceOf(error), ".txt");
            ScreenshotManager.captureFailure(failure);
            throw failure;
        }
    }

    private static String stackTraceOf(final Throwable error) {
        final StringWriter buffer = new StringWriter();
        error.printStackTrace(new PrintWriter(buffer));
        return buffer.toString();
    }
}
