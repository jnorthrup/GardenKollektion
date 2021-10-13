package rxf.server

/**
 * User: jim
 * Date: 4/30/12
 * Time: 12:18 AM
 */
enum class CompressionTypes(vararg suffix: String) {
    gzip("gz"), bzip2("bz2"), xz;

    var suffix: String

    init {
        this.suffix = if (suffix.size == 0) name else suffix[0]
    }
}