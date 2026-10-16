package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crash.CrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de la pantalla del informe: cargando, o el texto, o vacío.
 *
 * `texto` vacío **es** el estado vacío de RF-123: no hay un tercer caso, porque no hay informe a medias.
 */
data class CrashInformeUiState(
    val cargando: Boolean = true,
    val texto: String = ""
)

/**
 * Carga el informe pendiente (spec 020, RF-121) y lo consume (RF-122).
 *
 * Vive en un `AndroidViewModel` aunque no toque la base de datos: el I/O va en `Dispatchers.IO` —leer un
 * fichero en el hilo principal es justo lo que `AGENTS.md` prohíbe— y quien decide es el composable, no el
 * `CrashStore`.
 */
class CrashReportViewModel(application: Application) : AndroidViewModel(application) {

    private val _informe = MutableStateFlow(CrashInformeUiState())
    val informe: StateFlow<CrashInformeUiState> = _informe.asStateFlow()

    init {
        viewModelScope.launch {
            _informe.value = CrashInformeUiState(cargando = false, texto = leerEnDisco())
        }
    }

    /**
     * Borra el informe y cancela su notificación.
     *
     * Sin esto la app volvería a avisar del mismo fallo en cada arranque (RF-122), que es justo lo que hace
     * inservible un aviso que no se puede silenciar.
     *
     * @param alTerminar se invoca en el hilo principal, ya con el fichero borrado: cerrar la pantalla desde
     * aquí y no antes es lo que evita el parpadeo de «sin informe» al ir hacia atrás.
     */
    fun consumir(alTerminar: () -> Unit) {
        viewModelScope.launch {
            CrashReporter.consumir(getApplication())
            alTerminar()
        }
    }

    private suspend fun leerEnDisco(): String = withContext(Dispatchers.IO) {
        CrashReporter.almacenDe(getApplication()).leer()
    }.orEmpty()
}