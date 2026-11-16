package br.ia.sina.guardian

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

suspend fun currentFcmToken(): String? =
    withTimeoutOrNull(10_000) { runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() }
