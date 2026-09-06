package framework.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One running browser: the Playwright instance that drives it, the browser itself, its context and
 * the page (tab) the framework is currently acting on.
 *
 * <p>Several sessions can live side by side, each under its own alias, which is how a test case can
 * drive two browsers at the same time (for instance a maker and a checker).</p>
 */
public final class BrowserSession {

    private final String alias;
    private final String browserType;
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final List<Page> pages = new ArrayList<>();
    private volatile Page activePage;

    BrowserSession(final String alias, final String browserType, final Playwright playwright,
                   final Browser browser, final BrowserContext context) {
        this.alias = alias;
        this.browserType = browserType;
        this.playwright = playwright;
        this.browser = browser;
        this.context = context;
    }

    /**
     * @return the alias under which the session is registered, i.e. {@code default}
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @return the browser family that was started, i.e. {@code chrome}
     */
    public String getBrowserType() {
        return browserType;
    }

    /**
     * @return the browser version reported by Playwright, i.e. {@code 131.0.6778.33}
     */
    public String getBrowserVersion() {
        try {
            return browser.version();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    /**
     * @return the Playwright instance owning this session
     */
    public Playwright getPlaywright() {
        return playwright;
    }

    /**
     * @return the Playwright browser
     */
    public Browser getBrowser() {
        return browser;
    }

    /**
     * @return the browser context, i.e. the isolated profile the pages belong to
     */
    public BrowserContext getContext() {
        return context;
    }

    /**
     * @return the tab the framework is currently acting on
     */
    public Page getActivePage() {
        return activePage;
    }

    /**
     * @param page the tab that becomes the target of the next actions
     */
    public void setActivePage(final Page page) {
        this.activePage = page;
        if (page != null && !pages.contains(page)) {
            pages.add(page);
        }
    }

    /**
     * Registers a page that was opened by the application itself (a popup or a target=_blank link).
     *
     * @param page the newly opened page
     */
    public void addPage(final Page page) {
        if (page != null && !pages.contains(page)) {
            pages.add(page);
        }
    }

    /**
     * @return every tab known to this session, in the order they were opened, closed ones removed
     */
    public List<Page> getPages() {
        pages.removeIf(Page::isClosed);
        context.pages().forEach(this::addPage);
        pages.removeIf(Page::isClosed);
        return Collections.unmodifiableList(new ArrayList<>(pages));
    }

    /**
     * @return {@code true} when the browser is still connected and at least one tab is open
     */
    public boolean isAlive() {
        try {
            return browser.isConnected() && activePage != null && !activePage.isClosed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Closes the context, the browser and the Playwright instance, ignoring any error.
     */
    public void close() {
        closeQuietly(context::close);
        closeQuietly(browser::close);
        closeQuietly(playwright::close);
        pages.clear();
        activePage = null;
    }

    private static void closeQuietly(final Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ignored) {
            // a browser that already died must never fail the teardown
        }
    }

    @Override
    public String toString() {
        return "BrowserSession{" + alias + ", " + browserType + "}";
    }
}
