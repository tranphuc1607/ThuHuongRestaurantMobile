package com.example.thuhuong_restaurant.feature.employee.table

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EmployeeModule {

    @Provides
    @Singleton
    fun provideTablesApi(retrofit: Retrofit): TablesApi = retrofit.create(TablesApi::class.java)

    @Provides
    @Singleton
    fun provideEmployeeOrderApi(retrofit: Retrofit): EmployeeOrderApi =
        retrofit.create(EmployeeOrderApi::class.java)
}
