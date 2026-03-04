/**
 * VehicleDetailScreen.kt - Pantalla de detalle de un vehículo.
 *
 * Muestra información completa del vehículo (placa, marca, modelo, VIN, etc.),
 * galería de fotos, datos del propietario y el historial de órdenes de trabajo.
 * Permite navegar a editar el vehículo, ver el cliente o ver una orden.
 */
package com.example.serviaux.ui.vehicles

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.serviaux.ui.components.InfoRow
import com.example.serviaux.ui.components.SectionTitle
import com.example.serviaux.ui.components.StatusChip
import com.example.serviaux.util.PhotoUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToOrder: (Long) -> Unit,
    onNavigateToCustomer: (Long) -> Unit,
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es")) }

    LaunchedEffect(vehicleId) {
        viewModel.loadVehicle(vehicleId)
    }

    val vehicle = uiState.selectedVehicle

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 40.dp,
                title = { Text(vehicle?.plate ?: "Veh\u00edculo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        vehicle?.let {
                            viewModel.prepareEdit(it)
                            onNavigateToEdit(it.id)
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            )
        }
    ) { padding ->
        if (vehicle == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Informaci\u00f3n del Veh\u00edculo")
                            InfoRow(label = "Placa", value = vehicle.plate)
                            InfoRow(label = "Marca", value = vehicle.brand)
                            InfoRow(label = "Modelo", value = vehicle.model)
                            InfoRow(label = "Versi\u00f3n", value = vehicle.version ?: "N/A")
                            InfoRow(label = "A\u00f1o", value = vehicle.year?.toString() ?: "N/A")
                            InfoRow(label = "Color", value = vehicle.color ?: "N/A")
                            InfoRow(label = "Cilindraje", value = vehicle.engineDisplacement?.let { "$it L" } ?: "N/A")
                            InfoRow(label = "N\u00famero de motor", value = vehicle.engineNumber ?: "N/A")
                            InfoRow(label = "VIN", value = vehicle.vin ?: "N/A")
                            InfoRow(label = "Kilometraje", value = vehicle.currentMileage?.let { "$it km" } ?: "N/A")
                            InfoRow(label = "Tracci\u00f3n", value = vehicle.drivetrain)
                            InfoRow(label = "Transmisi\u00f3n", value = vehicle.transmission)
                            InfoRow(label = "Tipo de Veh\u00edculo", value = vehicle.vehicleType ?: "N/A")
                            InfoRow(label = "Tipo de Combustible", value = vehicle.fuelType ?: "N/A")
                            InfoRow(label = "Tipo de Aceite", value = vehicle.oilType ?: "N/A")
                            InfoRow(label = "Capacidad de Aceite", value = vehicle.oilCapacity ?: "N/A")
                        }
                    }

                    // Photos section
                    val photoPaths = PhotoUtils.parsePaths(vehicle.photoPaths)
                    if (photoPaths.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle("Fotos")
                        Spacer(modifier = Modifier.height(8.dp))
                        val detailContext = LocalContext.current
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(photoPaths) { index, path ->
                                AsyncImage(
                                    model = ImageRequest.Builder(detailContext)
                                        .data(File(path))
                                        .build(),
                                    contentDescription = "Foto ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCustomer(vehicle.customerId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Propietario",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.customerName.ifBlank { "Cliente #${vehicle.customerId}" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle("Historial de \u00d3rdenes")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.vehicleOrders.isEmpty()) {
                    item {
                        Text(
                            text = "No hay \u00f3rdenes registradas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(uiState.vehicleOrders, key = { it.id }) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onNavigateToOrder(order.id) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Orden #${order.id}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusChip(status = order.status)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormat.format(Date(order.entryDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = order.customerComplaint,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("$%.2f", order.total),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
