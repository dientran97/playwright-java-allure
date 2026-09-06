package pages;

import framework.actions.PlaywrightActions;
import framework.assertions.CustomAssertions;
import framework.config.FrameworkConfig;
import framework.locator.NameLocator;
import io.qameta.allure.Step;

import java.nio.file.Path;

/**
 * Common ground of every page object.
 *
 * <p>A page object owns two things and nothing else: the {@link NameLocator}s of its elements and
 * the high level keywords a test case reads like a sentence ("login as", "download the report").
 * The keywords are written with {@code PlaywrightActions}, never with raw Playwright calls, so
 * every interaction ends up in the Allure report.</p>
 *
 * <p>No page object holds a Playwright {@code Page}: the active page is resolved by the framework
 * when the action runs, which is what keeps a page object valid after a switch to another tab or
 * another browser.</p>
 */
public abstract class BasePage {

    /**
     * The element that proves the page is the one displayed, i.e. its title or its main heading.
     *
     * @return the locator used by {@link #isDisplayed()} and {@link #waitUntilDisplayed()}
     */
    protected abstract NameLocator pageIdentifier();

    /**
     * @return the name of the page as shown in the report, by default the simple class name
     */
    protected String pageName() {
        return getClass().getSimpleName();
    }

    /**
     * Tells whether the page is displayed, without failing when it is not.
     *
     * @return {@code true} when the identifying element showed up within the default timeout
     */
    @Step("Is the page displayed?")
    public boolean isDisplayed() {
        return PlaywrightActions.isDisplayedWithin(pageIdentifier(),
                FrameworkConfig.defaultTimeoutMs() / 1000);
    }

    /**
     * Waits for the page to be displayed and fails the test case when it is not.
     *
     * @return this page object, so calls can be chained
     */
    @Step("Wait until the page is displayed")
    public BasePage waitUntilDisplayed() {
        CustomAssertions.assertVisible(pageIdentifier());
        return this;
    }

    /**
     * Takes a screenshot of the visible part of the page.
     *
     * @param description short text describing the picture
     * @return the path of the image, or {@code null} when no browser is open
     */
    public Path takeScreenshot(final String description) {
        return PlaywrightActions.takeScreenshot(description);
    }

    /**
     * Takes a screenshot of the whole page.
     *
     * @param description short text describing the picture
     * @return the path of the image, or {@code null} when no browser is open
     */
    public Path takeFullPageScreenshot(final String description) {
        return PlaywrightActions.takeFullPageScreenshot(description);
    }
}
