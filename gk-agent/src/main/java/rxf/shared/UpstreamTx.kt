package rxf.shared

data class  UpstreamTx(
    var ok: Boolean? = null,
    var id: String? = null,
    var rev: String? = null,
    var error: String? = null,
    var reason: String? = null)