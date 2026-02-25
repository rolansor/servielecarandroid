package com.example.serviaux.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.serviaux.data.dao.*
import com.example.serviaux.data.entity.*
import com.example.serviaux.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        CatalogService::class
    ],
    version = 6,
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

        fun getInstance(context: Context): ServiauxDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ServiauxDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ServiauxDatabase::class.java,
                "serviaux_database"
            )
                .fallbackToDestructiveMigration()
                .addCallback(SeedCallback())
                .build()
        }
    }

    private class SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedDatabase(database)
                }
            }
        }

        private suspend fun seedDatabase(database: ServiauxDatabase) {
            val userDao = database.userDao()

            val hashedPassword = SecurityUtils.hashPassword("f4d3s2a1")

            // ── Users ──────────────────────────────────────────────
            userDao.insert(
                User(
                    name = "Andrés Sornoza",
                    username = "servielecar",
                    role = UserRole.ADMIN,
                    passwordHash = hashedPassword,
                    active = true
                )
            )

            userDao.insert(
                User(
                    name = "Nicole Plaza",
                    username = "nicopla",
                    role = UserRole.RECEPCIONISTA,
                    passwordHash = hashedPassword
                )
            )

            userDao.insert(
                User(
                    name = "Smith Mosquera",
                    username = "smimos",
                    role = UserRole.MECANICO,
                    passwordHash = hashedPassword
                )
            )

            userDao.insert(
                User(
                    name = "Ronny Mosquera",
                    username = "marciano",
                    role = UserRole.MECANICO,
                    passwordHash = hashedPassword
                )
            )

            // ── Catalog: Brands & Models ─────────────────────────────
            val catalogDao = database.catalogDao()

            val brandsWithModels = mapOf(
                "Chevrolet" to listOf("Sail","Aveo","Onix","Groove","Tracker","Captiva","D-Max","Colorado"),
                "Kia" to listOf("Soluto","Rio","Cerato","Picanto","Sonet","Seltos","Sportage","Sorento"),
                "Hyundai" to listOf("Grand i10","Accent","Elantra","Tucson","Creta","Santa Fe","Kona"),
                "Toyota" to listOf("Yaris","Corolla","Raize","Rush","Fortuner","Hilux","Land Cruiser Prado"),
                "Nissan" to listOf("Versa","Sentra","Kicks","X-Trail","Frontier"),
                "Suzuki" to listOf("Alto","Swift","Dzire","Vitara","S-Cross","Jimny"),
                "Renault" to listOf("Kwid","Logan","Sandero","Duster","Koleos"),
                "Chery" to listOf("Tiggo 2","Tiggo 4","Tiggo 7","Tiggo 8","Arrizo 5"),
                "GWM" to listOf("Wingle 5","Poer","Jolion","Haval H6"),
                "JAC" to listOf("J3","J4","S2","S3","T6"),
                "Honda" to listOf("Civic","CR-V","HR-V","City","Accord","Pilot"),
                "Mitsubishi" to listOf("L200","Montero Sport","Outlander","Eclipse Cross","ASX")
            )

            for ((brandName, models) in brandsWithModels) {
                val brandId = catalogDao.insertBrand(CatalogBrand(name = brandName))
                for (modelName in models) {
                    catalogDao.insertModel(CatalogModel(brandId = brandId, name = modelName))
                }
            }

            // ── Catalog: Colors ──────────────────────────────────────
            val colors = listOf(
                // Primarios y secundarios
                "Rojo", "Azul", "Amarillo", "Verde", "Naranja", "Morado", "Rosa", "Marrón",
                // Neutros
                "Blanco", "Negro", "Gris", "Beige", "Crema", "Marfil",
                // Variaciones comunes
                "Celeste", "Azul Oscuro", "Verde Claro", "Verde Oscuro",
                "Turquesa", "Cian", "Magenta", "Violeta",
                // Otros conocidos
                "Dorado", "Plateado", "Azul Marino", "Lima", "Oliva"
            )

            for (colorName in colors) {
                catalogDao.insertColor(CatalogColor(name = colorName))
            }

            // ── Catalog: Part Brands ───────────────────────────────────
            val partBrands = listOf(
                "Bosch", "Denso", "NGK", "Monroe", "KYB", "Gates", "SKF",
                "Valeo", "Hella", "Brembo", "TRW", "Dayco", "Continental",
                "Mann-Filter", "Mahle", "ACDelco", "Motorcraft", "Mopar",
                "Delphi", "Aisin", "NTN", "Koyo", "Timken", "Wagner",
                "Champion", "Febi Bilstein", "Sachs", "LuK", "INA",
                "Fram", "Wix", "Castrol", "Mobil", "Shell", "Total"
            )

            for (brandName in partBrands) {
                catalogDao.insertPartBrand(CatalogPartBrand(name = brandName))
            }

            // ── Catalog: Predefined Services ──────────────────────────
            val servicesMap = mapOf(
                "Mantenimiento Preventivo" to listOf(
                    "Cambio de aceite y filtro",
                    "Cambio de filtro de aire",
                    "Cambio de filtro de gasolina",
                    "Revisión y cambio de bujías",
                    "Revisión de niveles (refrigerante, frenos, dirección hidráulica)",
                    "Rotación de llantas",
                    "Balanceo y alineación",
                    "Cambio de batería",
                    "Revisión general de luces"
                ),
                "Sistema de Frenos" to listOf(
                    "Cambio de pastillas de freno",
                    "Rectificación o cambio de discos",
                    "Cambio de líquido de frenos",
                    "Revisión de frenos delanteros y traseros",
                    "Ajuste de freno de mano"
                ),
                "Motor y Sistema de Combustible" to listOf(
                    "Afinación de motor",
                    "Limpieza de inyectores",
                    "Limpieza de cuerpo de aceleración",
                    "Diagnóstico con escáner",
                    "Cambio de correas",
                    "Cambio de bomba de agua",
                    "Limpieza o cambio de radiador"
                ),
                "Suspensión y Dirección" to listOf(
                    "Cambio de amortiguadores",
                    "Cambio de rótulas",
                    "Cambio de terminales de dirección",
                    "Revisión de cremallera",
                    "Reparación de dirección hidráulica"
                ),
                "Sistema Eléctrico y Aire Acondicionado" to listOf(
                    "Recarga de aire acondicionado",
                    "Reparación de alternador",
                    "Reparación de motor de arranque",
                    "Revisión de sistema eléctrico",
                    "Cambio de fusibles y relés"
                ),
                "Otros Servicios" to listOf(
                    "Cambio de embrague",
                    "Reparación de transmisión",
                    "Enderezado de chasis",
                    "Pintura automotriz",
                    "Lavado de inyectores",
                    "Escaneo y borrado de códigos",
                    "Revisión pre-compra de vehículo"
                )
            )

            for ((category, services) in servicesMap) {
                for (serviceName in services) {
                    catalogDao.insertService(
                        CatalogService(
                            category = category,
                            name = serviceName,
                            defaultPrice = 10.0
                        )
                    )
                }
            }
        }
    }
}
