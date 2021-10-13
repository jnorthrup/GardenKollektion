package rxf.server.driver

import vec.util.logDebug
import java.util.*

object RxfBootstrap {
    fun getVar(rxf_var: String, vararg defaultVal: String?): String? {
        val javapropname =
            "rxf.server." + rxf_var.lowercase(Locale.getDefault()).replace("^rxf_(server_)?".toRegex(), "")
                .replace('_', '.')
        val rxfenv = System.getenv(rxf_var)
        var theVar = rxfenv ?: System.getProperty(javapropname)
        theVar = (defaultVal.takeIf { it.isNotEmpty() })?.let { theVar } ?: defaultVal[0]
        return ( theVar?.also {
            System.setProperty(javapropname, it)
            logDebug { "// -D$javapropname=\"$it\"" }
        })
    }
}