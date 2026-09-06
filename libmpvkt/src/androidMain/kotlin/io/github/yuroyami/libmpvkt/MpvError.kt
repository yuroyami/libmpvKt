package io.github.yuroyami.libmpvkt

/** The `mpv_error` codes. [SUCCESS] is not an error; every other value is negative. */
public enum class MpvError(public val code: Int) {
    SUCCESS(0), EVENT_QUEUE_FULL(-1), NOMEM(-2), UNINITIALIZED(-3), INVALID_PARAMETER(-4),
    OPTION_NOT_FOUND(-5), OPTION_FORMAT(-6), OPTION_ERROR(-7), PROPERTY_NOT_FOUND(-8),
    PROPERTY_FORMAT(-9), PROPERTY_UNAVAILABLE(-10), PROPERTY_ERROR(-11), COMMAND(-12),
    LOADING_FAILED(-13), AO_INIT_FAILED(-14), VO_INIT_FAILED(-15), NOTHING_TO_PLAY(-16),
    UNKNOWN_FORMAT(-17), UNSUPPORTED(-18), NOT_IMPLEMENTED(-19), GENERIC(-20);

    public val isError: Boolean get() = code < 0

    public companion object {
        /** The value for an mpv return code; unknown negative codes map to [GENERIC], non-negative to [SUCCESS]. */
        public fun of(code: Int): MpvError = if (code >= 0) SUCCESS else entries.firstOrNull { it.code == code } ?: GENERIC
    }
}

public class MpvException(public val error: MpvError, detail: String? = null) :
    RuntimeException(if (detail == null) error.name else "${error.name}: $detail")
