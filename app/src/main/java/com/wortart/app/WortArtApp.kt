package com.wortart.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.wortart.app.data.auth.AuthRepository
import com.wortart.app.data.auth.FirebaseAuthRepository
import com.wortart.app.data.auth.UnconfiguredAuthRepository
import com.wortart.app.data.local.LocalDataResetter
import com.wortart.app.data.local.WortArtDatabase
import com.wortart.app.data.membership.MembershipRepository
import com.wortart.app.data.remote.AdminRepository
import com.wortart.app.data.remote.FirestoreAdminRepository
import com.wortart.app.data.remote.FirestoreInvitationRepository
import com.wortart.app.data.remote.FirestoreUserRepository
import com.wortart.app.data.remote.InvitationRepository
import com.wortart.app.data.remote.UnconfiguredAdminRepository
import com.wortart.app.data.remote.UnconfiguredInvitationRepository
import com.wortart.app.data.remote.UnconfiguredUserRepository
import com.wortart.app.data.remote.UserRepository
import com.wortart.app.data.local.seed.VocabSeeder
import com.wortart.app.data.repository.StatsRepository
import com.wortart.app.data.repository.StatsRepositoryImpl
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.data.repository.VocabRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Registered in AndroidManifest.xml via android:name=".WortArtApp".
 *
 * Seeding runs in the background. Room Flows re-emit when the rows are inserted,
 * so screens that observe the repository update automatically on first launch.
 */
class WortArtApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: WortArtDatabase by lazy { WortArtDatabase.getInstance(this) }

    val vocabRepository: VocabRepository by lazy { VocabRepositoryImpl(database.vocabDao()) }

    val statsRepository: StatsRepository by lazy { StatsRepositoryImpl(database.userStatsDao()) }

    /** False until app/google-services.json is added (see docs/FIREBASE_SETUP.md). */
    val firebaseConfigured: Boolean by lazy { FirebaseApp.getApps(this).isNotEmpty() }

    val authRepository: AuthRepository by lazy {
        if (firebaseConfigured) FirebaseAuthRepository(this) else UnconfiguredAuthRepository
    }

    val userRepository: UserRepository by lazy {
        if (firebaseConfigured) {
            FirestoreUserRepository(FirebaseFirestore.getInstance())
        } else {
            UnconfiguredUserRepository
        }
    }

    val invitationRepository: InvitationRepository by lazy {
        if (firebaseConfigured) {
            FirestoreInvitationRepository(FirebaseFirestore.getInstance())
        } else {
            UnconfiguredInvitationRepository
        }
    }

    val adminRepository: AdminRepository by lazy {
        if (firebaseConfigured) {
            FirestoreAdminRepository(FirebaseFirestore.getInstance())
        } else {
            UnconfiguredAdminRepository
        }
    }

    val membershipRepository: MembershipRepository by lazy {
        MembershipRepository(authRepository, userRepository, appScope)
    }

    /** Clears favorites and statistics on this device and reloads the bundled words. */
    val localDataResetter: LocalDataResetter by lazy {
        LocalDataResetter(database, reseed = { VocabSeeder(this, database).seedIfNeeded() })
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            VocabSeeder(this@WortArtApp, database).seedIfNeeded()
        }
    }
}
