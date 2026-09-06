package tests.login;

import framework.actions.PlaywrightActions;
import framework.annotations.Tag;
import framework.annotations.TestCaseInfo;
import framework.assertions.CustomAssertions;
import framework.config.UserConfig;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import pages.LoginPage;
import tests.BaseTest;

/**
 * URS 3.2.1.2 - the application under test refuses unknown credentials.
 */
@TestCaseInfo(urs = "3.2.1.2", id = "TC002", description = "Login with invalid credentials",
        suite = "SP0308_3.2.1.1_Login functionality")
@Feature("Authentication")
@Story("Login")
@Tag("negative")
public class TC002_login_with_invalid_credentials extends BaseTest {

    /**
     * Submits unknown credentials and verifies that the application does not log the user in.
     */
    @Test(testName = "3.2.1.2_TC002_Login with invalid credentials",
            description = "An unknown user is not logged in")
    @Description("Submits the credentials of the InvalidUser file and verifies that the toolbar "
            + "never shows a logged in user.")
    @Severity(SeverityLevel.CRITICAL)
    public void e2eTest() {
        PlaywrightActions.openBrowserAndNavigate(url());

        final LoginPage loginPage = new LoginPage();
        loginPage.waitUntilDisplayed();
        loginPage.loginAs(UserConfig.of("InvalidUser"));

        PlaywrightActions.takeScreenshot("Login refused");
        CustomAssertions.assertFalse(loginPage.isUserLoggedIn(), "the unknown user is logged in");
    }
}
