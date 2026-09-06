package tests.demo;

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
import pages.demo.DemoHomePage;
import pages.demo.DemoLoginPage;
import tests.BaseTest;

/**
 * URS 3.2.1.2 - the application refuses unknown credentials and explains why.
 */
@TestCaseInfo(urs = "3.2.1.2", id = "TC002", description = "Login with invalid credentials",
        suite = "SP0308_3.2.1.1_Demo application end to end")
@Feature("Authentication")
@Story("Login")
@Tag("negative")
@Tag("login")
public class TC002_login_with_invalid_credentials extends BaseTest {

    /**
     * Submits unknown credentials and checks that the error message is shown and that the user
     * stays on the login page.
     */
    @Test(testName = "3.2.1.2_TC002_Login with invalid credentials",
            description = "An unknown user is refused and an explicit message is displayed")
    @Description("Submits the credentials of the InvalidUser file and verifies that the login form "
            + "shows the error message and that the home page is never reached.")
    @Severity(SeverityLevel.CRITICAL)
    public void e2eTest() {
        PlaywrightActions.openBrowserAndNavigate(url());

        final DemoLoginPage loginPage = new DemoLoginPage();
        loginPage.waitUntilDisplayed();

        final UserConfig unknownUser = UserConfig.of("InvalidUser");
        loginPage.login(unknownUser.getUsername(), unknownUser.getPassword());

        PlaywrightActions.takeScreenshot("Login refused");
        CustomAssertions.assertVisible(DemoLoginPage.ERROR_MESSAGE);
        CustomAssertions.assertText(DemoLoginPage.ERROR_MESSAGE, "Invalid user name or password");
        CustomAssertions.assertNotVisible(DemoHomePage.WELCOME_MESSAGE);
    }
}
