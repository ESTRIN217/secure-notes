package com.estrin217.codetools.data.samples

import com.estrin217.codetools.data.model.CodeFile
import com.estrin217.codetools.syntax.SupportedLanguage

object CodeSamples {

    val samples: List<CodeFile> = listOf(
        CodeFile(
            name = "ServicioCotizaciones.kt",
            language = SupportedLanguage.KOTLIN,
            isSample = true,
            content = """package com.empresa.servicios

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

/**
 * Servicio reactivo para cálculo y procesamiento de cotizaciones.
 *
 * @param tasaCambio Tasa de cambio oficial del día en Bs.
 * @param margenGanancia Margen porcentual aplicado a las operaciones.
 * @return Flujo de cotizaciones validadas para facturación.
 */
@Suppress("unused")
class ServicioCotizaciones(
    private val tasaCambio: Double = 42.50,
    private val margenGanancia: Double = 0.15
) {
    // Identificador de lote y estado inicial
    private val prefijoFactura = "COT-VE"
    private var contadorSecuencia: Int = 1_000

    sealed interface EstadoPedido {
        data object Pendiente : EstadoPedido
        data class Aprobado(val codigoAutorizacion: String, val montoTotal: Double) : EstadoPedido
        data class Rechazado(val motivo: String) : EstadoPedido
    }

    data class ItemCotizacion(
        val descripcion: String,
        val cantidad: Int,
        val precioUnitarioUsd: Double,
        val aplicaIva: Boolean = true
    )

    /**
     * Procesa la cotización del cliente aplicando la tasa y márgenes.
     * Pana, si no hay ítems lanzamos error de una vez.
     */
    fun calcularPresupuesto(
        cliente: String,
        items: List<ItemCotizacion>
    ): EstadoPedido {
        require(items.isNotEmpty()) { "Chamo, no puedes procesar una cotización sin ítems!" }

        val subtotalUsd = items.sumOf { item ->
            val totalLinea = item.cantidad * item.precioUnitarioUsd
            if (item.aplicaIva) totalLinea * 1.16 else totalLinea
        }

        val totalConMargen = subtotalUsd * (1.0 + margenGanancia)
        val totalBolivares = totalConMargen * tasaCambio

        // Chamo, si el monto supera 50.000 USD requiere aprobación manual
        return if (totalConMargen > 50_000.0) {
            EstadoPedido.Rechazado("Monto excede el límite automático: ${'$'}totalConMargen USD")
        } else {
            contadorSecuencia++
            val codigo = "${'$'}prefijoFactura-${'$'}contadorSecuencia"
            println("Pana, cotización ${'$'}codigo generada para ${'$'}cliente por Bs. ${'$'}{totalBolivares}")
            EstadoPedido.Aprobado(codigoAutorizacion = codigo, montoTotal = totalBolivares)
        }
    }

    /**
     * Emite un flujo de actualizaciones en tiempo real.
     */
    fun monitorearTasaEnVivo(): Flow<Double> = flow {
        var tasaActual = tasaCambio
        repeat(5) { paso ->
            delay(1000L)
            tasaActual += (paso * 0.05)
            emit(tasaActual)
        }
    }
}
"""
        ),
        CodeFile(
            name = "deploy_backup.sh",
            language = SupportedLanguage.BASH,
            isSample = true,
            content = """#!/usr/bin/env bash
# =========================================================
# Script de Automatización: Respaldo y Despliegue de Servicios
# Autor: DevOps Team
# Fecha: 2026-09-05
# =========================================================

set -euo pipefail

APP_NAME="api-gateway"
DEPLOY_DIR="/opt/production/${'$'}{APP_NAME}"
BACKUP_DIR="/var/backups/${'$'}{APP_NAME}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
MAX_BACKUPS=5

echo "==> Iniciando proceso para: ${'$'}APP_NAME [${'$'}TIMESTAMP]"

# Verificar directorio de respaldos
if [ ! -d "${'$'}BACKUP_DIR" ]; then
    echo "[INFO] Creando directorio de respaldo: ${'$'}BACKUP_DIR"
    mkdir -p "${'$'}BACKUP_DIR"
fi

backup_database() {
    local target_file="${'$'}{BACKUP_DIR}/db_dump_${'$'}{TIMESTAMP}.sql.gz"
    echo "[DB] Generando respaldo comprimido..."
    # pg_dump -U postgres -h localhost production_db | gzip > "${'$'}target_file"
    echo "[OK] Respaldo guardado en: ${'$'}target_file"
}

cleanup_old_backups() {
    echo "[CLEAN] Eliminando respaldos antiguos (> ${'$'}MAX_BACKUPS copias)..."
    ls -t "${'$'}{BACKUP_DIR}"/db_dump_*.sql.gz 2>/dev/null | tail -n +$((MAX_BACKUPS + 1)) | xargs -r rm -f
}

deploy_release() {
    echo "[DEPLOY] Actualizando versión en ${'$'}DEPLOY_DIR..."
    cd "${'$'}DEPLOY_DIR" || exit 1
    
    # Recargar servicios
    echo "[SERVICE] Reiniciando contenedores de producción..."
    # docker compose pull && docker compose up -d --remove-orphans
    
    # Health check
    echo "[HEALTH] Verificando estado HTTP en puerto 8080..."
    local status_code
    status_code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health || echo "500")
    
    if [ "${'$'}status_code" -eq 200 ]; then
        echo "[EXITO] Despliegue completado sin errores (HTTP 200)."
    else
        echo "[ERROR] El servicio reportó código: ${'$'}status_code. Iniciando rollback!"
        return 1
    fi
}

main() {
    backup_database
    cleanup_old_backups
    deploy_release
    echo "==> Tarea finalizada exitosamente."
}

main "${'$'}@"
"""
        ),

        CodeFile(
            name = "server_config.json",
            language = SupportedLanguage.JSON,
            isSample = true,
            content = """{
  "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
  "app": {
    "name": "CloudSync Engine",
    "version": "2.4.1",
    "environment": "production",
    "debug": false
  },
  "server": {
    "host": "0.0.0.0",
    "port": 8443,
    "ssl": {
      "enabled": true,
      "cert_file": "/etc/ssl/certs/server.crt",
      "key_file": "/etc/ssl/private/server.key",
      "protocols": ["TLSv1.2", "TLSv1.3"]
    },
    "timeouts": {
      "read_seconds": 30,
      "write_seconds": 45,
      "idle_seconds": 120
    }
  },
  "database": {
    "engine": "postgresql",
    "pool_size": 25,
    "max_overflow": 10,
    "connection_timeout": 5000,
    "auto_migrate": true
  },
  "features": {
    "enable_rate_limit": true,
    "requests_per_minute": 1200,
    "audit_logging": true,
    "cors_origins": [
      "https://dashboard.empresa.com",
      "https://admin.empresa.com"
    ]
  },
  "telemetry": {
    "enabled": true,
    "sample_rate": 0.15,
    "endpoint": "https://metrics.internal.net/v1/traces"
  }
}
"""
        ),

        CodeFile(
            name = "docker-compose.yaml",
            language = SupportedLanguage.YAML,
            isSample = true,
            content = """# Configuración de Orquestación Docker Compose
# Entorno: Desarrollo y Pruebas Locales
version: "3.9"

services:
  web-api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: api_core_service
    restart: unless-stopped
    ports:
      - "8000:8000"
    environment:
      - APP_ENV=staging
      - DATABASE_URL=postgres://app_user:s3cr3t@postgres-db:5432/app_db
      - REDIS_HOST=redis-cache
      - LOG_LEVEL=debug
    depends_on:
      postgres-db:
        condition: service_healthy
      redis-cache:
        condition: service_started
    volumes:
      - ./logs:/var/log/app
      - ./uploads:/app/uploads

  postgres-db:
    image: postgres:16-alpine
    container_name: postgres_primary
    restart: always
    environment:
      POSTGRES_DB: app_db
      POSTGRES_USER: app_user
      POSTGRES_PASSWORD: ${'$'}{DB_PASSWORD:-local_dev_pass}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app_user -d app_db"]
      interval: 5s
      timeout: 3s
      retries: 5

  redis-cache:
    image: redis:7.2-alpine
    container_name: redis_cache
    command: ["redis-server", "--appendonly", "yes", "--requirepass", "redis_pass"]
    ports:
      - "6379:6379"
    volumes:
      - redisdata:/data

volumes:
  pgdata:
    driver: local
  redisdata:
    driver: local
"""
        ),

        CodeFile(
            name = "log_processor.py",
            language = SupportedLanguage.PYTHON,
            isSample = true,
            content = """#!/usr/bin/env python3
\"\"\"
Script de Procesamiento de Registros de Acceso
Filtra errores 5xx y genera alertas de latencia.
\"\"\"

import os
import sys
import re
from datetime import datetime
from typing import Dict, List, Optional

LOG_PATTERN = re.compile(
    r'^(?P<ip>\S+) \S+ \S+ \[(?P<time>[^\]]+)\] "(?P<method>\S+) (?P<path>\S+) \S+" (?P<status>\d{3}) (?P<size>\d+) (?P<latency>\d+\.\d+)'
)

class LogAnalyzer:
    def __init__(self, threshold_ms: float = 800.0):
        self.threshold_ms = threshold_ms
        self.error_count = 0
        self.slow_requests: List[Dict] = []

    def parse_line(self, line: str) -> Optional[Dict]:
        match = LOG_PATTERN.match(line.strip())
        if not match:
            return None
        data = match.groupdict()
        data["status"] = int(data["status"])
        data["latency_ms"] = float(data["latency"]) * 1000.0
        return data

    def process_file(self, file_path: str) -> None:
        if not os.path.exists(file_path):
            print(f"[ERROR] Archivo no encontrado: {file_path}")
            sys.exit(1)

        with open(file_path, "r", encoding="utf-8") as f:
            for idx, line in enumerate(f, start=1):
                entry = self.parse_line(line)
                if not entry:
                    continue

                if entry["status"] >= 500:
                    self.error_count += 1
                    print(f"[5XX] Línea {idx}: {entry['method']} {entry['path']} -> {entry['status']}")

                if entry["latency_ms"] > self.threshold_ms:
                    self.slow_requests.append(entry)

    def print_summary(self) -> None:
        print("=" * 45)
        print("RESUMEN DE ANÁLISIS DE REGISTROS")
        print("=" * 45)
        print(f"Total de errores 5xx detectados: {self.error_count}")
        print(f"Peticiones lentas (> {self.threshold_ms}ms): {len(self.slow_requests)}")


if __name__ == "__main__":
    analyzer = LogAnalyzer(threshold_ms=500.0)
    print("Iniciando escaneo de logs...")
    analyzer.print_summary()
"""
        ),

        CodeFile(
            name = ".env.production",
            language = SupportedLanguage.ENV,
            isSample = true,
            content = """# ==========================================
# VARIABLES DE ENTORNO - AMBIENTE PRODUCCIÓN
# ==========================================

# Configuración Base
NODE_ENV=production
APP_PORT=8080
APP_SECRET_KEY=k8f93j20fj3m_super_secure_hash_2026

# Base de Datos Principal
DB_HOST=db.cluster.internal.net
DB_PORT=5432
DB_NAME=core_enterprise_db
DB_USER=app_prod_worker
DB_PASSWORD="p@ssword_with_special_ch@rs!"
DB_SSL_MODE=require
DB_POOL_MAX=30

# Caché y Colas
REDIS_URL=rediss://default:token_cache_99@cache.internal.net:6380
CACHE_TTL_SECONDS=3600

# Servicios de Correo y Notificaciones
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USER=apikey
SMTP_API_KEY=SG.938f2kd092jf9_example_api_key
MAIL_FROM_ADDRESS="no-reply@empresa.com"

# Proveedores de Pagos y Seguridad
STRIPE_PUBLIC_KEY=pk_live_51M00000000000000000000
JWT_EXPIRATION_HOURS=24
ENABLE_TWO_FACTOR_AUTH=true
RATE_LIMIT_MAX_REQUESTS=100
"""
        ),

        CodeFile(
            name = "nginx_gateway.conf",
            language = SupportedLanguage.INI_TOML,
            isSample = true,
            content = """# ==========================================
# Configuración Servidor Web / Reverse Proxy
# ==========================================

[server]
listen = 80
server_name = api.empresa.com
access_log = /var/log/nginx/api_access.log
error_log = /var/log/nginx/api_error.log
client_max_body_size = 50M

[upstream_cluster]
keepalive = 32
timeout = 60s
server_1 = 10.0.1.10:8080
server_2 = 10.0.1.11:8080

[security_headers]
X-Frame-Options = DENY
X-Content-Type-Options = nosniff
X-XSS-Protection = "1; mode=block"
Strict-Transport-Security = "max-age=31536000; includeSubDomains"

[ssl_configuration]
ssl_certificate = /etc/letsencrypt/live/api.empresa.com/fullchain.pem
ssl_certificate_key = /etc/letsencrypt/live/api.empresa.com/privkey.pem
ssl_session_cache = shared:SSL:10m
ssl_session_timeout = 10m
"""
        )
    )
}
