package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Los diálogos del editor de notas (spec 014, RNF-43).
 *
 * Cada uno vive aquí con **parámetros con nombre** y **devuelve eventos**: quien lo pone decide
 * qué hacer con lo que se escriba, y el diálogo no sabe nada de la nota. Es la línea que ya
 * trazaron `ui/chat/` y `ui/table/`, y la razón por la que se pueden quitar del composable sin
 * inventarle un contenedor de estado.
 *
 * Hay dos cosas que este fichero **no** hace a propósito:
 *
 * - **No guarda estado.** Los campos de texto son parámetros, no `remember`. Si el diálogo guardara
 *   el texto, dejaría de reflejar lo que el editor tiene cuando lo abre, y el editor tiene que
 *   poder prefillarlos —al editar un enlace ya puesto, por ejemplo—.
 * - **No decide nada.** Si la dirección está vacía, si el texto es el mismo que la dirección, si el
 *   enlace se inserta o se sustituye al que había: eso lo decide quien lo llama, con el estado que
 *   solo el editor tiene.
 */

/**
 * Inserta o **edita** un enlace.
 *
 * Los dos casos son el mismo diálogo: se abre para escribir uno nuevo o para cambiar el que ya
 * estaba bajo el cursor, y quien lo llama distingue por lo que le llega.
 *
 * @param onConfirm recibe la dirección y el texto que se verá. Solo se llama con dirección no vacía.
 * @param onCerrar se llama al confirmar o al cancelar: limpia los campos y cierra.
 * @param onDismissRequest se llama al cerrar **tocar fuera** del diálogo.
 *
 * **Los dos caminos no hacen lo mismo, a propósito.** Confirmar y cancelar limpian los campos;
 * tocar fuera solo cierra. No es un descuido: al abrir el diálogo el editor rellena el texto con lo
 * que hay bajo el cursor, y el rango de edición lo fija otro camino que todavía puede necesitarlo si
 * el usuario cierra sin confirmar. Son dos callbacks distintos porque eso es lo que había.
 */
@Composable
fun InsertUrlDialog(
    address: String,
    text: String,
    onAddressChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onConfirm: (address: String, display: String) -> Unit,
    onCerrar: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.dialog_insert_url_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text(stringResource(id = R.string.label_url_address)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text(stringResource(id = R.string.label_url_text)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // El texto vacío no es un error: se ve la dirección, que es lo que quiere el
                    // usuario que escribe un enlace y no se molesta en ponerle otro texto.
                    if (address.isNotBlank()) onConfirm(address, text.ifEmpty { address })
                    onCerrar()
                }
            ) {
                Text(stringResource(id = R.string.btn_insert))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        }
    )
}

/**
 * Inserta una tabla: cuántas filas, cuántas columnas y si lleva cabecera.
 *
 * El diálogo **no cuenta las filas** —eso lo hace [buildTableRows]—, porque hacerlo aquí lo dejaría
 * sin probar. Solo recoge lo que el usuario ha marcado y se lo pasa a quien lo llama.
 *
 * @param onConfirm recibe filas, columnas y si hay cabecera.
 */
@Composable
fun InsertTableDialog(
    rows: Int,
    columns: Int,
    hasHeader: Boolean,
    onRowsChange: (Int) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onHasHeaderChange: (Boolean) -> Unit,
    onConfirm: (rows: Int, columns: Int, hasHeader: Boolean) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.insert_table)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CounterRow(
                    label = stringResource(R.string.rows_label),
                    value = rows,
                    range = 1..10,
                    steps = 8,
                    onValueChange = onRowsChange
                )
                CounterRow(
                    label = stringResource(R.string.cols_label),
                    value = columns,
                    range = 1..8,
                    steps = 6,
                    onValueChange = onColumnsChange
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasHeader, onCheckedChange = onHasHeaderChange)
                    Text(
                        stringResource(R.string.include_header_row),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(rows, columns, hasHeader); onCerrar() }) {
                Text(stringResource(id = R.string.btn_insert))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        }
    )
}

/**
 * Un deslizador con su número al lado, para los dos contadores de la tabla.
 *
 * Están juntos porque son el mismo control con distinto tope: por separado serían dos copias que
 * se tocan al cambiar el rango.
 */
@Composable
private fun CounterRow(
    label: String,
    value: Int,
    range: IntRange,
    steps: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        @Suppress("DEPRECATION")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps,
            modifier = Modifier.width(120.dp)
        )
        Text("$value", style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Las cuatro cosas que se pueden hacer con un enlace: abrirlo, copiarlo, editarlo o borrarlo.
 *
 * **Cada acción cierra el diálogo ella misma** y no se le da un `onCerrar` compartido, porque el
 * orden importa: al editar, este diálogo se cierra **antes** de abrir el de escribir, y si el
 * cierre lo hiciera el diálogo padre el orden quedaría al revés y los dos se pisarían.
 *
 * @param url el enlace sobre el que se ha pulsado.
 */
@Composable
fun UrlActionDialog(
    url: String,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(id = R.string.url_dialog_title))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                UrlActionButton(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.url_dialog_open), onOpen)
                UrlActionButton(Icons.Default.ContentCopy, stringResource(R.string.url_dialog_copy), onCopy)
                UrlActionButton(Icons.Default.Edit, stringResource(R.string.url_dialog_edit), onEdit)
                UrlActionButton(Icons.Default.Delete, stringResource(R.string.url_dialog_delete), onDelete, esPeligroso = true)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        }
    )
}

/**
 * Un botón de acción con su icono y su texto, en una columna.
 *
 * Borrar va en rojo: es la única de las cuatro que no se puede deshacer, y esconderla en un botón
 * más sin distinguir de las demás es cómo se pierde un enlace por un clic de más.
 */
@Composable
private fun UrlActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    esPeligroso: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (esPeligroso) {
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (esPeligroso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Inserta o edita una ecuación, con la vista previa mientras se escribe.
 *
 * @param isEdit solo cambia el título y el texto del botón: si está editando, lo que hay bajo el
 *   cursor se sustituye; si no, se inserta. El resto del diálogo es idéntico en los dos casos.
 * @param onConfirm recibe el LaTeX ya **recortado**. Solo se llama si no está en blanco.
 * @param onCerrar cierra por los tres caminos —confirmar, cancelar y tocar fuera—, y aquí los tres
 *   **sí** hacen lo mismo, al revés que en los diálogos de enlace y marcador. El motivo está en la
 *   nota de arriba: allí lo que se conserva al cerrar por fuera es un rango que fija otro camino.
 */
@Composable
fun EquationDialog(
    latex: String,
    isEdit: Boolean,
    onLatexChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                stringResource(
                    id = if (isEdit) R.string.dialog_edit_equation_title
                    else R.string.dialog_insert_equation_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = latex,
                    onValueChange = onLatexChange,
                    label = { Text(stringResource(id = R.string.label_equation_latex)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (latex.isNotBlank()) EquationPreview(latex)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limpio = latex.trim()
                    if (limpio.isNotBlank()) onConfirm(limpio)
                    onCerrar()
                }
            ) {
                Text(stringResource(id = if (isEdit) R.string.btn_save else R.string.btn_insert))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        }
    )
}

/** La ecuación tal como se verá, con el tema del editor y sin impedir que se pueda desplazar. */
@Composable
private fun EquationPreview(latex: String) {
    Text(
        text = stringResource(id = R.string.rich_live_preview),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        com.hrm.latex.renderer.Latex(
            latex = latex.trim(),
            config = com.hrm.latex.renderer.model.LatexConfig(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                theme = com.hrm.latex.renderer.model.LatexTheme.light(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                accessibilityEnabled = true
            ),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        )
    }
}

/**
 * Inserta un marcador.
 *
 * La URL se entrega **tal cual**, sin normalizar: normalizarla es cosa de quien la guarda, porque
 * solo él sabe si viene de crear uno o de editar uno que ya estaba.
 *
 * Igual que en el diálogo de enlace, **cancelar limpia y cerrar por fuera no**. La asimetría viene
 * de que al abrirlo el editor vacía el campo, así que dejarlo sucio solo se nota si el diálogo se
 * abre dos veces seguidas sin llegar a confirmar.
 *
 * @param onConfirm recibe la URL escrita. Se llama antes de cerrar, con el campo todavía lleno.
 */
@Composable
fun InsertBookmarkDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onCerrar: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.dialog_insert_bookmark_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text(stringResource(id = R.string.label_bookmark_url)) },
                    placeholder = { Text(stringResource(id = R.string.block_bookmark_url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(url)
                    onCerrar()
                }
            ) {
                Text(stringResource(id = R.string.btn_insert))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        }
    )
}