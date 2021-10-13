package rxf.server

/**
 * Simple pair class.
 *
 * @param <A> any type
 * @param <B> any type
</B></A> */
class Pair<A, B>(a: A, b: B) {
    val a: A?
    val b: B?

    init {
        this.a = a
        this.b = b
    }

    override fun equals(o: Any?): Boolean {
        if (this !== o) {
            if (o is Pair<*, *>) {
                val pair = o
                return !((if (null != a) a != pair.a else null != pair.a) || if (null != b) b != pair.b else null != pair.b)
            }
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = a?.hashCode() ?: 0
        result = 31 * result + (b?.hashCode() ?: 0)
        return result
    }
}