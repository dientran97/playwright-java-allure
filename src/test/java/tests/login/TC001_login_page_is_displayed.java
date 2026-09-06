package tests.login;

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
import pages.LoginPage;
import tests.BaseTest;

/**
 * URS 3.2.1.1 - the login page of the application under test exposes the expected fields.
 */
@TestCaseInfo(urs = "3.2.1.1", id = "TC001", description = "The login page is displayed",
        suite = "SP0308_3.2.1.1_Login functionality")
@Feature("Authentication")
@Story("Login")
@Tag("smoke")
public class TC001_login_page_is_displayed extends BaseTest {

    /**
     * Opens the application and verifies that the login form is complete.
     */
    @Test(testName = "3.2.1.1_TC001_The login page is displayed",
            description = "The login form shows the user name, the password and the login button")
    @Description("Opens the url of the active environment and verifies the three fields of the "
            + "login form, so a broken deployment is detected before any deeper test case runs.")
    @Severity(SeverityLevel.BLOCKER)
    public void e2eTest() {
        PlaywrightActions.openBrowserAndNavigate(url(), user().getUsername(), user().getPassword());

        final LoginPage loginPage = new LoginPage();
        loginPage.waitUntilDisplayed();
        PlaywrightActions.takeFullPageScreenshot("Login page");

        softly().assertVisible(LoginPage.USERNAME_FIELD);
        softly().assertVisible(LoginPage.PASSWORD_FIELD);
        softly().assertVisible(LoginPage.LOGIN_BUTTON);
        softly().assertAll();
    }
}
