package framework.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import framework.config.FrameworkConfig;
import framework.logging.Log;
import framework.report.DownloadManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of the browsers and tabs opened by the current thread and, above all, the single place
 * that answers the question "which page should the next action run on?".
 *
 * <p>Every action of {@code PlaywrightActions} and every {@code NameLocator} goes through
 * {@link #page()}, so switching tab or browser is enough to redirect the whole framework: no page
 * object and no keyword has to be told about it.</p>
 *
 * <p>The state is held in {@link ThreadLocal} fields, which keeps parallel TestNG execution safe.</p>
 */
public final class PageManager {

    /** Alias of the browser opened by {@code openBrowserAndNavigate}. */
    public static final String DEFAULT_ALIAS = "default";

    private static final ThreadLocal<Map<String, BrowserSession>> SESSIONS =
            ThreadLocal.withInitial(LinkedHashMap::new);
    private static final ThreadLocal<BrowserSession> ACTIVE = new ThreadLocal<>();

    private PageManager() {
    }

    /**
     * Registers a freshly launched browser and makes it the active one.
     *
     * @param session the session created by {@link BrowserManager}
     * @return the same session
     */
    public static BrowserSession register(final BrowserSession session) {
        SESSIONS.get().put(session.getAlias(), session);
        ACTIVE.set(session);
        session.getContext().onPage(page -> {
            session.addPage(page);
            prepare(page);
        });
        session.getContext().pages().forEach(session::addPage);
        return session;
    }

    /**
     * Applies the framework defaults to a page and wires the automatic download handling.
     *
     * @param page a page that has just been created
     * @return the same page
     */
    public static Page prepare(final Page page) {
        page.setDefaultTimeout(FrameworkConfig.defaultTimeoutMs());
        page.setDefaultNavigationTimeout(FrameworkConfig.defaultTimeoutMs());
        DownloadManager.attachTo(page);
        return page;
    }

    /**
     * @return the active browser session
     * @throws IllegalStateException when no browser has been opened yet
     */
    public static BrowserSession session() {
        final BrowserSession session = ACTIVE.get();
        if (session == null) {
            throw new IllegalStateException("No browser is open. A test case must call "
                    + "PlaywrightActions.openBrowserAndNavigate(...) before using any other keyword.");
        }
        return session;
    }

    /**
     * @return the active browser session, or an empty optional when no browser is open
     */
    public static Optional<BrowserSession> sessionOrEmpty() {
        return Optional.ofNullable(ACTIVE.get());
    }

    /**
     * @return the page (tab) every action currently runs on
     * @throws IllegalStateException when no browser has been opened yet
     */
    public static Page page() {
        final Page page = session().getActivePage();
        if (page == null) {
            throw new IllegalStateException("The active browser has no open page.");
        }
        return page;
    }

    /**
     * @return the context of the active browser
     */
    public static BrowserContext context() {
        return session().getContext();
    }

    /**
     * @return the active browser
     */
    public static Browser browser() {
        return session().getBrowser();
    }

    /**
     * @return {@code true} when a browser is open and usable, i.e. when a screenshot can be taken
     */
    public static boolean isBrowserOpen() {
        return sessionOrEmpty().map(BrowserSession::isAlive).orElse(false);
    }

    /**
     * Makes a page the target of the next actions and brings it to the front.
     *
     * @param page the page to activate
     * @return the activated page
     */
    public static Page setActivePage(final Page page) {
        final BrowserSession session = session();
        session.setActivePage(page);
        try {
            page.bringToFront();
        } catch (RuntimeException ignored) {
            // headless browsers do not always support it, and it is not essential
        }
        return page;
    }

    /**
     * @return every open tab of the active browser, in the order they were opened
     */
    public static List<Page> pages() {
        return session().getPages();
    }

    /**
     * @return every browser session opened by this thread
     */
    public static List<BrowserSession> sessions() {
        return new ArrayList<>(SESSIONS.get().values());
    }

    /**
     * @return the aliases of every browser opened by this thread
     */
    public static List<String> aliases() {
        return new ArrayList<>(SESSIONS.get().keySet());
    }

    /**
     * Switches the framework to another browser previously opened under the given alias.
     *
     * @param alias alias used when the browser was opened
     * @return the page that became active
     * @throws IllegalArgumentException when no browser is registered under that alias
     */
    public static Page switchToBrowser(final String alias) {
        final BrowserSession session = SESSIONS.get().get(alias);
        if (session == null) {
            throw new IllegalArgumentException("No browser registered under the alias '" + alias
                    + "'. Known aliases: " + aliases());
        }
        ACTIVE.set(session);
        Log.info("Switched to browser '{}'", alias);
        return setActivePage(session.getActivePage());
    }

    /**
     * Closes one browser and, when it was the active one, falls back on any other open browser.
     *
     * @param alias alias of the browser to close
     */
    public static void closeSession(final String alias) {
        final BrowserSession session = SESSIONS.get().remove(alias);
        if (session == null) {
            return;
        }
        session.close();
        if (ACTIVE.get() == session) {
            ACTIVE.set(SESSIONS.get().values().stream().findFirst().orElse(null));
        }
    }

    /**
     * Closes every browser opened by this thread and clears the registry. Called by
     * {@code BaseTest} after each test method.
     */
    public static void closeAll() {
        new ArrayList<>(SESSIONS.get().values()).forEach(BrowserSession::close);
        SESSIONS.get().clear();
        SESSIONS.remove();
        ACTIVE.remove();
    }
}
