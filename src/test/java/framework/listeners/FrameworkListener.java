package framework.listeners;

import framework.assertions.SoftAssertions;
import framework.config.FrameworkConfig;
import framework.logging.Log;
import framework.report.AllureEnvironmentWriter;
import framework.report.ScreenshotManager;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Optional;

/**
 * TestNG listener that glues the framework to the test runner. It is registered once, with
 * {@code @Listeners} on {@code tests.BaseTest}, so it is active both under Maven and when a single
 * class is started from the IDE.
 *
 * <p>It is responsible for three things:</p>
 * <ul>
 *     <li>publishing the run information (browser, version, host) to the Allure report;</li>
 *     <li>failing a test method whose soft assertions were never verified;</li>
 *     <li>taking the "Screenshot at the fail step" picture for failures that did not go through a
 *         framework keyword, i.e. a raw Playwright call or a plain TestNG assertion.</li>
 * </ul>
 */
public class FrameworkListener implements ITestListener, IInvokedMethodListener {

    @Override
    public void onStart(final ITestContext context) {
        FrameworkConfig.captureXmlParameters(context);
        AllureEnvironmentWriter.recordRunInfo();
        Log.info("Starting '{}' of the suite '{}' [environment={}, user={}, browser={}, headless={}]",
                context.getName(), context.getSuite().getName(), FrameworkConfig.environment(),
                FrameworkConfig.userName(), FrameworkConfig.browser(), FrameworkConfig.headless());
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult result) {
        if (!method.isTestMethod()) {
            return;
        }
        if (result.getStatus() == ITestResult.SUCCESS) {
            final Optional<AssertionError> softFailure = SoftAssertions.takeCollectedFailure();
            if (softFailure.isPresent()) {
                Log.fail("The test method ended with unverified soft assertion failures");
                result.setStatus(ITestResult.FAILURE);
                result.setThrowable(softFailure.get());
                // each of those failures was photographed when it happened, nothing to add here
            }
            return;
        }
        if (result.getStatus() == ITestResult.FAILURE) {
            // last resort: a failure raised outside of a framework keyword still gets its picture
            ScreenshotManager.captureFailure(result.getThrowable());
        }
    }

    @Override
    public void onTestSuccess(final ITestResult result) {
        Log.pass("PASSED  - {}", result.getName());
    }

    @Override
    public void onTestFailure(final ITestResult result) {
        final Throwable error = result.getThrowable();
        Log.fail("FAILED  - {} : {}", result.getName(), error == null ? "no details" : error.getMessage());
    }

    @Override
    public void onTestSkipped(final ITestResult result) {
        Log.warn("SKIPPED - {}", result.getName());
    }

    @Override
    public void onFinish(final ITestContext context) {
        Log.info("Finished '{}': {} passed, {} failed, {} skipped",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }
}
