/**
 * ServiauxDatabase.kt - Base de datos Room de la aplicación.
 *
 * Configuración central de la base de datos, incluyendo:
 * - Declaración de las 21 entidades del sistema.
 * - Carga inicial de datos semilla desde `assets/seed/seed_data.sql`.
 * - Patrón Singleton thread-safe para la instancia de la BD.
 *
 * La BD se almacena como `serviaux` en el almacenamiento interno de la app.
 * Version 1 — esquema definitivo, sin migraciones.
 */
package com.example.serviaux.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.serviaux.data.dao.*
import com.example.serviaux.data.entity.*
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
        CatalogDiagnosis::class,
        WorkOrderMechanic::class,
        CatalogOilType::class,
        Appointment::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
/**
 * Base de datos principal de Serviaux.
 *
 * Expone todos los DAOs necesarios y gestiona datos iniciales.
 * Se obtiene la instancia mediante [getInstance].
 */
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
    abstract fun workOrderMechanicDao(): WorkOrderMechanicDao
    abstract fun appointmentDao(): AppointmentDao

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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_lines ADD COLUMN discount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE work_order_parts ADD COLUMN discount REAL NOT NULL DEFAULT 0.0")
            }
        }

        private fun buildDatabase(context: Context): ServiauxDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ServiauxDatabase::class.java,
                "serviaux"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(SeedCallback(context.applicationContext))
                .build()
        }
    }

    /**
     * Callback que carga datos semilla al crear la BD por primera vez.
     *
     * Lee y ejecuta sentencias SQL desde `assets/seed/seed_data.sql`
     * dentro de una transacción. Incluye catálogos iniciales y el usuario admin.
     */
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