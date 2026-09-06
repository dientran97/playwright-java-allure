package framework.report;

import framework.context.TestCaseContext;
import framework.logging.LogLevel;
import framework.logging.StepTracker;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;

import java.util.List;

/**
 * Allure service loader listener that gives the report its shape. It is registered through
 * {@code src/test/resources/META-INF/services}.
 *
 * <p>It does two things:</p>
 * <ul>
 *     <li>renames every step to {@code <start step time>    <log level>    <step description>};</li>
 *     <li>renames every test case to the {@code <test>} name of the TestNG xml and flattens the
 *         suite labels, so the "Suites" page of the report shows exactly two levels:
 *         the test suite name and, under it, its test cases.</li>
 * </ul>
 */
public class AllureStepListener implements StepLifecycleListener, TestLifecycleListener {

    /** Value written instead of a secret, see {@link #maskSecrets(List)}. */
    private static final String MASK = "****";

    private static final String PARENT_SUITE = "parentSuite";
    private static final String SUITE = "suite";
    private static final String SUB_SUITE = "subSuite";

    @Override
    public void beforeStepStart(final StepResult result) {
        StepTracker.push();
    }

    @Override
    public void beforeStepStop(final StepResult result) {
        final LogLevel statusLevel = levelOf(result.getStatus());
        final StepTracker.StepMeta meta = StepTracker.pop().orElse(null);
        if (meta == null || StepNameFormatter.isFormatted(result.getName())) {
            return;
        }
        maskSecrets(result.getParameters());
        final LogLevel level = LogLevel.max(meta.getLevel(), statusLevel);
        if (level.getRank() >= LogLevel.ERROR.getRank() && result.getStatus() == Status.PASSED) {
            // a soft assertion that failed does not throw, the step must still be red
            result.setStatus(Status.FAILED);
        }
        result.setName(StepNameFormatter.format(meta.getStartTime(), level, result.getName()));
    }

    @Override
    public void beforeTestStart(final TestResult result) {
        TestCaseContext.currentOrEmpty().ifPresent(testCase -> {
            result.setName(testCase.getTestName());
            result.setFullName(testCase.getSuiteName() + " : " + testCase.getTestName());
            if (result.getDescription() == null || result.getDescription().trim().isEmpty()) {
                result.setDescription(testCase.getDescription());
            }
            flattenSuiteLabels(result, testCase.getSuiteName());
        });
    }

    @Override
    public void beforeTestStop(final TestResult result) {
        maskSecrets(result.getParameters());
        StepTracker.clear();
    }

    /**
     * Overwrites the value of every parameter a keyword declared as a secret with
     * {@link #MASK}.
     *
     * <p>Annotating a parameter with {@code @Param(mode = MASKED)} only tells the Allure user
     * interface to hide it: the real value is still written to the json results and therefore
     * embedded in the published html report. Replacing the value here is what actually keeps a
     * password out of the report file.</p>
     *
     * @param parameters the parameters of a step or of a test case, may be {@code null}
     */
    private void maskSecrets(final List<Parameter> parameters) {
        if (parameters == null) {
            return;
        }
        parameters.stream()
                .filter(parameter -> Parameter.Mode.MASKED == parameter.getMode()
                        || Boolean.TRUE.equals(parameter.getExcluded()))
                .forEach(parameter -> parameter.setValue(MASK));
    }

    /**
     * Replaces the three suite labels Allure derives from TestNG by the single test suite name, so
     * the collection page of the report is a two level tree.
     *
     * @param result    the test result being started
     * @param suiteName the name declared by {@code <suite name="...">}
     */
    private void flattenSuiteLabels(final TestResult result, final String suiteName) {
        final List<Label> labels = result.getLabels();
        labels.removeIf(label -> PARENT_SUITE.equals(label.getName())
                || SUITE.equals(label.getName())
                || SUB_SUITE.equals(label.getName()));
        labels.add(new Label().setName(SUITE).setValue(suiteName));
    }

    private static LogLevel levelOf(final Status status) {
        if (status == null) {
            return LogLevel.INFO;
        }
        switch (status) {
            case FAILED:
            case BROKEN:
                return LogLevel.ERROR;
            case SKIPPED:
                return LogLevel.WARN;
            default:
                return LogLevel.INFO;
        }
    }
}
