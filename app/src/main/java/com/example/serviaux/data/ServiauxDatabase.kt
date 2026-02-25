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
    version = 5,
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
            val customerDao = database.customerDao()
            val vehicleDao = database.vehicleDao()
            val workOrderDao = database.workOrderDao()
            val serviceLineDao = database.serviceLineDao()
            val partDao = database.partDao()
            val workOrderPartDao = database.workOrderPartDao()
            val statusLogDao = database.workOrderStatusLogDao()

            val hashedPassword = SecurityUtils.hashPassword("f4d3s2a1")

            // ── Users ──────────────────────────────────────────────
            val adminId = userDao.insert(
                User(
                    name = "Andrés Sornoza",
                    username = "servielecar",
                    role = UserRole.ADMIN,
                    passwordHash = hashedPassword,
                    active = true
                )
            )

            val receptionistId = userDao.insert(
                User(
                    name = "Nicole Plaza",
                    username = "nicopla",
                    role = UserRole.RECEPCIONISTA,
                    passwordHash = hashedPassword
                )
            )

            val mechanic1Id = userDao.insert(
                User(
                    name = "Smith Mosquera",
                    username = "smimos",
                    role = UserRole.MECANICO,
                    passwordHash = hashedPassword
                )
            )

            val mechanic2Id = userDao.insert(
                User(
                    name = "Ronny Mosquera",
                    username = "marciano",
                    role = UserRole.MECANICO,
                    passwordHash = hashedPassword
                )
            )

            // ── Customers ──────────────────────────────────────────
            val customer1Id = customerDao.insert(
                Customer(
                    fullName = "Roberto S\u00e1nchez",
                    idNumber = "1712345678",
                    phone = "0984512345",
                    email = "rsanchez@gmail.com"
                )
            )

            val customer2Id = customerDao.insert(
                Customer(
                    fullName = "Ana Mar\u00eda Vargas",
                    idNumber = "0923456789",
                    phone = "0997123456",
                    email = "amvargas@hotmail.com"
                )
            )

            val customer3Id = customerDao.insert(
                Customer(
                    fullName = "Luis Fernando Mora",
                    idNumber = "1803456712",
                    phone = "0961234567"
                )
            )

            val customer4Id = customerDao.insert(
                Customer(
                    fullName = "Patricia Jim\u00e9nez Sol\u00eds",
                    idNumber = "0109876543",
                    phone = "0728345678",
                    email = "pjimenez@outlook.com"
                )
            )

            val customer5Id = customerDao.insert(
                Customer(
                    fullName = "Transportes del Valle S.A.",
                    idNumber = "1790456789",
                    phone = "022345678",
                    email = "info@transvalle.ec",
                    notes = "Flotilla de 12 veh\u00edculos"
                )
            )

            // ── Vehicles ───────────────────────────────────────────
            val vehicleHiluxId = vehicleDao.insert(
                Vehicle(
                    customerId = customer1Id,
                    plate = "BCD-123",
                    brand = "Toyota",
                    model = "Hilux",
                    version = "SR 4x4",
                    year = 2019,
                    color = "Blanco",
                    currentMileage = 85000,
                    vin = "JTFDT4GN0KJ012345",
                    engineDisplacement = "2.8",
                    drivetrain = "4x4",
                    transmission = "Manual"
                )
            )

            val vehicleTucsonId = vehicleDao.insert(
                Vehicle(
                    customerId = customer2Id,
                    plate = "CDE-456",
                    brand = "Hyundai",
                    model = "Tucson",
                    version = "GLS",
                    year = 2021,
                    color = "Gris",
                    currentMileage = 42000,
                    vin = "KM8J3CA46MU234567",
                    engineDisplacement = "2.0",
                    drivetrain = "4x2",
                    transmission = "Autom\u00e1tico"
                )
            )

            val vehicleFrontierId = vehicleDao.insert(
                Vehicle(
                    customerId = customer3Id,
                    plate = "EFG-789",
                    brand = "Nissan",
                    model = "Frontier",
                    version = "SE",
                    year = 2018,
                    color = "Negro",
                    currentMileage = 120000,
                    vin = "1N6AD0CW8JN345678",
                    engineDisplacement = "2.5",
                    drivetrain = "4x4",
                    transmission = "Manual"
                )
            )

            val vehicleCorollaId = vehicleDao.insert(
                Vehicle(
                    customerId = customer4Id,
                    plate = "FGH-012",
                    brand = "Toyota",
                    model = "Corolla",
                    version = "XEI",
                    year = 2020,
                    color = "Plata",
                    currentMileage = 55000,
                    vin = "JTDKN3DU7L0456789",
                    engineDisplacement = "1.8",
                    drivetrain = "4x2",
                    transmission = "Autom\u00e1tico"
                )
            )

            val vehicleL200Id = vehicleDao.insert(
                Vehicle(
                    customerId = customer5Id,
                    plate = "GHI-345",
                    brand = "Mitsubishi",
                    model = "L200",
                    version = "GLS Sport",
                    year = 2017,
                    color = "Rojo",
                    currentMileage = 145000,
                    vin = "MMBJNKL10HH567890",
                    engineDisplacement = "2.4",
                    drivetrain = "4x4",
                    transmission = "Manual"
                )
            )

            val vehicleJimnyId = vehicleDao.insert(
                Vehicle(
                    customerId = customer2Id,
                    plate = "HIJ-678",
                    brand = "Suzuki",
                    model = "Jimny",
                    version = "GLX",
                    year = 2022,
                    color = "Verde",
                    currentMileage = 28000,
                    vin = "JS3JB5V35N4678901",
                    engineDisplacement = "1.5",
                    drivetrain = "4x4",
                    transmission = "Autom\u00e1tico"
                )
            )

            val vehicleSportageId = vehicleDao.insert(
                Vehicle(
                    customerId = customer5Id,
                    plate = "IJK-901",
                    brand = "Kia",
                    model = "Sportage",
                    version = "EX 2.0",
                    year = 2020,
                    color = "Azul",
                    currentMileage = 67000,
                    vin = "KNDPN3AC5L7789012",
                    engineDisplacement = "2.0",
                    drivetrain = "4x2",
                    transmission = "Autom\u00e1tico"
                )
            )

            val vehicleCrvId = vehicleDao.insert(
                Vehicle(
                    customerId = customer1Id,
                    plate = "JKL-234",
                    brand = "Honda",
                    model = "CR-V",
                    version = "EXL",
                    year = 2019,
                    color = "Blanco",
                    currentMileage = 78000,
                    vin = "2HKRW2H53KH890123",
                    engineDisplacement = "1.5",
                    drivetrain = "4x2",
                    transmission = "Autom\u00e1tico"
                )
            )

            // ── Parts / Repuestos ──────────────────────────────────
            val partFiltroAceiteId = partDao.insert(
                Part(
                    name = "Filtro de aceite",
                    code = "FO-TOY-001",
                    brand = "Toyota Genuine",
                    unitCost = 3500.0,
                    salePrice = 5500.0,
                    currentStock = 25
                )
            )

            val partFiltroAireId = partDao.insert(
                Part(
                    name = "Filtro de aire",
                    code = "FA-UNI-001",
                    brand = "Fram",
                    unitCost = 4200.0,
                    salePrice = 7000.0,
                    currentStock = 18
                )
            )

            val partPastillasDelId = partDao.insert(
                Part(
                    name = "Pastillas de freno delanteras",
                    code = "PF-BOS-001",
                    brand = "Bosch",
                    unitCost = 12000.0,
                    salePrice = 18500.0,
                    currentStock = 12
                )
            )

            val partPastillasTrasId = partDao.insert(
                Part(
                    name = "Pastillas de freno traseras",
                    code = "PF-BOS-002",
                    brand = "Bosch",
                    unitCost = 10000.0,
                    salePrice = 16000.0,
                    currentStock = 10
                )
            )

            val partAceite5w30Id = partDao.insert(
                Part(
                    name = "Aceite 5W-30 sintético (litro)",
                    code = "AC-MOB-530",
                    brand = "Mobil 1",
                    unitCost = 5500.0,
                    salePrice = 8000.0,
                    currentStock = 40
                )
            )

            partDao.insert(
                Part(
                    name = "Aceite 10W-40 semi-sintético (litro)",
                    code = "AC-CAS-1040",
                    brand = "Castrol",
                    unitCost = 3800.0,
                    salePrice = 6000.0,
                    currentStock = 35
                )
            )

            partDao.insert(
                Part(
                    name = "Bujía de iridio",
                    code = "BJ-NGK-001",
                    brand = "NGK",
                    unitCost = 4500.0,
                    salePrice = 7500.0,
                    currentStock = 32
                )
            )

            partDao.insert(
                Part(
                    name = "Batería 12V 60Ah",
                    code = "BA-BOS-060",
                    brand = "Bosch",
                    unitCost = 35000.0,
                    salePrice = 52000.0,
                    currentStock = 6
                )
            )

            val partAmortiguadorId = partDao.insert(
                Part(
                    name = "Amortiguador delantero",
                    code = "AM-KYB-001",
                    brand = "KYB",
                    unitCost = 22000.0,
                    salePrice = 35000.0,
                    currentStock = 8
                )
            )

            partDao.insert(
                Part(
                    name = "Correa de distribución",
                    code = "CD-GAT-001",
                    brand = "Gates",
                    unitCost = 8500.0,
                    salePrice = 14000.0,
                    currentStock = 5
                )
            )

            partDao.insert(
                Part(
                    name = "Bomba de agua",
                    code = "BA-GMB-001",
                    brand = "GMB",
                    unitCost = 15000.0,
                    salePrice = 24000.0,
                    currentStock = 4
                )
            )

            partDao.insert(
                Part(
                    name = "Disco de freno delantero",
                    code = "DF-BRE-001",
                    brand = "Brembo",
                    unitCost = 18000.0,
                    salePrice = 28000.0,
                    currentStock = 6
                )
            )

            partDao.insert(
                Part(
                    name = "Líquido de frenos DOT 4",
                    code = "LF-WAG-004",
                    brand = "Wagner",
                    unitCost = 3200.0,
                    salePrice = 5500.0,
                    currentStock = 15
                )
            )

            partDao.insert(
                Part(
                    name = "Refrigerante/Anticongelante (galón)",
                    code = "RF-PRE-001",
                    brand = "Prestone",
                    unitCost = 6000.0,
                    salePrice = 9500.0,
                    currentStock = 10
                )
            )

            partDao.insert(
                Part(
                    name = "Kit de embrague completo",
                    code = "KE-LUK-001",
                    brand = "LUK",
                    unitCost = 45000.0,
                    salePrice = 68000.0,
                    currentStock = 3
                )
            )

            // ── Work Order 1: Toyota Hilux – Servicio Mayor (ENTREGADO) ─
            val wo1Id = workOrderDao.insert(
                WorkOrder(
                    vehicleId = vehicleHiluxId,
                    customerId = customer1Id,
                    status = OrderStatus.ENTREGADO,
                    priority = Priority.ALTA,
                    customerComplaint = "Servicio Mayor 80,000 km – cambio de aceite, revisión completa de frenos, alineación y balanceo",
                    initialDiagnosis = "Vehículo requiere servicio de mantenimiento mayor. Pastillas de freno con 30% de vida útil, aceite degradado.",
                    assignedMechanicId = mechanic1Id,
                    entryMileage = 80000,
                    fuelLevel = "3/4",
                    totalLabor = 35000.0,
                    totalParts = 50000.0,
                    total = 85000.0,
                    createdBy = receptionistId,
                    updatedBy = mechanic1Id
                )
            )

            // Service lines for WO1
            serviceLineDao.insert(
                ServiceLine(
                    workOrderId = wo1Id,
                    description = "Cambio de aceite y filtro",

                    laborCost = 8000.0
                )
            )
            serviceLineDao.insert(
                ServiceLine(
                    workOrderId = wo1Id,
                    description = "Revisión de frenos",

                    laborCost = 12000.0
                )
            )
            serviceLineDao.insert(
                ServiceLine(
                    workOrderId = wo1Id,
                    description = "Alineación y balanceo",

                    laborCost = 10000.0
                )
            )
            serviceLineDao.insert(
                ServiceLine(
                    workOrderId = wo1Id,
                    description = "Rotación de llantas",

                    laborCost = 5000.0
                )
            )

            // Parts used in WO1
            workOrderPartDao.insert(
                WorkOrderPart(
                    workOrderId = wo1Id,
                    partId = partFiltroAceiteId,
                    quantity = 1,
                    appliedUnitPrice = 5500.0,
                    subtotal = 5500.0
                )
            )
            workOrderPartDao.insert(
                WorkOrderPart(
                    workOrderId = wo1Id,
                    partId = partFiltroAireId,
                    quantity = 1,
                    appliedUnitPrice = 7000.0,
                    subtotal = 7000.0
                )
            )
            workOrderPartDao.insert(
                WorkOrderPart(
                    workOrderId = wo1Id,
                    partId = partAceite5w30Id,
                    quantity = 5,
                    appliedUnitPrice = 8000.0,
                    subtotal = 40000.0
                )
            )

            // Status log for WO1
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo1Id,
                    oldStatus = null,
                    newStatus = OrderStatus.RECIBIDO,
                    changedByUserId = receptionistId,
                    note = "Orden creada – Servicio Mayor 80,000 km"
                )
            )
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo1Id,
                    oldStatus = OrderStatus.RECIBIDO,
                    newStatus = OrderStatus.EN_DIAGNOSTICO,
                    changedByUserId = mechanic1Id,
                    note = "Iniciando inspección del vehículo"
                )
            )
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo1Id,
                    oldStatus = OrderStatus.EN_DIAGNOSTICO,
                    newStatus = OrderStatus.EN_PROCESO,
                    changedByUserId = mechanic1Id,
                    note = "Diagnóstico completo, iniciando servicio"
                )
            )
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo1Id,
                    oldStatus = OrderStatus.EN_PROCESO,
                    newStatus = OrderStatus.LISTO,
                    changedByUserId = mechanic1Id,
                    note = "Servicio completado, listo para entrega"
                )
            )
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo1Id,
                    oldStatus = OrderStatus.LISTO,
                    newStatus = OrderStatus.ENTREGADO,
                    changedByUserId = receptionistId,
                    note = "Vehículo entregado al cliente"
                )
            )

            // ── Work Order 2: Hyundai Tucson – Diagnóstico suspensión (EN_PROCESO) ─
            val wo2Id = workOrderDao.insert(
                WorkOrder(
                    vehicleId = vehicleTucsonId,
                    customerId = customer2Id,
                    status = OrderStatus.EN_PROCESO,
                    priority = Priority.MEDIA,
                    customerComplaint = "Ruido en la suspensión delantera al pasar por baches. Más pronunciado del lado derecho.",
                    initialDiagnosis = "Amortiguadores delanteros desgastados, se recomienda reemplazo de ambos.",
                    assignedMechanicId = mechanic1Id,
                    entryMileage = 42000,
                    fuelLevel = "1/2",
                    createdBy = receptionistId,
                    updatedBy = mechanic1Id
                )
            )

            // Service lines for WO2
            serviceLineDao.insert(
                ServiceLine(
                    workOrderId = wo2Id,
                    description = "Diagnóstico computarizado",

                    laborCost = 15000.0
                )
            )
            serviceLineDao.insert(
                ServiceLine(
                    workOrderId = wo2Id,
                    description = "Revisión de amortiguadores",

                    laborCost = 8000.0
                )
            )

            // Parts used in WO2
            workOrderPartDao.insert(
                WorkOrderPart(
                    workOrderId = wo2Id,
                    partId = partAmortiguadorId,
                    quantity = 2,
                    appliedUnitPrice = 35000.0,
                    subtotal = 70000.0
                )
            )

            // Status log for WO2
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo2Id,
                    oldStatus = null,
                    newStatus = OrderStatus.RECIBIDO,
                    changedByUserId = receptionistId,
                    note = "Orden creada – cliente reporta ruido en suspensión"
                )
            )
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo2Id,
                    oldStatus = OrderStatus.RECIBIDO,
                    newStatus = OrderStatus.EN_DIAGNOSTICO,
                    changedByUserId = mechanic1Id,
                    note = "Inicia diagnóstico computarizado e inspección visual"
                )
            )
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo2Id,
                    oldStatus = OrderStatus.EN_DIAGNOSTICO,
                    newStatus = OrderStatus.EN_PROCESO,
                    changedByUserId = mechanic1Id,
                    note = "Amortiguadores confirmados como desgastados, procediendo con reemplazo"
                )
            )

            // ── Work Order 3: Nissan Frontier – Mantenimiento preventivo (RECIBIDO) ─
            val wo3Id = workOrderDao.insert(
                WorkOrder(
                    vehicleId = vehicleFrontierId,
                    customerId = customer3Id,
                    status = OrderStatus.RECIBIDO,
                    priority = Priority.BAJA,
                    customerComplaint = "Mantenimiento preventivo – cambio de aceite y revisión general",
                    assignedMechanicId = mechanic2Id,
                    entryMileage = 120000,
                    fuelLevel = "1/4",
                    createdBy = receptionistId,
                    updatedBy = receptionistId
                )
            )

            // Status log for WO3
            statusLogDao.insert(
                WorkOrderStatusLog(
                    workOrderId = wo3Id,
                    oldStatus = null,
                    newStatus = OrderStatus.RECIBIDO,
                    changedByUserId = receptionistId,
                    note = "Orden creada – mantenimiento preventivo programado"
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
