/**
 * CustomerFormScreen.kt - Formulario de creación/edición de clientes.
 *
 * Campos: nombre, tipo de documento, cédula, teléfono, email, dirección, notas.
 * Si recibe un [customerId], carga los datos existentes para edición.
 * Al guardar exitosamente, navega de regreso a la pantalla anterior.
 */
package com.example.serviaux.ui.customers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    customerId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CustomerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditing = customerId != null

    LaunchedEffect(customerId) {
        if (customerId != null) {
            viewModel.loadAndPrepareEdit(customerId)
        } else {
            viewModel.prepareNewCustomer()
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onSaved()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Cliente" else "Nuevo Cliente") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.formFullName,
                onValueChange = { viewModel.onFormFullNameChange(it) },
                label = { Text("Nombre completo *") },
                singleLine = true,
                isError = uiState.formNameError != null,
                supportingText = uiState.formNameError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("name") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.formPhone,
                onValueChange = { viewModel.onFormPhoneChange(it) },
                label = { Text("Tel\u00e9fono *") },
                singleLine = true,
                isError = uiState.formPhoneError != null,
                supportingText = uiState.formPhoneError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("phone") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Document type + number
            val docTypeLabels = mapOf(
                "CEDULA" to "C\u00e9dula",
                "PASAPORTE" to "Pasaporte",
                "EXTRANJERIA" to "Doc. Extranjer\u00eda",
                "VISA" to "Visa",
                "OTRO" to "Otro"
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                // Doc type dropdown
                var docTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = docTypeExpanded,
                    onExpandedChange = { docTypeExpanded = it },
                    modifier = Modifier.weight(0.45f)
                ) {
                    OutlinedTextField(
                        value = docTypeLabels[uiState.formDocType] ?: uiState.formDocType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo Doc.") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docTypeExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = docTypeExpanded,
                        onDismissRequest = { docTypeExpanded = false }
                    ) {
                        CustomerViewModel.DOC_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(docTypeLabels[type] ?: type) },
                                onClick = {
                                    viewModel.onFormDocTypeChange(type)
                                    docTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Document number
                OutlinedTextField(
                    value = uiState.formIdNumber,
                    onValueChange = { viewModel.onFormIdNumberChange(it) },
                    label = { Text("N\u00famero") },
                    placeholder = {
                        Text(
                            when (uiState.formDocType) {
                                "CEDULA" -> "10 d\u00edgitos"
                                "PASAPORTE" -> "N\u00famero"
                                else -> "Documento"
                            }
                        )
                    },
                    singleLine = true,
                    isError = uiState.formIdNumberError != null,
                    supportingText = uiState.formIdNumberError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (uiState.formDocType == "CEDULA") KeyboardType.Number else KeyboardType.Text,
                        capitalization = if (uiState.formDocType != "CEDULA") KeyboardCapitalization.Characters else KeyboardCapitalization.None
                    ),
                    modifier = Modifier
                        .weight(0.55f)
                        .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("idNumber") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Email con sugerencias de dominio
            var emailExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = emailExpanded && uiState.emailSuggestions.isNotEmpty(),
                onExpandedChange = { }
            ) {
                OutlinedTextField(
                    value = uiState.formEmail,
                    onValueChange = {
                        viewModel.onFormEmailChange(it)
                        emailExpanded = true
                    },
                    label = { Text("Email") },
                    placeholder = { Text("Opcional") },
                    singleLine = true,
                    isError = uiState.formEmailError != null,
                    supportingText = uiState.formEmailError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .onFocusChanged {
                            if (!it.isFocused) {
                                emailExpanded = false
                                viewModel.validateFieldOnFocusLost("email")
                            }
                        }
                )
                ExposedDropdownMenu(
                    expanded = emailExpanded && uiState.emailSuggestions.isNotEmpty(),
                    onDismissRequest = { emailExpanded = false }
                ) {
                    uiState.emailSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                viewModel.onEmailSuggestionSelected(suggestion)
                                emailExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.formAddress,
                onValueChange = { viewModel.onFormAddressChange(it) },
                label = { Text("Direcci\u00f3n") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.formNotes,
                onValueChange = { viewModel.onFormNotesChange(it) },
                label = { Text("Notas") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveCustomer() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}
