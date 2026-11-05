package fr.courrierdesnuages.app.data

import java.security.MessageDigest
import java.security.SecureRandom

/** Les codes à 4 chiffres sont salés puis hachés avant d'être enregistrés. */
object Codes {
    private val hasard = SecureRandom()

    fun nouveauSel(): String {
        val octets = ByteArray(16)
        hasard.nextBytes(octets)
        return hexa(octets)
    }

    fun empreinte(code: String, sel: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return hexa(md.digest("$sel:$code".toByteArray(Charsets.UTF_8)))
    }

    fun verifie(code: String, sel: String, empreinte: String): Boolean =
        sel.isNotEmpty() && empreinte.isNotEmpty() && empreinte(code, sel) == empreinte

    private fun hexa(octets: ByteArray): String = buildString(octets.size * 2) {
        for (o in octets) {
            val v = o.toInt() and 0xff
            append("0123456789abcdef"[v shr 4])
            append("0123456789abcdef"[v and 0x0f])
        }
    }
}
