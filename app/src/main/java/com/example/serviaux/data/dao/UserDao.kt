package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Query("SELECT * FROM users WHERE username = :username AND active = 1 LIMIT 1")
    suspend fun getByUsername(username: String): User?

    @Query("SELECT * FROM users ORDER BY name")
    fun getAll(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getById(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getByIdDirect(id: Long): User?

    @Query("SELECT * FROM users WHERE role = :role AND active = 1")
    fun getByRole(role: UserRole): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllDirect(): List<User>
}
