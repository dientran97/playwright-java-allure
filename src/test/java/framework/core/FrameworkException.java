package framework.core;

/**
 * Thrown when a framework keyword cannot do its job. The message always starts with the keyword
 * description, so the console log, the Allure step and the stack trace tell the same story.
 */
public class FrameworkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of what the framework was trying to do
     * @param cause   the original Playwright or JDK error
     */
    public FrameworkException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message description of what the framework was trying to do
     */
    public FrameworkException(final String message) {
        super(message);
    }
}
