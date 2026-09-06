package tests.demo;

import framework.actions.PlaywrightActions;
import framework.annotations.Tag;
import framework.annotations.TestCaseInfo;
import framework.assertions.CustomAssertions;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import pages.demo.DemoDocsPage;
import pages.demo.DemoHomePage;
import pages.demo.DemoLoginPage;
import tests.BaseTest;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * URS 3.2.1.3 and 3.2.1.4 - a logged in user downloads the sales report and reads the documentation
 * that opens in a second tab.
 */
@TestCaseInfo(urs = "3.2.1.3_3.2.1.4", id = "TC003",
        description = "Download the report and read the documentation",
        suite = "SP0308_3.2.1.1_Demo application end to end")
@Feature("My account")
@Story("Reports")
@Tag("regression")
public class TC003_download_report_and_read_documentation extends BaseTest {

    /**
     * Downloads a file, checks it landed in the folder of the test case, then switches to the tab
     * the application opens and comes back to the first one.
     */
    @Test(testName = "3.2.1.3_3.2.1.4_TC003_Download the report and read the documentation",
            description = "The report is downloaded in the folder of the test case and the "
                    + "documentation opens in a second tab")
    @Description("Verifies the two features the framework has to support end to end: a download "
            + "stored under downloads/[suite]/[testcase id], and keywords that keep working after "
            + "the framework has switched to another tab.")
    @Severity(SeverityLevel.NORMAL)
    public void e2eTest() {
        PlaywrightActions.openBrowserAndNavigate(url(), user().getUsername(), user().getPassword());

        final DemoHomePage homePage = new DemoLoginPage().loginAs(user());
        homePage.waitUntilDisplayed();

        // --- the download lands in downloads/<suite name>/<testcase ID> ------------------
        final Path report = homePage.downloadSalesReport();
        CustomAssertions.assertTrue(Files.exists(report), "the sales report has been saved");
        CustomAssertions.assertEquals(report.getParent().getFileName().toString(),
                testCase().getTestCaseId(), "the folder the report was saved in");

        // --- from here on every keyword runs on the second tab --------------------------
        final DemoDocsPage documentation = homePage.openDocumentationInNewTab();
        documentation.waitUntilDisplayed();
        PlaywrightActions.takeScreenshot("Documentation tab");
        CustomAssertions.assertText(DemoDocsPage.DOCS_TITLE, "User documentation");
        CustomAssertions.assertUrlContains("docs.html");

        // --- and back on the first one --------------------------------------------------
        PlaywrightActions.switchToTab(0);
        CustomAssertions.assertVisible(DemoHomePage.WELCOME_MESSAGE);
        homePage.subscribeToNewsletter().selectCountry("sg");

        softly().assertChecked(DemoHomePage.NEWSLETTER_CHECKBOX);
        softly().assertTextContains(DemoHomePage.SELECTED_COUNTRY, "sg");
        softly().assertAll();
    }
}
