package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.chat.ChatSuggestion

/**
 * Pantalla de bienvenida del chat con IA (RF-36, RF-37, RF-40). **Solo pinta y lanza eventos**
 * (decisión D-5 de `plan.md`):
 *
 * - **Qué sugerencias se ven** lo decide L1 (`visibleSuggestions`) y llega ya resuelto en
 *   [suggestions]; aquí no se añade ni se quita ninguna, ni se decide su visibilidad condicional.
 * - **Qué hace** la sugerencia tocada lo decide L2 (`requestFor`), en la pantalla: [onSuggestion]
 *   recibe la misma sugerencia que se pinta, con su etiqueta ya resuelta, y este archivo **no** mira
 *   su acción ni construye ninguna petición.
 * - **El saludo** [greeting] llega ya resuelto —el día y el índice los calcula L3 en la pantalla— y no
 *   lleva ningún nombre concatenado ni lee ninguna preferencia (RF-36).
 *
 * Las cuatro etiquetas salen de los recursos (RF-40): tres claves ya traducidas y la nueva de la
 * cuarta, ninguna escrita como texto.
 */
@Composable
fun EmptyChatWelcome(
    greeting: String,
    suggestions: List<ChatSuggestion>,
    onSuggestion: (ChatSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    // Las etiquetas se resuelven **antes** de la fila: `stringResource` es una llamada composible y
    // no se puede invocar desde dentro de `items`.
    val labelled = withResolvedLabels(suggestions)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono / Branding dinámico de la IA
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Saludo en Headline: la frase ya viene resuelta desde la pantalla.
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Sugerencias en fila horizontal scrollable (Pills estilo Kimi/Gemini)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(
                items = labelled,
                key = { it.id },
                contentType = { SUGGESTION_CHIP }
            ) { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestion(suggestion) },
                    label = { Text(suggestion.label, style = MaterialTheme.typography.labelMedium) },
                    shape = CircleShape,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }
    }
}

/**
 * Rellena la etiqueta de cada sugerencia con su clave de recurso, por su `id` estable (el que L1
 * declara: `poem`, `ideas`, `explain`, `summarize`), sin tocar el `id` ni la acción.
 *
 * Se resuelve **antes** de la fila porque `stringResource` es una llamada composable; el `id` que no
 * aparezca en el mapa conserva la etiqueta que llega, con lo que una sugerencia nueva no se pinta
 * en blanco ni desaparece.
 */
@Composable
private fun withResolvedLabels(suggestions: List<ChatSuggestion>): List<ChatSuggestion> {
    val labels = SUGGESTION_LABEL_RES.mapValues { (_, res) -> stringResource(res) }
    return suggestions.map { it.copy(label = labels[it.id] ?: it.label) }
}

/** Etiqueta visible de cada sugerencia, por `id`. Ninguna se escribe como texto en el código. */
private val SUGGESTION_LABEL_RES: Map<String, Int> = mapOf(
    "poem" to R.string.ai_suggestion_poem,
    "ideas" to R.string.ai_suggestion_ideas,
    "explain" to R.string.ai_suggestion_explain_concept,
    "summarize" to R.string.ai_summarize
)

// Clave de `contentType` de los chips de sugerencia. Ints: no asignan por item. Es la única
// declaración de este tipo en el welcome; las de los mensajes y las fichas de adjuntos viven en
// `AiChatScreen.kt` y en `ui/chat/ChatInputState.kt`, con otros valores (RF-39).
private const val SUGGESTION_CHIP = 5