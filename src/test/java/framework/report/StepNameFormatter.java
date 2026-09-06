package framework.report;

import framework.logging.LogLevel;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds the step names shown in the Allure report:
 * {@code <start step time>    <log level>    <step description>}.
 *
 * <p>The three columns are separated by non breaking spaces on purpose: a browser collapses a run
 * of ordinary spaces into a single one, which would destroy the alignment of the report.</p>
 */
public final class StepNameFormatter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    /** Non breaking space: an HTML renderer collapses ordinary spaces, this one it keeps. */
    private static final char PAD = '\u00A0';
    private static final String SEPARATOR = "\u00A0\u00A0\u00A0\u00A0";
    private static final int LEVEL_WIDTH = 5;

    private StepNameFormatter() {
    }

    /**
     * Formats one step name.
     *
     * @param startTime   moment the step was started
     * @param level       highest log level reached inside the step
     * @param description original step description, i.e. the text of {@code @Step}
     * @return the formatted name
     */
    public static String format(final LocalTime startTime, final LogLevel level, final String description) {
        return TIME.format(startTime) + SEPARATOR + pad(level.name()) + SEPARATOR
                + (description == null ? "" : description);
    }

    /**
     * Tells whether a name has already been formatted, so a step is never decorated twice.
     *
     * @param name the current step name
     * @return {@code true} when the name already starts with the time column
     */
    public static boolean isFormatted(final String name) {
        if (name == null || name.length() < 13) {
            return false;
        }
        return name.charAt(2) == ':' && name.charAt(5) == ':' && name.charAt(8) == '.'
                && name.charAt(12) == PAD;
    }

    private static String pad(final String level) {
        final StringBuilder padded = new StringBuilder(level);
        while (padded.length() < LEVEL_WIDTH) {
            padded.append(PAD);
        }
        return padded.toString();
    }
}
