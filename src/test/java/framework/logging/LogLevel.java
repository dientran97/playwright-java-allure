package framework.logging;

/**
 * Severity shown in the middle column of every Allure step name
 * ({@code <start time>  <log level>  <step description>}).
 */
public enum LogLevel {

    /** Low level detail, only written to the console log. */
    DEBUG(0),
    /** Normal progress information, the default of every step. */
    INFO(1),
    /** A verification that succeeded. */
    PASS(2),
    /** Something unexpected that does not fail the test case. */
    WARN(3),
    /** A failure. */
    ERROR(4),
    /** A verification that failed. */
    FAIL(5);

    private final int rank;

    LogLevel(final int rank) {
        this.rank = rank;
    }

    /**
     * @return the numeric severity, used to keep the highest level reached inside a step
     */
    public int getRank() {
        return rank;
    }

    /**
     * Returns the most severe of two levels.
     *
     * @param first  first level, may be {@code null}
     * @param second second level, may be {@code null}
     * @return the level with the highest rank, {@code null} only when both are {@code null}
     */
    public static LogLevel max(final LogLevel first, final LogLevel second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return second.rank > first.rank ? second : first;
    }
}
