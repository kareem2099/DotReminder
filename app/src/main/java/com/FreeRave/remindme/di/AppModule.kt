package com.FreeRave.remindme.di

import android.content.Context
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.FacebookSignInOnlyService
import com.FreeRave.remindme.service.FacebookSignInOnlyServiceImpl
import com.FreeRave.remindme.service.FirebaseAuthServiceImpl
import com.FreeRave.remindme.service.GeofenceService
import com.FreeRave.remindme.service.GoogleAuthServiceInterface // Added for new Google Auth Service
import com.FreeRave.remindme.service.GoogleAuthServiceImpl // Added for new Google Auth Service
import com.FreeRave.remindme.service.GoogleSignInOnlyService
import com.FreeRave.remindme.service.GoogleSignInOnlyServiceImpl
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.service.FirebaseService // Added import
import com.FreeRave.remindme.service.FirebaseVerificationServiceImpl
import com.FreeRave.remindme.service.VerificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuthService(firebaseService: FirebaseService, firebaseAuth: FirebaseAuth): FirebaseAuthServiceInterface {
        return FirebaseAuthServiceImpl(firebaseService, firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideGoogleAuthService(firebaseAuth: FirebaseAuth): GoogleAuthServiceInterface { // New provider for GoogleAuthService
        return GoogleAuthServiceImpl(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideGoogleSignInOnlyService(): GoogleSignInOnlyService {
        return GoogleSignInOnlyServiceImpl()
    }

    @Provides
    @Singleton
    fun provideReminderService(db: FirebaseFirestore, geofenceService: GeofenceService): ReminderService {
        return ReminderService(db, geofenceService)
    }

   @Provides
   @Singleton
   fun provideGeofenceService(@ApplicationContext context: Context): GeofenceService {
       return GeofenceService(context)
   }

    @Provides
    @Singleton
    fun provideVerificationService(firebaseAuth: FirebaseAuth): VerificationService {
        return FirebaseVerificationServiceImpl(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideFacebookSignInOnlyService(): FacebookSignInOnlyService {
        return FacebookSignInOnlyServiceImpl()
    }
}
