package com.example.thuhuong_restaurant.feature.employee.receiptscan

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReceiptScanModule {

    @Provides
    @Singleton
    fun provideReceiptScanApi(retrofit: Retrofit): ReceiptScanApi = retrofit.create(ReceiptScanApi::class.java)
}
