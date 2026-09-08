package pages;

import framework.actions.PlaywrightActions;
import framework.config.UserConfig;
import framework.locator.NameLocator;
import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.Step;

/**
 * Login page of the demo application used by the {@code Login} suite.
 *
 * <p>It shows the shape every page object of the project follows: the elements are declared once
 * as {@link NameLocator} constants and the keywords below are the only vocabulary a test case
 * uses.</p>
 */
public class LoginPage extends BasePage {

    /** User name field of the login form. */
    public static final NameLocator USERNAME_FIELD =
            new NameLocator("Username field", "//input[@formcontrolname='username']");
    /** Password field of the login form. */
    public static final NameLocator PASSWORD_FIELD =
            new NameLocator("Password field", "//input[@formcontrolname='password']");
    /** Button that submits the login form. */
    public static final NameLocator LOGIN_BUTTON =
            new NameLocator("Login button", "//button[.//span[normalize-space()='Login']]");
    /** Link that opens the registration form. */
    public static final NameLocator REGISTER_LINK =
            new NameLocator("Register link", "//button[.//span[normalize-space()='Register']]");
    /** Validation message shown under an invalid field. */
    public static final NameLocator ERROR_MESSAGE =
            new NameLocator("Error message", "//mat-error");
    /** Name of the logged in user, shown in the toolbar. */
    public static final NameLocator USER_MENU =
            new NameLocator("User menu", "//*[@aria-haspopup='menu']//span[contains(@class,'label')]");

    @Override
    protected NameLocator pageIdentifier() {
        return LOGIN_BUTTON;
    }

    /**
     * Types a user name in the login form.
     *
     * @param username the user name to type
     * @return this page object, so calls can be chained
     */
    @Step("Enter the user name '{username}'")
    public LoginPage enterUsername(final String username) {
        PlaywrightActions.type(USERNAME_FIELD, username);
        return this;
    }

    /**
     * Types a password in the login form. The value never reaches the log nor the report.
     *
     * @param password the password to type
     * @return this page object, so calls can be chained
     */
    @Step("Enter the password")
    public LoginPage enterPassword(@Param(mode = Parameter.Mode.MASKED) final String password) {
        PlaywrightActions.typeSecret(PASSWORD_FIELD, password);
        return this;
    }

    /**
     * Submits the login form.
     */
    @Step("Submit the login form")
    public void submit() {
        PlaywrightActions.click(LOGIN_BUTTON);
        PlaywrightActions.waitForPageLoad();
    }

    /**
     * Fills the login form with the credentials of a user file and submits it.
     *
     * @param user the user to log in with, i.e. {@code UserConfig.current()}
     */
    @Step("Log in as the user '{user.userKey}'")
    public void loginAs(final UserConfig user) {
        enterUsername(user.getUsername());
        enterPassword(user.getPassword());
        submit();
    }

    /**
     * Reads the validation message displayed by the form.
     *
     * @return the text of the message
     */
    @Step("Read the login error message")
    public String getErrorMessage() {
        return PlaywrightActions.getText(ERROR_MESSAGE);
    }

    /**
     * Tells whether the application accepted the credentials.
     *
     * @return {@code true} when the toolbar shows the name of a logged in user
     */
    @Step("Is the user logged in?")
    public boolean isUserLoggedIn() {
        return PlaywrightActions.isDisplayedWithin(USER_MENU, 5);
    }
}
