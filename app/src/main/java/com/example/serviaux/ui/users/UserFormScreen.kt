/**
 * UserFormScreen.kt - Formulario de creación/edición de usuarios.
 *
 * Campos: nombre, username, rol (dropdown), contraseña, estado activo.
 * En modo edición permite cambiar contraseña y activar/desactivar usuario.
 * Solo accesible para administradores.
 */
package com.example.serviaux.ui.users

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.CommissionType
import com.example.serviaux.data.entity.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    userId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: UserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditing = userId != null

    // If editing, the UserListScreen already called prepareEdit
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadAndPrepareEdit(userId)
        } else {
            viewModel.prepareNew()
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

    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var commissionTypeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Usuario" else "Nuevo Usuario") },
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
                value = uiState.formName,
                onValueChange = { viewModel.onFormNameChange(it) },
                label = { Text("Nombre completo *") },
                singleLine = true,
                isError = uiState.formNameError != null,
                supportingText = uiState.formNameError?.let { error -> { Text(error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("name") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.formUsername,
                onValueChange = { viewModel.onFormUsernameChange(it) },
                label = { Text("Usuario *") },
                singleLine = true,
                isError = uiState.formUsernameError != null,
                supportingText = uiState.formUsernameError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("username") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Role dropdown
            ExposedDropdownMenuBox(
                expanded = roleDropdownExpanded,
                onExpandedChange = { roleDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.formRole.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rol") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = roleDropdownExpanded,
                    onDismissRequest = { roleDropdownExpanded = false }
                ) {
                    UserRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.displayName) },
                            onClick = {
                                viewModel.onFormRoleChange(role)
                                roleDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Commission fields (only for MECANICO role)
            if (uiState.formRole == UserRole.MECANICO) {
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = commissionTypeExpanded,
                    onExpandedChange = { commissionTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.formCommissionType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Comisi\u00f3n") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = commissionTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = commissionTypeExpanded,
                        onDismissRequest = { commissionTypeExpanded = false }
                    ) {
                        CommissionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    viewModel.onFormCommissionTypeChange(type)
                                    commissionTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                if (uiState.formCommissionType != CommissionType.NINGUNA) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.formCommissionValue,
                        onValueChange = { viewModel.onFormCommissionValueChange(it) },
                        label = { Text(if (uiState.formCommissionType == CommissionType.FIJA) "Valor por trabajo ($)" else "Porcentaje (%)") },
                        singleLine = true,
                        isError = uiState.formCommissionValueError != null,
                        supportingText = uiState.formCommissionValueError?.let { error -> { Text(error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("commissionValue") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isEditing) {
                OutlinedTextField(
                    value = uiState.formPassword,
                    onValueChange = { viewModel.onFormPasswordChange(it) },
                    label = { Text("Contrase\u00f1a *") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = uiState.formPasswordError != null,
                    supportingText = uiState.formPasswordError?.let { error -> { Text(error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("password") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveUser() },
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

            // Reset password section (only when editing)
            if (isEditing && userId != null) {
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Restablecer Contrase\u00f1a",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.newPassword,
                    onValueChange = { viewModel.onNewPasswordChange(it) },
                    label = { Text("Nueva contrase\u00f1a") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = uiState.newPasswordError != null,
                    supportingText = uiState.newPasswordError?.let { error -> { Text(error) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.resetPassword(userId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restablecer Contrase\u00f1a")
                }
            }
        }
    }
}
