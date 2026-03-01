/**
 * CustomerDetailScreen.kt - Pantalla de detalle de un cliente.
 *
 * Muestra la información completa del cliente, su lista de vehículos
 * registrados y permite navegar a editar el cliente, ver un vehículo
 * o agregar un nuevo vehículo preseleccionando al cliente.
 */
package com.example.serviaux.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.ui.components.InfoRow
import com.example.serviaux.ui.components.SectionTitle
import com.example.serviaux.ui.vehicles.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToVehicle: (Long) -> Unit,
    onNavigateToNewVehicle: (Long) -> Unit,
    viewModel: CustomerViewModel = viewModel(),
    vehicleViewModel: VehicleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vehicleUiState by vehicleViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(customerId) {
        viewModel.loadCustomer(customerId)
        vehicleViewModel.loadVehiclesByCustomer(customerId)
    }

    val customer = uiState.selectedCustomer
    val customerVehicles = vehicleUiState.customerVehicles

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.fullName ?: "Cliente") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        customer?.let {
                            viewModel.prepareEditCustomer(it)
                            onNavigateToEdit(it.id)
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            )
        }
    ) { padding ->
        if (customer == null) {
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
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Informaci\u00f3n del Cliente")
                            InfoRow(label = "Nombre", value = customer.fullName)
                            InfoRow(label = "Tel\u00e9fono", value = customer.phone ?: "N/A")
                            InfoRow(label = "C\u00e9dula/RUC", value = customer.idNumber ?: "N/A")
                            InfoRow(label = "Email", value = customer.email ?: "N/A")
                            InfoRow(label = "Direcci\u00f3n", value = customer.address ?: "N/A")
                            InfoRow(label = "Notas", value = customer.notes ?: "N/A")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle("Veh\u00edculos", modifier = Modifier.weight(1f))
                        Button(onClick = { onNavigateToNewVehicle(customerId) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar Veh\u00edculo")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (customerVehicles.isEmpty()) {
                    item {
                        Text(
                            text = "No hay veh\u00edculos registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(customerVehicles, key = { it.id }) { vehicle ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onNavigateToVehicle(vehicle.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = vehicle.plate,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${vehicle.brand} ${vehicle.model}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
