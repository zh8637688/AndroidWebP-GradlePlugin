package cz.gradleplugin.android.webp.util;

/**
 * @author haozhou
 */

public class Logger {
    private static org.gradle.api.logging.Logger gradleLogger = null;

    public static void initialize(org.gradle.api.logging.Logger gradleLogger) {
        Logger.gradleLogger = gradleLogger;
    }

    public static void i(String s) {
        if (gradleLogger != null) {
            gradleLogger.lifecycle(String.format("WebPAndroidPlugin: %s", s));
        }
    }
}
