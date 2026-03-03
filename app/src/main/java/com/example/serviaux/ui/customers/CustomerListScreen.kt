/**
 * CustomerListScreen.kt - Pantalla de lista de clientes.
 *
 * Muestra todos los clientes con búsqueda por nombre, teléfono o cédula.
 * Permite navegar al detalle de un cliente o crear uno nuevo mediante FAB.
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.serviaux.data.entity.Customer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.ui.components.EmptyState
import com.example.serviaux.ui.components.ServiauxSearchBar
import com.example.serviaux.ui.components.ShimmerLoadingList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: (Long?) -> Unit,
    viewModel: CustomerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    // Delete confirmation dialog
    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Confirmar eliminación") },
            text = {
                Text("¿Está seguro de eliminar al cliente ${customer.fullName}${customer.idNumber?.let { " (ID: $it)" } ?: ""}?\n\nEsta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        customerToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Clientes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.prepareNewCustomer()
                onNavigateToForm(null)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo cliente")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ServiauxSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                placeholder = "Buscar cliente...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (!uiState.isListLoaded) {
                ShimmerLoadingList()
            } else if (uiState.customers.isEmpty()) {
                EmptyState(
                    message = "No se encontraron clientes",
                    icon = Icons.Default.People
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.customers, key = { it.id }) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onNavigateToDetail(customer.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    customer.idNumber?.let { id ->
                                        Text(
                                            text = "ID: $id",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onNavigateToForm(customer.id) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { customerToDelete = customer }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
