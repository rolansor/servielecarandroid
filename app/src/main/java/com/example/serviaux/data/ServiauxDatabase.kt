package com.example.serviaux.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.serviaux.data.dao.*
import com.example.serviaux.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Database(
    entities = [
        User::class,
        Customer::class,
        Vehicle::class,
        WorkOrder::class,
        ServiceLine::class,
        Part::class,
        WorkOrderPart::class,
        WorkOrderPayment::class,
        WorkOrderStatusLog::class,
        CatalogBrand::class,
        CatalogModel::class,
        CatalogColor::class,
        CatalogPartBrand::class,
        CatalogService::class,
        CatalogVehicleType::class,
        CatalogAccessory::class,
        CatalogComplaint::class,
        CatalogDiagnosis::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ServiauxDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun serviceLineDao(): ServiceLineDao
    abstract fun partDao(): PartDao
    abstract fun workOrderPartDao(): WorkOrderPartDao
    abstract fun workOrderPaymentDao(): WorkOrderPaymentDao
    abstract fun workOrderStatusLogDao(): WorkOrderStatusLogDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: ServiauxDatabase? = null
        private lateinit var appContext: Context

        fun getInstance(context: Context): ServiauxDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ServiauxDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ServiauxDatabase::class.java,
                "serviaux_v3"
            )
                .fallbackToDestructiveMigration()
                .addCallback(SeedCallback(context.applicationContext))
                .build()
        }
    }

    private class SeedCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            loadSeedSql(db)
        }

        private fun loadSeedSql(db: SupportSQLiteDatabase) {
            try {
                val inputStream = context.assets.open("seed/seed_data.sql")
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                db.beginTransaction()
                try {
                    val statement = StringBuilder()
                    reader.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("--")) return@forEachLine
                        statement.append(trimmed)
                        if (trimmed.endsWith(";")) {
                            db.execSQL(statement.toString())
                            statement.clear()
                        } else {
                            statement.append(" ")
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                    reader.close()
                }
                Log.i("ServiauxDatabase", "Seed data loaded successfully")
            } catch (e: Exception) {
                Log.e("ServiauxDatabase", "Error loading seed data", e)
            }
        }
    }
}