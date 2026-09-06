package pages.demo;

import framework.actions.PlaywrightActions;
import framework.locator.NameLocator;
import io.qameta.allure.Step;
import pages.BasePage;

import java.nio.file.Path;

/**
 * Home page of the bundled demo application: it carries a download link, a link that opens a second
 * tab, a checkbox and a drop down list, which is enough to exercise every part of the framework.
 */
public class DemoHomePage extends BasePage {

    /** Greeting shown in the toolbar once logged in. */
    public static final NameLocator WELCOME_MESSAGE = new NameLocator("Welcome message", "#welcome");
    /** Heading of the page body. */
    public static final NameLocator PAGE_TITLE = new NameLocator("Page title", "#pageTitle");
    /** Link that downloads the sales report. */
    public static final NameLocator DOWNLOAD_REPORT_LINK =
            new NameLocator("Download the sales report link", "#downloadReport");
    /** Link that opens the documentation in a second tab. */
    public static final NameLocator OPEN_DOCS_LINK =
            new NameLocator("Open the documentation link", "#openDocs");
    /** Newsletter checkbox. */
    public static final NameLocator NEWSLETTER_CHECKBOX = new NameLocator("Newsletter checkbox", "#newsletter");
    /** Country drop down list. */
    public static final NameLocator COUNTRY_SELECT = new NameLocator("Country list", "#country");
    /** Line echoing the country currently selected. */
    public static final NameLocator SELECTED_COUNTRY = new NameLocator("Selected country", "#selectedCountry");
    /** Button that logs the user out. */
    public static final NameLocator LOGOUT_BUTTON = new NameLocator("Logout button", "#logout");

    @Override
    protected NameLocator pageIdentifier() {
        return WELCOME_MESSAGE;
    }

    /**
     * Reads the greeting of the toolbar.
     *
     * @return the text of the welcome message
     */
    @Step("Read the welcome message")
    public String getWelcomeMessage() {
        return PlaywrightActions.getText(WELCOME_MESSAGE);
    }

    /**
     * Downloads the sales report into the download folder of the running test case.
     *
     * @return the path of the downloaded file
     */
    @Step("Download the sales report")
    public Path downloadSalesReport() {
        return PlaywrightActions.downloadFileByClicking(DOWNLOAD_REPORT_LINK);
    }

    /**
     * Opens the documentation link and switches the framework to the tab it opens. Every keyword
     * called afterwards runs on that new tab.
     *
     * @return the documentation page object
     */
    @Step("Open the documentation in a new tab")
    public DemoDocsPage openDocumentationInNewTab() {
        PlaywrightActions.clickAndSwitchToNewTab(OPEN_DOCS_LINK);
        return new DemoDocsPage();
    }

    /**
     * Ticks the newsletter checkbox.
     *
     * @return this page object, so calls can be chained
     */
    @Step("Subscribe to the newsletter")
    public DemoHomePage subscribeToNewsletter() {
        PlaywrightActions.check(NEWSLETTER_CHECKBOX);
        return this;
    }

    /**
     * Selects a country in the drop down list.
     *
     * @param countryCode value of the option, i.e. {@code sg}
     * @return this page object, so calls can be chained
     */
    @Step("Select the country '{countryCode}'")
    public DemoHomePage selectCountry(final String countryCode) {
        PlaywrightActions.selectOption(COUNTRY_SELECT, countryCode);
        return this;
    }

    /**
     * Reads the country the page currently echoes.
     *
     * @return the text of the "Selected country" line
     */
    @Step("Read the selected country")
    public String getSelectedCountry() {
        return PlaywrightActions.getText(SELECTED_COUNTRY);
    }

    /**
     * Logs the user out and comes back to the login page.
     *
     * @return the login page object
     */
    @Step("Log out")
    public DemoLoginPage logout() {
        PlaywrightActions.click(LOGOUT_BUTTON);
        return new DemoLoginPage();
    }
}
