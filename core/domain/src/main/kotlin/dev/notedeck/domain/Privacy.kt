package dev.notedeck.domain

/** Personal details stay on the phone: redacted from anything sent to a cloud model. */
object Privacy {
    private val email = Regex("""[\w.+-]+@[\w-]+\.[\w.]+""")
    private val phone = Regex("""\+?\d[\d ()-]{7,}\d""")

    fun redact(text: String): String = text.replace(email, "[email]").replace(phone, "[phone]")
}
