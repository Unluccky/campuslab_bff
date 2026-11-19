# CampusLab BFF

Backend for Frontend de CampusLab. Java 21 y Spring Boot. Centraliza la autenticación Cognito, valida tokens académicos de Microsoft Entra y reenvía solicitudes a Catálogo, Bookings, Reportería y Rabbit Admin. El BFF no sustituye la validación JWT ni las reglas de rol de cada servicio.

## Rutas principales

| Ruta | Función |
|---|---|
| `POST /api/bff/auth/login` | Inicia sesión en Cognito. |
| `POST /api/bff/auth/register` | Inicia el registro público en Cognito. |
| `POST /api/bff/auth/confirm` | Confirma el código de registro. |
| `GET /api/bff/academic/me` | Devuelve identidad y roles de una sesión académica válida. |
| `/api/bff/academic/reservas`, `/tickets`, `/recursos` | Proxy académico a Bookings y Catálogo. |
| `/api/bff/academic/reportes/kpi` y `/mis-reservas` | Proxy a Reportería. KPI requiere rol Admin; mis reservas requiere autenticación. |
| `/api/bff/academic/kafka/topics` | Lista, crea, actualiza retención y elimina topics `lab.*`; requiere Admin en Reportería. |
| `/api/bff/academic/rabbit/**` | Proxy a Rabbit Admin; requiere identidad académica Admin. |
| `/api/bff/admin/directorio` | Consulta de directorios configurados; requiere Admin. |

Las rutas académicas verifican issuer, scope y autoridades, y propagan el Bearer token al servicio correspondiente. `POST /auth/register` y `POST /auth/confirm` son públicas para el flujo de registro; la autorización de otras APIs sigue protegida.

## Ejecución local

Requiere JDK 21. Configurar las variables de conexión y autenticación en el entorno de la terminal, sin escribir secretos en archivos versionados.

```powershell
./mvnw.cmd spring-boot:run
```

Puerto predeterminado: `8081`. Salud: `http://localhost:8081/actuator/health`.

Variables principales:

- `PORT`, `JWT_ISSUER_URI`, `AZURE_ENABLED`, `AZURE_ADMIN_OBJECT_IDS`, `AZURE_OPERATOR_OBJECT_IDS`.
- `CATALOG_BASE_URL`, `REPORTES_BASE_URL`, `CAMPUSLAB_BOOKINGS_URL`, `CAMPUSLAB_RABBIT_ADMIN_URL`.
- `CAMPUSLAB_ALLOWED_ORIGINS`.
- Para el flujo Cognito local, usar credenciales temporales de AWS Academy mediante el proveedor de credenciales de AWS, nunca en `.env` versionado.

## Despliegue

API Gateway publica las rutas del BFF hacia la instancia de aplicaciones. La ruta académica conserva el path con `overwrite:path`; OPTIONS debe permitir CORS sin exigir JWT. Las IP de integración pueden cambiar al reiniciar la instancia de AWS Academy. Los puertos internos de los servicios se documentan en [CampusLab-Documentacion](https://github.com/Unluccky/CampusLab-Documentacion/tree/main/infra/ep3).

## Seguridad

- No colocar client secrets, claves AWS, tokens ni contraseñas en el código.
- Azure y Cognito son proveedores separados; cada API valida el issuer que le corresponde.
- Los accesos de administrador se asignan mediante claims/roles o los Object IDs configurados en backend; un login exitoso no concede privilegios por sí solo.
