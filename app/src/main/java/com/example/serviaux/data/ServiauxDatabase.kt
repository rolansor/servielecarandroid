/**
 * ServiauxDatabase.kt - Base de datos Room de la aplicación.
 *
 * Configuración central de la base de datos, incluyendo:
 * - Declaración de las 21 entidades del sistema.
 * - Carga inicial de datos semilla desde `assets/seed/seed_data.sql` (admin + catálogos).
 * - Carga opcional de datos de ejemplo desde `assets/seed/sample_data.sql`.
 * - Patrón Singleton thread-safe para la instancia de la BD.
 *
 * La BD se almacena como `serviaux` en el almacenamiento interno de la app.
 */
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
        Appointment::class,
        WorkOrderExtra::class
    ],
    version = 1,
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
    abstract fun workOrderMechanicDao(): WorkOrderMechanicDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun workOrderExtraDao(): WorkOrderExtraDao

    companion object {
        @Volatile
        private var INSTANCE: ServiauxDatabase? = null
        private lateinit var appContext: Context

        private const val PREFS_NAME = "serviaux_prefs"
        private const val KEY_NEEDS_SAMPLE_PROMPT = "needs_sample_prompt"

        fun getInstance(context: Context): ServiauxDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        fun needsSamplePrompt(context: Context): Boolean {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_NEEDS_SAMPLE_PROMPT, false)
        }

        fun clearSamplePrompt(context: Context) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_NEEDS_SAMPLE_PROMPT, false).apply()
        }

        fun loadSampleData(context: Context, db: ServiauxDatabase) {
            try {
                val sqlDb = db.openHelper.writableDatabase
                executeSqlFile(context, sqlDb, "seed/sample_data.sql")
                Log.i("ServiauxDatabase", "Sample data loaded successfully")
            } catch (e: Exception) {
                Log.e("ServiauxDatabase", "Error loading sample data", e)
            }
        }

        private fun executeSqlFile(context: Context, db: SupportSQLiteDatabase, assetPath: String) {
            val inputStream = context.assets.open(assetPath)
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
        }

        private fun buildDatabase(context: Context): ServiauxDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ServiauxDatabase::class.java,
                "serviaux"
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(SeedCallback(context.applicationContext))
                .build()
        }
    }

    private class SeedCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            try {
                executeSqlFile(context, db, "seed/seed_data.sql")
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_NEEDS_SAMPLE_PROMPT, true)
                    .apply()
                Log.i("ServiauxDatabase", "Seed data loaded successfully")
            } catch (e: Exception) {
                Log.e("ServiauxDatabase", "Error loading seed data", e)
            }
        }
    }
}
