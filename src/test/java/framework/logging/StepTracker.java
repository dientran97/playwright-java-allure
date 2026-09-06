package framework.logging;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Keeps, per thread, the start time and the highest log level reached by every Allure step that is
 * currently open. The framework uses it to rewrite step names as
 * {@code <start time>  <log level>  <step description>}.
 *
 * <p>The stack is maintained by {@code framework.report.AllureStepListener}; test code only
 * interacts with it indirectly, through {@link Log}.</p>
 */
public final class StepTracker {

    private static final ThreadLocal<Deque<StepMeta>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<LogLevel> PENDING_LEVEL = new ThreadLocal<>();

    private StepTracker() {
    }

    /**
     * Registers a newly started step.
     *
     * @return the metadata created for that step
     */
    public static StepMeta push() {
        final StepMeta meta = new StepMeta(LocalTime.now());
        final LogLevel pending = PENDING_LEVEL.get();
        if (pending != null) {
            meta.setLevel(pending);
            PENDING_LEVEL.remove();
        }
        STACK.get().push(meta);
        return meta;
    }

    /**
     * Declares the level of the next step that will be started on this thread. Used by
     * {@link Log#step(LogLevel, String)} to publish a stand alone log line with the right severity.
     *
     * @param level level applied to the next step
     */
    public static void setPendingLevel(final LogLevel level) {
        PENDING_LEVEL.set(level);
    }

    /**
     * Removes the innermost open step.
     *
     * @return its metadata, or an empty optional when no step is open
     */
    public static Optional<StepMeta> pop() {
        final Deque<StepMeta> stack = STACK.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.pop());
    }

    /**
     * @return the metadata of the innermost open step, or an empty optional outside of any step
     */
    public static Optional<StepMeta> current() {
        final Deque<StepMeta> stack = STACK.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek());
    }

    /**
     * Raises the level of the innermost open step when the given one is more severe.
     *
     * @param level level reported by {@link Log}
     */
    public static void raise(final LogLevel level) {
        current().ifPresent(meta -> meta.setLevel(LogLevel.max(meta.getLevel(), level)));
    }

    /**
     * Drops every open step of the current thread, called between two test cases.
     */
    public static void clear() {
        STACK.get().clear();
        STACK.remove();
        PENDING_LEVEL.remove();
    }

    /**
     * Start time and severity of a single Allure step.
     */
    public static final class StepMeta {

        private final LocalTime startTime;
        private LogLevel level = LogLevel.INFO;

        private StepMeta(final LocalTime startTime) {
            this.startTime = startTime;
        }

        /**
         * @return the wall clock time at which the step was started
         */
        public LocalTime getStartTime() {
            return startTime;
        }

        /**
         * @return the highest level reported inside the step so far
         */
        public LogLevel getLevel() {
            return level;
        }

        /**
         * @param level the new level of the step
         */
        public void setLevel(final LogLevel level) {
            this.level = level;
        }
    }
}
