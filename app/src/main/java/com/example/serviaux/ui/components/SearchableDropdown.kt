/**
 * SearchableDropdown.kt - Componente de dropdown con autocompletado.
 *
 * Campo de texto con menú desplegable que filtra opciones mientras el usuario
 * escribe. Se usa en formularios de vehículos (cliente, marca, modelo) y
 * en la asignación de repuestos a órdenes.
 *
 * El texto ingresado se convierte a mayúsculas automáticamente.
 */
package com.example.serviaux.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/** Elemento seleccionable del dropdown con ID, nombre y subtítulo opcional. */
data class SearchableItem(
    val id: Long,
    val name: String,
    val subtitle: String? = null
)

/**
 * Dropdown con búsqueda integrada.
 *
 * @param value Texto actual del campo.
 * @param onValueChange Callback al cambiar el texto (ya en mayúsculas).
 * @param items Lista completa de opciones disponibles.
 * @param onItemSelected Callback al seleccionar un elemento del dropdown.
 * @param label Etiqueta del campo de texto.
 * @param maxSuggestions Máximo de sugerencias visibles (por defecto 3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    items: List<SearchableItem>,
    onItemSelected: (SearchableItem) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    maxSuggestions: Int = 3
) {
    var expanded by remember { mutableStateOf(false) }

    val filtered = remember(value, items) {
        if (value.isBlank()) emptyList()
        else items.filter {
            it.name.contains(value, ignoreCase = true) ||
            it.subtitle?.contains(value, ignoreCase = true) == true
        }.take(maxSuggestions)
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filtered.isNotEmpty(),
        onExpandedChange = { },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it.uppercase())
                expanded = it.isNotBlank()
            },
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            isError = isError,
            supportingText = supportingText,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = {
                        onValueChange("")
                        expanded = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(20.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filtered.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            if (item.subtitle != null) {
                                androidx.compose.foundation.layout.Column {
                                    Text(item.name)
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(item.name)
                            }
                        },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
