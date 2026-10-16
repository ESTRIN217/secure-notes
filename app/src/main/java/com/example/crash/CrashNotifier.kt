package com.example.crash

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.CrashReportActivity
import com.example.R

/**
 * La notificación del fallo pendiente (spec 020, RF-119) y su cancelación (RF-122).
 *
 * **No pide permiso**: la app hoy solo lo pide cuando hay una actualización disponible, y pedirlo en cada
 * arranque molesta más de lo que informa. Sin permiso esta notificación simplemente no sale -y no rompe-,
 * y el aviso de dentro de la app es lo que avisa siempre (caso límite 3).
 */
object CrashNotifier {

    private const val TAG = "CrashNotifier"

    /** Canal propio: no se mezcla con el de actualizaciones, que tiene otro propósito. */
    const val CANAL = "fallos"

    /** Id fijo: hay **un** informe pendiente, y el anterior ya se consumió (caso límite 1). */
    private const val ID_NOTIFICACION = 4201

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL,
            context.getString(R.string.crash_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.crash_channel_description) }
        gestor(context).createNotificationChannel(canal)
    }

    /** Publica la notificación **si** hay informe pendiente y permiso concedido. */
    fun anunciar(context: Context) {
        if (!CrashReporter.almacenDe(context).hayPendiente()) return
        if (!puedeNotificar(context)) {
            Log.i(TAG, "sin permiso de notificaciones: avisa el aviso de la app")
            return
        }
        runCatching { gestor(context).notify(ID_NOTIFICACION, notificacion(context)) }
            .onFailure { Log.e(TAG, "no se pudo publicar la notificación del fallo", it) }
    }

    /** Cancela la notificación. Va **siempre** junto a consumir el informe (RF-122). */
    fun cancelar(context: Context) {
        runCatching { gestor(context).cancel(ID_NOTIFICACION) }
            .onFailure { Log.e(TAG, "no se pudo cancelar la notificación del fallo", it) }
    }

    private fun notificacion(context: Context): Notification {
        val abrir = PendingIntent.getActivity(
            context,
            0,
            Intent(context, CrashReportActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val texto = context.getString(R.string.crash_notification_text)
        return NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.crash_notification_title))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
    }

    /**
     * Sin este permiso, `notify()` es un **no-op silencioso** en Android 13+: la app creería haber avisado y
     * no habría avisado (caso límite 3).
     */
    private fun puedeNotificar(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun gestor(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}