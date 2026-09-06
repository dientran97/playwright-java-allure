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
import pages.demo.DemoHomePage;
import pages.demo.DemoLoginPage;
import tests.BaseTest;

/**
 * URS 3.2.1.1 - a registered user can log in with valid credentials.
 */
@TestCaseInfo(urs = "3.2.1.1", id = "TC001", description = "Login with valid credentials",
        suite = "SP0308_3.2.1.1_Demo application end to end")
@Feature("Authentication")
@Story("Login")
@Tag("smoke")
@Tag("login")
public class TC001_login_with_valid_credentials extends BaseTest {

    /**
     * Logs in with the user of the run and checks that the home page greets that user.
     */
    @Test(testName = "3.2.1.1_TC001_Login with valid credentials",
            description = "A registered user reaches the home page after a successful login")
    @Description("Opens the application, logs in with the credentials of the selected user file "
            + "and verifies that the home page greets the user by name.")
    @Severity(SeverityLevel.BLOCKER)
    public void e2eTest() {
        PlaywrightActions.openBrowserAndNavigate(url(), user().getUsername(), user().getPassword());

        final DemoLoginPage loginPage = new DemoLoginPage();
        loginPage.waitUntilDisplayed();
        PlaywrightActions.takeScreenshot("Login page");

        final DemoHomePage homePage = loginPage.loginAs(user());
        homePage.waitUntilDisplayed();
        PlaywrightActions.takeFullPageScreenshot("Home page after login");

        CustomAssertions.assertTextContains(DemoHomePage.WELCOME_MESSAGE, user().getName());
        softly().assertVisible(DemoHomePage.LOGOUT_BUTTON);
        softly().assertText(DemoHomePage.PAGE_TITLE, "My account");
        softly().assertAll();
    }
}
