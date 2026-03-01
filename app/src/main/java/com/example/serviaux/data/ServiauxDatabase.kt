/**
 * ServiauxDatabase.kt - Base de datos Room de la aplicación.
 *
 * Configuración central de la base de datos, incluyendo:
 * - Declaración de las 18 entidades del sistema.
 * - Migraciones incrementales (v2->v3, v3->v4, v4->v5).
 * - Carga inicial de datos semilla desde `assets/seed/seed_data.sql`.
 * - Patrón Singleton thread-safe para la instancia de la BD.
 *
 * La BD se almacena como `serviaux_v1` en el almacenamiento interno de la app.
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
        CatalogOilType::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
/**
 * Base de datos principal de Serviaux.
 *
 * Expone todos los DAOs necesarios y gestiona migraciones y datos iniciales.
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

        // ── Migraciones ──────────────────────────────────────────────────

        /** v7->v8: Agrega tabla de tipos de aceite y campos oilType/oilCapacity a vehículos. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS catalog_oil_types (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                try { db.execSQL("ALTER TABLE vehicles ADD COLUMN oilType TEXT") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE vehicles ADD COLUMN oilCapacity TEXT") } catch (_: Exception) {}
                Log.i("ServiauxDatabase", "Migration 7->8 completed successfully")
            }
        }

        /** v6->v7: Agrega condición de llegada y tipo de orden a órdenes. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_orders ADD COLUMN arrivalCondition TEXT NOT NULL DEFAULT 'RODANDO'")
                db.execSQL("ALTER TABLE work_orders ADD COLUMN orderType TEXT NOT NULL DEFAULT 'SERVICIO_NUEVO'")
                Log.i("ServiauxDatabase", "Migration 6->7 completed successfully")
            }
        }

        /** v5->v6: Agrega tabla de mecánicos por orden y campos de comisión en usuarios. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS work_order_mechanics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workOrderId INTEGER NOT NULL,
                        mechanicId INTEGER NOT NULL,
                        commissionType TEXT NOT NULL,
                        commissionValue REAL NOT NULL,
                        commissionAmount REAL NOT NULL,
                        commissionPaid INTEGER NOT NULL DEFAULT 0,
                        paidAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (workOrderId) REFERENCES work_orders(id) ON DELETE CASCADE,
                        FOREIGN KEY (mechanicId) REFERENCES users(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_order_mechanics_workOrderId ON work_order_mechanics(workOrderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_order_mechanics_mechanicId ON work_order_mechanics(mechanicId)")
                try { db.execSQL("ALTER TABLE users ADD COLUMN commissionType TEXT NOT NULL DEFAULT 'NINGUNA'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE users ADD COLUMN commissionValue REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE vehicles ADD COLUMN fuelType TEXT") } catch (_: Exception) {}
                Log.i("ServiauxDatabase", "Migration 5->6 completed successfully")
            }
        }

        /** v4->v5: Agrega campo de descuento a la tabla de pagos. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_order_payments ADD COLUMN discount REAL NOT NULL DEFAULT 0.0")
                Log.i("ServiauxDatabase", "Migration 4->5 completed successfully")
            }
        }

        /** v3->v4: Agrega campos de fotos, nota de entrega, factura y notas a órdenes. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_orders ADD COLUMN filePaths TEXT")
                db.execSQL("ALTER TABLE work_orders ADD COLUMN deliveryNote TEXT")
                db.execSQL("ALTER TABLE work_orders ADD COLUMN invoiceNumber TEXT")
                db.execSQL("ALTER TABLE work_orders ADD COLUMN notes TEXT")
                Log.i("ServiauxDatabase", "Migration 3->4 completed successfully")
            }
        }

        /** v2->v3: Agrega tablas de catálogos nuevos y campo vehicleType a vehículos. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add tables that may not exist in version 2
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS catalog_vehicle_types (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS catalog_accessories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS catalog_complaints (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS catalog_diagnoses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        complaintId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        FOREIGN KEY (complaintId) REFERENCES catalog_complaints(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_diagnoses_complaintId ON catalog_diagnoses(complaintId)")

                // Add vehicleType column to vehicles if not present
                try {
                    db.execSQL("ALTER TABLE vehicles ADD COLUMN vehicleType TEXT")
                } catch (_: Exception) { /* column already exists */ }

                Log.i("ServiauxDatabase", "Migration 2->3 completed successfully")
            }
        }

        private fun buildDatabase(context: Context): ServiauxDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ServiauxDatabase::class.java,
                "serviaux_v1"
            )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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