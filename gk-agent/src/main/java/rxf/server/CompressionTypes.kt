package rxf.server

/**
 * User: jim
 * Date: 4/30/12
 * Time: 12:18 AM
 */
enum class CompressionTypes(val _suf :String?=null) {
    gzip("gz"),
    bzip2("bz2"),
    xz,
    zstd,
    lz4,
    lzo,
    `$7za`("7z");
    val suffix get() = _suf ?: name

}