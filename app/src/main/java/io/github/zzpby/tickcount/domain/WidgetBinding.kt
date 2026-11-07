package io.github.zzpby.tickcount.domain

/**
 * Which countdown a placed widget is bound to, read from the value the store holds.
 *
 * The widget's slot in `tickcount_widgets` has carried two different things. Until 1.0.7 it
 * was the countdown's **date** as a long, because a countdown was identified by its day;
 * since then it is its **id** as a string. Both are stored under the same key, so a widget
 * placed before that change sits next to a long, and asking the preference for a string
 * throws `ClassCastException`.
 *
 * That is not a visible error: a widget draw swallows `RuntimeException` so that one bad
 * draw does not kill the receiver, which means the exception turned into a widget that was
 * never drawn again — it kept the picture from before the update for ever, and the only way
 * out was deleting the widget or clearing the app's data.
 *
 * Reading the stored value as `Any?` moves that decision here, where it can be tested.
 * Anything that is not a non-empty string means "this widget has no usable binding", which
 * the widget already knows how to answer: it falls back to the soonest countdown.
 */
fun storedWidgetEventId(stored: Any?): String? =
    (stored as? String)?.takeIf { it.isNotEmpty() }
