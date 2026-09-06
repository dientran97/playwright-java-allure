package framework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a test case: its URS reference, its test case id and its short description.
 *
 * <p>The same three values are also encoded in the {@code <test name="...">} of the TestNG xml
 * ({@code <URS>_<testcase ID>_short description}). Declaring them once more on the class is what
 * lets the framework build the correct screenshot folder, download folder and Allure names when
 * the class is launched directly from the IDE, where no TestNG xml is involved.</p>
 *
 * <pre>{@code
 * @TestCaseInfo(urs = "3.2.1.1", id = "TC001", description = "Login with valid credentials")
 * public class TC001_login_with_valid_credentials extends BaseTest { ... }
 * }</pre>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TestCaseInfo {

    /**
     * @return the URS reference covered by the test case, i.e. {@code 3.2.1.1} or
     *         {@code 3.2.1.2_3.2.1.3_3.2.1.12}
     */
    String urs();

    /**
     * @return the test case identifier, i.e. {@code TC001}
     */
    String id();

    /**
     * @return a short human readable description of the test case
     */
    String description() default "";

    /**
     * @return the test suite name to fall back on when the run is not driven by a TestNG xml
     *         (IDE run). Leave empty to let the framework build
     *         {@code <projectId>_<URS>_<description>}.
     */
    String suite() default "";
}
