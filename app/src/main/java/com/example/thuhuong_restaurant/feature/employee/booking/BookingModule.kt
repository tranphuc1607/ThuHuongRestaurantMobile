package com.example.thuhuong_restaurant.feature.employee.booking

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BookingModule {

    @Provides
    @Singleton
    fun provideBookingsApi(retrofit: Retrofit): BookingsApi = retrofit.create(BookingsApi::class.java)
}
