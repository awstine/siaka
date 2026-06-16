package com.siaka.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // MapboxRouteGenerator is provided via its @Inject constructor and @Singleton annotation.
    // No explicit @Provides is needed here unless complex initialization is required.
}
