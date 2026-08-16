package com.example.smarthomesimulator.di

import com.example.smarthomesimulator.data.repository.SmartHomeRepositoryImpl
import com.example.smarthomesimulator.domain.repository.SmartHomeRepository
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance(
            "https://smart-home-system-d7702-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
    }

    @Provides
    @Singleton
    fun provideSmartHomeRepository(database: FirebaseDatabase): SmartHomeRepository {
        return SmartHomeRepositoryImpl(database)
    }
}
