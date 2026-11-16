package br.ia.sina.protection

import android.content.Context
import android.provider.Telephony
import br.ia.sina.data.MessageSource

object MessageSources {
    private val whatsapp = setOf("com.whatsapp", "com.whatsapp.w4b")
    private val knownSms = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.motorola.messaging",
        "com.android.messaging",
    )

    fun resolve(context: Context, packageName: String): MessageSource? = when {
        packageName in whatsapp -> MessageSource.WHATSAPP
        packageName in knownSms -> MessageSource.SMS
        packageName == Telephony.Sms.getDefaultSmsPackage(context) -> MessageSource.SMS
        else -> null
    }
}
