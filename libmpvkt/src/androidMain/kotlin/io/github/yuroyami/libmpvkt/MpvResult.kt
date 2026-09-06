package io.github.yuroyami.libmpvkt

/** What an mpv call answered. mpv errors are values here, never exceptions. */
public sealed interface MpvResult<out T> {
    public data class Ok<T>(val value: T) : MpvResult<T>
    public data class Fail(val error: MpvError, val detail: String? = null) : MpvResult<Nothing>
}

public fun <T> MpvResult<T>.getOrNull(): T? = (this as? MpvResult.Ok)?.value

public fun <T> MpvResult<T>.getOrThrow(): T = when (this) {
    is MpvResult.Ok -> value
    is MpvResult.Fail -> throw MpvException(error, detail)
}

public inline fun <T, R> MpvResult<T>.map(transform: (T) -> R): MpvResult<R> = when (this) {
    is MpvResult.Ok -> MpvResult.Ok(transform(value))
    is MpvResult.Fail -> this
}

public inline fun <T> MpvResult<T>.onFail(action: (MpvResult.Fail) -> Unit): MpvResult<T> {
    if (this is MpvResult.Fail) action(this)
    return this
}

public val MpvResult<*>.isOk: Boolean get() = this is MpvResult.Ok

internal fun unitResult(code: Int): MpvResult<Unit> = if (code >= 0) MpvResult.Ok(Unit) else MpvResult.Fail(MpvError.of(code))
