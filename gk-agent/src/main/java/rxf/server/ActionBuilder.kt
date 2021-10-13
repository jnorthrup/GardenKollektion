package rxf.server

import one.xio.HttpHeaders
import java.nio.channels.SelectionKey
import java.util.concurrent.atomic.AtomicReference

/**
 * User: jim
 * Date: 5/29/12
 * Time: 1:58 PM
 */
abstract class ActionBuilder {
    private val state = AtomicReference<Rfc822HeaderState>()
    private var key: SelectionKey? = null

    init {
        currentAction.set(this)
    }

    abstract fun fire(): TerminalBuilder?
    override fun toString(): String {
        return "ActionBuilder{state=$state, key=$key}"
    }

    fun state(): Rfc822HeaderState? {
        var ret = state.get()
        if (null == ret) state.set(Rfc822HeaderState(*HEADER_INTEREST).also { ret = it })
        return ret
    }

    fun key(): SelectionKey? {
        return key
    }

    fun state(state: Rfc822HeaderState): ActionBuilder =apply{
        this.state.set(state)
    }

    fun key(key: SelectionKey?): ActionBuilder {
        this.key = key
        return this
    }

    companion object {
        val HEADER_INTEREST: Array<String?> =
            Rfc822HeaderState.Companion.staticHeaderStrings(HttpHeaders.ETag, HttpHeaders.`Content$2dLength`)
        protected var currentAction: ThreadLocal<ActionBuilder?> = InheritableThreadLocal()
        fun get(): ActionBuilder? {
            if (currentAction.get() == null) currentAction.set(object : ActionBuilder() {
                override fun fire(): TerminalBuilder? {
                    throw AbstractMethodError(
                        "This is a ActionBuilder with no DbKeysBuilder and therefore now Terminal")
                }
            })
            return currentAction.get()
        }
    }
}