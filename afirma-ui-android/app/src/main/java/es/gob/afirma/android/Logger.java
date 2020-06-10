package es.gob.afirma.android;

import android.util.Log;

import es.gob.afirma.BuildConfig;

/**
 * Utilities class that manages the app logs messages.
 */
public class Logger {

    /**
     * Attribute that indicates if the app is in production or not.
     */
    public static final boolean isProduction;

    static {
        isProduction = true;
    }

    /**
     * Private constructor.
     */
    private Logger() {
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     */
    public static void w(String tag, String message) {
        if (!isProduction) {
            Log.w(tag, message);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     * @param t       Throwable object to include in the log message.
     */
    public static void w(String tag, String message, Throwable t) {
        if (!isProduction) {
            Log.w(tag, message, t);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     */
    public static void v(String tag, String message) {
        if (!isProduction) {
            Log.v(tag, message);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     * @param t       Throwable object to include in the log message.
     */
    public static void v(String tag, String message, Throwable t) {
        if (!isProduction) {
            Log.v(tag, message, t);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     */
    public static void i(String tag, String message) {
        if (!isProduction) {
            Log.i(tag, message);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     * @param t       Throwable object to include in the log message.
     */
    public static void i(String tag, String message, Throwable t) {
        if (!isProduction) {
            Log.i(tag, message, t);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     */
    public static void e(String tag, String message) {
        if (!isProduction) {
            Log.e(tag, message);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     * @param t       Throwable object to include in the log message.
     */
    public static void e(String tag, String message, Throwable t) {
        if (!isProduction) {
            Log.e(tag, message, t);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     */
    public static void d(String tag, String message) {
        if (!isProduction) {
            Log.d(tag, message);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     * @param t       Throwable object to include in the log message.
     */
    public static void d(String tag, String message, Throwable t) {
        if (!isProduction) {
            Log.d(tag, message, t);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     */
    public static void wtf(String tag, String message) {
        if (!isProduction) {
            Log.wtf(tag, message);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param tag     Log tag.
     * @param message Message to write.
     * @param t       Throwable object to include in the log message.
     */
    public static void wtf(String tag, String message, Throwable t) {
        if (!isProduction) {
            Log.wtf(tag, message, t);
        }
    }

    /**
     * Method that write a log line if the app it is not in production.
     *
     * @param priority message priority.
     * @param tag      Log tag.
     * @param message  Message to write.
     */
    public static void println(int priority, String tag, String message) {
        if (!isProduction) {
            Log.println(priority, tag, message);
        }
    }
}
