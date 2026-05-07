package com.vorsaciew.app.core.di

import com.vorsaciew.app.data.repository.AuthRepository
import com.vorsaciew.app.data.repository.AuthRepositoryImpl
import com.vorsaciew.app.data.repository.ConvoyRepository
import com.vorsaciew.app.data.repository.ConvoyRepositoryImpl
import com.vorsaciew.app.data.repository.ChatRepository
import com.vorsaciew.app.data.repository.ChatRepositoryImpl
import com.vorsaciew.app.data.repository.ClubRepository
import com.vorsaciew.app.data.repository.ClubRepositoryImpl
import com.vorsaciew.app.data.repository.EventRepository
import com.vorsaciew.app.data.repository.EventRepositoryImpl
import com.vorsaciew.app.data.repository.LocationRepository
import com.vorsaciew.app.data.repository.LocationRepositoryImpl
import com.vorsaciew.app.data.repository.PostRepository
import com.vorsaciew.app.data.repository.PostRepositoryImpl
import com.vorsaciew.app.data.repository.RallyRepository
import com.vorsaciew.app.data.repository.RallyRepositoryImpl
import com.vorsaciew.app.data.repository.UserRepository
import com.vorsaciew.app.data.repository.UserRepositoryImpl
import com.vorsaciew.app.data.repository.VehicleRepository
import com.vorsaciew.app.data.repository.VehicleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindVehicleRepository(impl: VehicleRepositoryImpl): VehicleRepository

    @Binds @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds @Singleton
    abstract fun bindRallyRepository(impl: RallyRepositoryImpl): RallyRepository

    @Binds @Singleton
    abstract fun bindClubRepository(impl: ClubRepositoryImpl): ClubRepository

    @Binds @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository

    @Binds @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds @Singleton
    abstract fun bindConvoyRepository(impl: ConvoyRepositoryImpl): ConvoyRepository
}
