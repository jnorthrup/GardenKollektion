package rxf.server

import one.xio.AsioVisitor
import one.xio.HttpMethod
import rxf.server.driver.RxfBootstrap
import java.util.*
import java.util.regex.Pattern

/**
 * User: jim
 * Date: 6/25/12
 * Time: 1:24 PM
 */
interface CouchNamespace {
    val orgName: String?
    fun setOrgname(orgname: String?)
    var entityName: String?
    val defaultEntityName: String?

    enum class ns {
        orgname {
            override fun setMe(cl: CouchNamespace, ns: String?) {
                cl.setOrgname(ns)
            }
        },
        entityName {
            override fun setMe(cl: CouchNamespace, ns: String?) {
                cl.entityName = ns
            }
        };

        abstract fun setMe(cl: CouchNamespace, ns: String?)
    }

    companion object {
        /**
         * a map of http methods each containing an ordered map of regexes tested in order of
         * map insertion.
         */
        val NAMESPACE: MutableMap<HttpMethod, Map<Pattern, Class<out AsioVisitor.Impl>>> = EnumMap(
            HttpMethod::class.java)

        /**
         * defines where 1xio/rxf finds static content root.
         */
        val COUCH_DEFAULT_FS_ROOT = RxfBootstrap.getVar("RXF_SERVER_CONTENT_ROOT", "./")

        /**
         * creates the orgname used in factories without localized namespaces
         */
        val COUCH_DEFAULT_ORGNAME = RxfBootstrap.getVar("RXF_ORGNAME", "rxf_")
    }
}