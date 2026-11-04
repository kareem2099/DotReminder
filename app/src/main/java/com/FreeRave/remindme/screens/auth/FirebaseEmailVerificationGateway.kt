package com.FreeRave.remindme.screens.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import com.FreeRave.remindme.security.awaitAuth
import com.FreeRave.remindme.security.AuthNetworkTimeout

internal class FirebaseEmailVerificationGateway(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : EmailVerificationGateway {
    override suspend fun readAccount(reload: Boolean): VerificationAccount? = mapped {
        val user = auth.currentUser ?: return@mapped null
        val initialEmail = user.email
        if (reload) user.reload().awaitAuth()
        val current = auth.currentUser
        if (current == null || current.uid != user.uid || current.email != initialEmail) changedAccount()
        VerificationAccount(current.uid, current.email.orEmpty(), current.isEmailVerified)
    }

    override suspend fun sendLink(account: VerificationAccount) = mapped {
        val user = matchingUser(account)
        if (user.isEmailVerified) changedAccount()
        user.sendEmailVerification().awaitAuth()
        matchingUser(account)
        Unit
    }

    override suspend fun syncVerifiedAccount(account: VerificationAccount) = mapped {
        val user = matchingUser(account)
        if (!user.isEmailVerified || !account.verified) changedAccount()
        firestore.collection("users").document(account.id)
            .set(mapOf("isEmailVerified" to true), SetOptions.merge()).awaitAuth()
        matchingUser(account)
        Unit
    }

    private fun matchingUser(account: VerificationAccount): FirebaseUser {
        val current = auth.currentUser
        if (current == null || current.uid != account.id || current.email.orEmpty() != account.email) changedAccount()
        return current
    }
    private fun changedAccount(): Nothing = throw EmailVerificationException(EmailVerificationError.ACCOUNT_CHANGED)
    private suspend fun <T> mapped(block: suspend () -> T): T = try { block() }
    catch (e: CancellationException) { throw e }
    catch (e: AuthNetworkTimeout) { throw EmailVerificationException(EmailVerificationError.NETWORK) }
    catch (e: FirebaseNetworkException) { throw EmailVerificationException(EmailVerificationError.NETWORK) }
    catch (e: FirebaseTooManyRequestsException) { throw EmailVerificationException(EmailVerificationError.RATE_LIMIT) }
}
