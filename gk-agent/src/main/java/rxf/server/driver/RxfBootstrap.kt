package rxf.server.driver

import java.util.*

object RxfBootstrap {
    fun getVar(rxf_var: String, vararg defaultVal: String?): String? {
        val javapropname =
            "rxf.server." + rxf_var.lowercase(Locale.getDefault()).replace("^rxf_(server_)?".toRegex(), "")
                .replace('_', '.')
        val rxfenv = System.getenv(rxf_var)
        var `var` = rxfenv ?: System.getProperty(javapropname)
        `var` = if (null == `var` && defaultVal.size > 0) defaultVal[0] else `var`
        if (null != `var`) {
            System.setProperty(javapropname, `var`)
            System.err.println("// -D$javapropname=\"$`var`\"")
        }
        return `var`
    }
}