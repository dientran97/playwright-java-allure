package pages.demo;

import framework.actions.PlaywrightActions;
import framework.config.UserConfig;
import framework.locator.NameLocator;
import io.qameta.allure.Step;
import pages.BasePage;

/**
 * Login page of the demo application bundled in {@code src/test/resources/demo-app}.
 */
public class DemoLoginPage extends BasePage {

    /** User name field. */
    public static final NameLocator USERNAME_FIELD = new NameLocator("Username field", "#username");
    /** Password field. */
    public static final NameLocator PASSWORD_FIELD = new NameLocator("Password field", "#password");
    /** Button that submits the form. */
    public static final NameLocator LOGIN_BUTTON = new NameLocator("Login button", "#loginbtn");
    /** Message shown when the credentials are refused. */
    public static final NameLocator ERROR_MESSAGE = new NameLocator("Error message", "#error");

    @Override
    protected NameLocator pageIdentifier() {
        return LOGIN_BUTTON;
    }

    /**
     * Fills the form with a user name and a password and submits it.
     *
     * @param username the user name to type
     * @param password the password to type
     * @return the home page object, whether or not the login succeeded
     */
    @Step("Log in with the user name '{username}'")
    public DemoHomePage login(final String username, final String password) {
        PlaywrightActions.type(USERNAME_FIELD, username);
        PlaywrightActions.typeSecret(PASSWORD_FIELD, password);
        PlaywrightActions.click(LOGIN_BUTTON);
        return new DemoHomePage();
    }

    /**
     * Fills the form with the credentials of a user file and submits it.
     *
     * @param user the user to log in with
     * @return the home page object
     */
    @Step("Log in as the user '{user.userKey}'")
    public DemoHomePage loginAs(final UserConfig user) {
        return login(user.getUsername(), user.getPassword());
    }

    /**
     * Reads the message displayed when the credentials are refused.
     *
     * @return the text of the message
     */
    @Step("Read the login error message")
    public String getErrorMessage() {
        return PlaywrightActions.getText(ERROR_MESSAGE);
    }
}
