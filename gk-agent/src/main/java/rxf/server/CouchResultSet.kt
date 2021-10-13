package rxf.server

/**
 * User: jim
 * Date: 5/16/12
 * Time: 7:56 PM
 */
class CouchResultSet<K, V> {
    var totalRows: Long = 0
    var offset: Long = 0
    var rows: List<tuple<K, V>>? = null

    class tuple<K, V> {
        var id: String? = null
        var key: K? = null
        var value: V? = null
        var doc: Map<String, *>? = null
    }
}