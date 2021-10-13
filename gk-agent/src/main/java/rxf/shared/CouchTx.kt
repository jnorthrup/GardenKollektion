package rxf.shared

class CouchTx {
    ////////////////////////////// RF hideousity below
    var ok: Boolean? = null
    var id: String? = null
    var rev: String? = null
    var error: String? = null
    var reason: String? = null
    override fun toString(): String =
        ("CouchTx{" + "ok=" + ok + ", key='" + id + '\'' + ", rev='" + rev + '\'' + ", error='" + error + '\'' + ", reason='" + reason + '\'' + '}')

    fun ok(): Boolean = ok ?: false

    fun id(): String? = id

    fun rev(): String? = rev

    fun error(): String? = error

    fun reason(): String? = reason

    fun ok(ok: Boolean?): CouchTx {
        this.ok = ok
        return this
    }

    fun id(id: String?)= apply{ this.id = id }

    fun rev(rev: String?) = apply { this.rev = rev }

    fun error(error: String?)=apply{ this.error = error }

    fun reason(reason: String?)= apply{ this.reason = reason }
}