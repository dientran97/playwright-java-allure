# Playwright + Java + TestNG + Allure test automation framework

A keyword driven UI automation framework built on **Playwright for Java**, **TestNG** and
**Allure**. It is organised around four ideas:

* a test case is a **java class** named after its test case id, listed in a **TestNG xml** whose
  names carry the project id, the URS and the test case id;
* a page object exposes **high level keywords** written with `PlaywrightActions` on top of
  **`NameLocator`** objects (a business name + a selector);
* every keyword is an Allure step, rendered as
  `<start step time>    <log level>    <step description>`;
* the evidence of a run is filed by suite and by test case:
  `reports/<suiteXmlFile>.html`, `screenshots/<suite>/<testcase id>/`,
  `downloads/<suite>/<testcase id>/`.

---

## 1. Prerequisites

| Tool | Version |
|------|---------|
| JDK | 11 or newer (the project compiles with `--release 11`) |
| Maven | 3.8 or newer |
| Playwright browsers | installed once with `mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"` or by simply running the suite the first time |

---

## 2. Running a suite

### 2.1 The short command line

```bash
./run.sh clean verify -PTEST -user=User1 -suiteXmlFolder=Login \
         -suiteXmlFile=testsuite.xml --headless -browser=chrome
```

Use `verify`, not `test`: the suites are driven by the Failsafe plugin (Surefire is switched off in
the pom) so that the Allure report is still produced when a test case fails.

`run.sh` (and `run.bat` on Windows) is a two line wrapper: Maven itself only understands
`-Dkey=value`, so `-user=...`, `-suiteXmlFolder=...`, `-suiteXmlFile=...`, `-browser=...` and
`--headless` are rewritten before Maven is called. It prints the translated command before running
it. Everything Maven already knows (`clean`, `verify`, `-P`, `-D`, `-o`, `-X`, ...) is passed
through untouched.

### 2.2 The plain Maven command line

Exactly the same run, written the way Maven expects:

```bash
mvn clean verify -PTEST -Duser=User1 -DsuiteXmlFolder=Login \
    -DsuiteXmlFile=testsuite.xml -Dheadless=true -Dbrowser=chrome
```

### 2.3 Everything you can pass

| Switch (`run.sh`) | Maven equivalent | Meaning | Default |
|---|---|---|---|
| `-PTEST`, `-PUAT`, `-PDEV`, `-PLOCAL` | same | selects `environments/<env>.properties` | `TEST` |
| `-user=User1` | `-Duser=User1` | selects `users/<user>.properties` | `User1` |
| `-suiteXmlFolder=Login` | `-DsuiteXmlFolder=Login` | folder under `src/test/resources/suites` | `Login` |
| `-suiteXmlFile=testsuite.xml` | `-DsuiteXmlFile=testsuite.xml` | xml inside that folder, also the report name | `testsuite.xml` |
| `-browser=chrome` | `-Dbrowser=chrome` | `chrome`, `chromium`, `edge`, `firefox`, `webkit` | from the xml, then the env file |
| `--headless` / `--headed` | `-Dheadless=true` / `false` | window visibility | from the xml, then the env file |

**Where a value comes from.** The first non blank one wins:

```
-D on the command line   >   <parameter> of the TestNG xml   >   environments/<env>.properties   >   framework default
```

That order is what makes one suite runnable from a CI job (everything on the command line) and
from a developer machine (nothing on the command line) without editing a single file.

### 2.4 Try it without any application

The repository ships a tiny demo application under `src/test/resources/demo-app`, so the whole
chain (browser, screenshots, downloads, tab switching, report) can be checked offline:

```bash
./run.sh clean verify -PLOCAL -suiteXmlFolder=Demo -suiteXmlFile=testsuite.xml --headless
```

The `Login` suite is the counterpart: it runs against the url of the active environment file
(a public demo application out of the box) and therefore needs outbound network access. Point
`environments/TEST.properties` at your own application to make it yours.

---

## 3. Output of a run

### 3.1 The report

`reports/<suiteXmlFile>.html` — a **single, self contained** Allure html file, i.e.
`reports/testsuite.html` for `-suiteXmlFile=testsuite.xml`. It is produced even when test cases
fail: the suite runs in the `integration-test` phase, the report is built in
`post-integration-test`, and only then does the `verify` phase fail the build.

* the **Environment** widget of the overview page shows the browser, the **browser version** and
  the **execution host**, plus the environment, the user and the url that were used;
* the **Suites** page is a two level tree:

```
SP0308_3.2.1.1_Login functionality          <- <suite name> of the TestNG xml
  3.2.1.1_TC001_Login with valid credentials    <- <test name> of the TestNG xml
  3.2.1.2_TC002_Login with invalid credentials
```

* every step is named `<start step time>    <log level>    <step description>`:

```
15:06:45.280    INFO     Open the browser and navigate to https://...
15:06:49.312    INFO     Log in as the user 'User1'
15:06:49.313    INFO       Type 'demo' into Username field
15:06:49.341    INFO       Type the secret value into Password field
15:06:49.354    INFO       Click Login button
15:06:49.559    PASS     Verify that Welcome message contains 'Demo User'
15:08:25.825    FAIL     Verify that Error message shows 'never shown'
```

The level is `INFO` by default, `PASS`/`FAIL` for verifications, and is raised to `WARN` or
`ERROR` as soon as `Log.warn(...)` / `Log.error(...)` is called inside the step.

### 3.2 Screenshots

```
screenshots/<suite name>/<testcase ID>/<URS>_<testcase ID>_<description>_SS<index>.png
```

for example

```
screenshots/SP0308_3.2.1.1_Login functionality/TC001/3.2.1.1_TC001_Login page_SS001.png
```

The index is a three digit counter that **restarts at 001 for every test case**. The same image is
attached to the Allure report **under that very same file name**, so a picture seen in the report is
found on disk without opening anything:

```
3.2.1.1_TC001_Home page after login_SS002.png     <- attachment title in the report
                                                     = file name under screenshots/
```

Two keywords produce them, and both are available on every page object as well:

```java
PlaywrightActions.takeScreenshot("Login page");
PlaywrightActions.takeFullPageScreenshot("Home page after login");
```

**On failure**, as long as a browser is still open, the framework takes one automatically with the
description `Screenshot at the fail step`. Each distinct failure gets its own picture (so a test
case with three failed soft assertions gets three), while a single failure travelling up through
nested keywords only gets one.

### 3.3 Downloads

```
downloads/<suite name>/<testcase ID>/
```

The folder is **emptied before each test case** and is set as the browser download folder, so a
test case only ever sees the files it produced. Every download is intercepted, whichever tab
started it, saved under its suggested name and attached to the report.

```java
Path report = PlaywrightActions.downloadFileByClicking(DemoHomePage.DOWNLOAD_REPORT_LINK);
```

---

## 4. Project layout

```
pom.xml
run.sh / run.bat                         short command line -> maven
src/test/java/
  framework/
    actions/PlaywrightActions.java       the keyword library (@Step + logging + evidence)
    annotations/TestCaseInfo.java        URS, testcase id, description, fallback suite name
    annotations/Tag.java                 @Tag("smoke"), mapped to the Allure "tag" label
    assertions/CustomAssertions.java     hard verifications (stop the test case)
    assertions/SoftAssertions.java       soft verifications (collected, then reported)
    assertions/AbstractAssertions.java   the verification vocabulary shared by both
    config/FrameworkConfig.java          -D > xml <parameter> > env file > default
    config/UserConfig.java               users/<user>.properties
    config/ConfigLoader.java             properties loading + ${placeholder} resolution
    config/FrameworkPaths.java           reports/ screenshots/ downloads/ allure-results/
    context/TestCaseContext.java         suite name, URS, testcase id, folders, screenshot counter
    core/BrowserManager.java             launches browsers (http credentials, downloads path)
    core/PageManager.java                which page/tab/browser the next action runs on
    core/BrowserSession.java             one browser: playwright + browser + context + tabs
    listeners/FrameworkListener.java     TestNG glue
    locator/NameLocator.java             a business name + a selector
    logging/Log.java, LogLevel, StepTracker
    report/ScreenshotManager.java        naming, counter, failure screenshots
    report/DownloadManager.java          per test case download folder
    report/AllureStepListener.java       step names + two level suite tree
    report/AllureEnvironmentWriter.java  browser / version / host in the report
    report/StepNameFormatter.java        "<time>  <level>  <description>"
  pages/                                 page objects (BasePage, LoginPage, demo/...)
  tests/BaseTest.java                    prepares the test case, closes the browser
  tests/login/TC001_...java              one class per test case
src/test/resources/
  environments/TEST.properties UAT DEV LOCAL
  users/User1.properties User2 InvalidUser
  suites/<folder>/<file>.xml             TestNG suites
  demo-app/                              offline demo application
  allure.properties, simplelogger.properties
  META-INF/services/                     registers the Allure listeners
```

---

## 5. Configuration files

### 5.1 Environment — `src/test/resources/environments/<ENV>.properties`

One file per environment, selected with the Maven profile of the same name (`-PTEST`, `-PUAT`, ...).
It holds the url and the general settings of the framework:

```properties
project.id=SP0308
url=https://your-app/login

browser=chrome
headless=false
timeout=30
default.user=User1

browser.channel=          # empty = the Chromium bundled with Playwright; 'chrome' or 'msedge' = installed browser
viewport.width=1920
viewport.height=1080
ignore.https.errors=true
slowmo=0

screenshot.on.failure=true
screenshots.clean.before.testcase=true
tracing.enabled=false     # true records a Playwright trace per test case in target/traces
```

Values may reference `${anotherKey}`, `${aSystemProperty}`, an environment variable, or the two
built-ins `${projectDir}` and `${projectDirUri}`.

### 5.2 User — `src/test/resources/users/<User>.properties`

One file per test account, selected with `-user=<name>`:

```properties
username=demo
password=ZGVtbzEyMw==
password.encoded=true      # remove or set to false to store the password in clear text
name=Demo User
email=demo.user@example.com
```

Read it from a test with `user()` (the user of the run) or `UserConfig.of("User2")` (any other
account):

```java
user().getUsername();  user().getPassword();  user().getName();  user().getEmail();
```

### 5.3 TestNG xml — `src/test/resources/suites/<folder>/<file>.xml`

```xml
<suite name="SP0308_3.2.1.1_Login functionality" verbose="1">

    <parameter name="browser" value="chrome"/>
    <parameter name="isHeadless" value="false"/>

    <test name="3.2.1.1_TC001_Login with valid credentials">
        <classes>
            <class name="tests.login.TC001_login_with_valid_credentials"/>
        </classes>
    </test>
</suite>
```

* `<suite name>` = `<projectID>_<URS>_<short description>` — shown at the first level of the Allure
  "Suites" page and used as the `screenshots/` and `downloads/` sub folder.
* `<test name>` = `<URS>_<testcase ID>_<short description>` — the name of the test case in the
  report. One `<test>` calls exactly one java class.
* `<parameter name="browser">` and `<parameter name="isHeadless">` are the fallback browser
  settings of the suite, overridden by `-Dbrowser` / `-Dheadless`.

---

## 6. Writing a test case

### 6.1 The test class

```java
@TestCaseInfo(urs = "3.2.1.1", id = "TC001", description = "Login with valid credentials",
        suite = "SP0308_3.2.1.1_Login functionality")
@Feature("Authentication")
@Story("Login")
@Tag("smoke")
public class TC001_login_with_valid_credentials extends BaseTest {

    @Test(testName = "3.2.1.1_TC001_Login with valid credentials",
          description = "A registered user reaches the home page after a successful login")
    @Description("Opens the application, logs in and verifies the greeting.")
    @Severity(SeverityLevel.BLOCKER)
    public void e2eTest() {
        PlaywrightActions.openBrowserAndNavigate(url(), user().getUsername(), user().getPassword());

        LoginPage loginPage = new LoginPage();
        loginPage.waitUntilDisplayed();
        PlaywrightActions.takeScreenshot("Login page");

        loginPage.loginAs(user());

        CustomAssertions.assertTextContains(HomePage.WELCOME_MESSAGE, user().getName());
        softly().assertVisible(HomePage.LOGOUT_BUTTON);
        softly().assertAll();
    }
}
```

`@TestCaseInfo` repeats what the xml `<test name>` already says. It is what allows the class to be
launched **straight from the IDE**, where TestNG invents a "Default suite" / "Default test": the
framework then falls back on the annotation to build the same names and the same folders. When the
run is driven by an xml, the xml wins.

`BaseTest` **never opens a browser** — the test case does, which keeps that step in the report and
lets each test case pick its url and its credentials. `BaseTest` only closes what is open, in an
`@AfterMethod`.

### 6.2 The page object

```java
public class LoginPage extends BasePage {

    public static final NameLocator USERNAME_FIELD = new NameLocator("Username field", "#username");
    public static final NameLocator PASSWORD_FIELD = new NameLocator("Password field", "#password");
    public static final NameLocator LOGIN_BUTTON   = new NameLocator("Login button", "#loginbtn");

    @Override
    protected NameLocator pageIdentifier() {
        return LOGIN_BUTTON;
    }

    @Step("Log in as the user '{user.userKey}'")
    public void loginAs(final UserConfig user) {
        PlaywrightActions.type(USERNAME_FIELD, user.getUsername());
        PlaywrightActions.typeSecret(PASSWORD_FIELD, user.getPassword());
        PlaywrightActions.click(LOGIN_BUTTON);
    }
}
```

A `NameLocator` keeps the **selector**, not a `Locator` bound to a page, so the element is looked up
on whichever page is active when the keyword runs. Dynamic elements use `format(...)`:

```java
static final NameLocator ROW = new NameLocator("Row %s", "//tr[td[normalize-space()='%s']]");
PlaywrightActions.click(ROW.format("Invoice 42"));     // step: Click Row Invoice 42
```

Elements inside an iframe: `MY_FIELD.inFrame("#payment-iframe")`. One element out of many:
`ROWS.nth(2)`.

### 6.3 The keywords

`PlaywrightActions` covers browser lifecycle (`openBrowserAndNavigate`, `navigateTo`, `reloadPage`,
`goBack`, `closeBrowser`), interactions (`click`, `doubleClick`, `forceClick`, `type`, `typeSecret`,
`clear`, `pressKey`, `check`, `uncheck`, `selectOption`, `selectOptionByLabel`, `hover`,
`scrollIntoView`, `uploadFile`), reads (`getText`, `getValue`, `getAttribute`, `getCount`,
`isVisible`, `isEnabled`, `isChecked`, `getPageTitle`, `getCurrentUrl`), waits (`waitForVisible`,
`waitForHidden`, `waitForPageLoad`, `isDisplayedWithin`), screenshots, tab and browser switching,
downloads and dialogs. Every one of them is documented with its parameters and its return value.

When a keyword fails it logs the failure, attaches the stack trace, takes the
`Screenshot at the fail step` picture and **rethrows** — a keyword never swallows an error.

### 6.4 Tabs and second browsers

The framework always acts on the page it was last switched to, so no page object needs to know a
new tab was opened:

```java
PlaywrightActions.clickAndSwitchToNewTab(HomePage.OPEN_DOCS_LINK);  // now on the new tab
CustomAssertions.assertText(DocsPage.DOCS_TITLE, "User documentation");
PlaywrightActions.switchToTab(0);                                   // back on the first tab
```

Also available: `switchToTabByTitle`, `switchToTabByUrl`, `switchToLastTab`, `openNewTabAndNavigate`,
`closeCurrentTab`, and for a second browser (a second user, for instance):

```java
PlaywrightActions.openNewBrowserAndNavigate("checker", url(), reviewer.getUsername(), reviewer.getPassword());
// ... acts on the checker browser ...
PlaywrightActions.switchToBrowser("default");
```

### 6.5 Verifications

```java
CustomAssertions.assertVisible(HomePage.WELCOME_MESSAGE);   // stops the test case on failure
CustomAssertions.assertText(HomePage.USER_NAME, "Demo User");

softly().assertText(HomePage.PAGE_TITLE, "My account");     // collected, the test case goes on
softly().assertVisible(HomePage.LOGOUT_BUTTON);
softly().assertAll();                                       // reports everything at once
```

Both flavours log `VERIFY PASSED` / `VERIFY FAILED`, attach the expected and actual values and take
the failure screenshot. A forgotten `assertAll()` cannot hide anything: the framework verifies the
collector when the test method ends and fails the test case if something was left.

---

## 7. Running from the IDE

Press **Run** or **Debug** on a test class or on the `e2eTest` method — no TestNG xml is needed.
The suite name, the URS, the test case id, the screenshot folder and the download folder are taken
from `@TestCaseInfo`, and the browser settings from the environment file.

By default the IDE uses the `TEST` environment. To run against another one, add the VM option of
your run configuration:

```
-Denv=LOCAL -Duser=User1 -Dheadless=false
```

`-Denv=LOCAL` is what the bundled `Demo` test cases need, since they point at
`src/test/resources/demo-app`.

---

## 8. Notes on the specification

A few points of the original specification cannot be written literally in Java/Maven/Allure; this
is what the framework does instead, and why:

1. **`mvn ... -user=User1 --headless -browser=chrome`** — Maven rejects any option it does not know
   (`Unrecognized option: -user=User1`). `run.sh` / `run.bat` accept that exact syntax and translate
   it to `-Duser=...`, `-Dheadless=true`, `-Dbrowser=...`; `mvn` with `-D` works too.
2. **`@Step("Click {locator.getName()}")`** — Allure's template engine resolves *properties*, not
   method calls, and throws on `{x.getName()}`. The framework uses `@Step("Click {locator.name}")`,
   which resolves through the very same `getName()` getter.
3. **`@Test(name = ...)`** — TestNG's attribute is `testName`; `@Test(testName = "3.2.1.1_TC001_...")`
   is used.
4. **`@Tag`** — Allure for Java has no `@Tag` annotation, so the framework declares its own
   (`framework.annotations.Tag`) mapped onto the Allure `tag` label.
5. **Keywords rethrow after logging.** The sketched `try/catch` that only logs would let a test case
   pass after a failed click, so each keyword logs, attaches the evidence, takes the screenshot and
   then rethrows a `FrameworkException`.
6. **Step name separator.** The three columns of a step name are separated by non breaking spaces:
   an html renderer collapses ordinary consecutive spaces, which would break the alignment.

---

## 9. Useful commands

```bash
# a suite, with everything on the command line
./run.sh clean verify -PTEST -user=User1 -suiteXmlFolder=Login -suiteXmlFile=testsuite.xml --headless -browser=chrome

# the offline demo suite
./run.sh clean verify -PLOCAL -suiteXmlFolder=Demo -suiteXmlFile=testsuite.xml --headless

# one class only, no xml (same thing the IDE does)
mvn clean verify -PLOCAL -Dit.test=TC001_login_with_valid_credentials -Dheadless=true

# install the Playwright browsers explicitly
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```
