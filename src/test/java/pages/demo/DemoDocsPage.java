package pages.demo;

import framework.actions.PlaywrightActions;
import framework.locator.NameLocator;
import io.qameta.allure.Step;
import pages.BasePage;

/**
 * Documentation page of the bundled demo application, opened in a second tab by
 * {@link DemoHomePage#openDocumentationInNewTab()}.
 */
public class DemoDocsPage extends BasePage {

    /** Heading of the documentation page. */
    public static final NameLocator DOCS_TITLE = new NameLocator("Documentation title", "#docsTitle");
    /** Body of the documentation page. */
    public static final NameLocator DOCS_BODY = new NameLocator("Documentation body", "#docsBody");

    @Override
    protected NameLocator pageIdentifier() {
        return DOCS_TITLE;
    }

    /**
     * Reads the heading of the documentation page.
     *
     * @return the text of the heading
     */
    @Step("Read the documentation title")
    public String getTitleText() {
        return PlaywrightActions.getText(DOCS_TITLE);
    }
}
