# Experiencia 2 — estado actual

Rama `feature/siguiente-experiencia`. Login académico mediante Azure/MSAL, token validado en API Gateway y Spring. Se conserva compatibilidad con Cognito para las rutas anteriores. La configuración cloud y evidencia del 19/10 están en [CampusLab-Documentacion](https://github.com/Unluccky/CampusLab-Documentacion/tree/feature/siguiente-experiencia).

BFF desplegado en EC2, consumido por Angular mediante Gateway. `/api/bff/academic/me` verifica identidad y roles; `/recursos`, `/reservas`, `/tickets` y `/rabbit` reenvían exclusivamente a los servicios configurados. La administración de mensajería exige Azure y Admin. Mantener el mapeo Gateway `overwrite:path=$request.path` y una integración OPTIONS sin JWT para CORS.

Variables: `AZURE_ENABLED`, `AZURE_ADMIN_OBJECT_IDS`, `AZURE_OPERATOR_OBJECT_IDS`, `CATALOG_BASE_URL`, `CAMPUSLAB_BOOKINGS_URL`, `CAMPUSLAB_RABBIT_ADMIN_URL`. No requiere credenciales AWS para verificar JWT Azure; el login Cognito anterior sí usa su configuración AWS.

---

## Documentación anterior y compatibilidad

# Actualización: siguiente experiencia

Rama feature/siguiente-experiencia. Incluye las mejoras solicitadas después de EA1; ver CampusLab-Documentacion/infra/siguiente/README.md. Catálogo, reservas y usuarios usan RDS en AWS. El frontend incorpora /admin/usuarios, guard probado y Login Admin renovado. El BFF conserva el login con Cognito y requiere credenciales temporales vigentes. La documentación EA1 que sigue describe la entrega anterior.

# CampusLab BFF

Backend de autenticación de CampusLab para la EA1 de Iniciando Cloud Native I (Duoc UC). Implementado con Java 21, Spring Boot 4.1.1, Spring Security y AWS SDK Cognito.

## Funciones

- `POST /api/bff/auth/login`: recibe `email` y `password`; devuelve access token, ID token, refresh token, vigencia y tipo de token.
- `GET /api/bff/catalogo` y `GET /api/bff/catalogo/{id}`: consultan catálogo y propagan el Bearer token.
- Dos cadenas de seguridad: autenticación pública separada de los endpoints que validan JWT.
- Credenciales incorrectas: HTTP 401. Desafío Cognito pendiente: HTTP 409.
- El BFF exige autenticación para consultas; catalog comprueba el grupo `ROLE_ADMIN`.

## Ejecución local

Requiere JDK 21. Usar `mvnw.cmd` en Windows o `./mvnw` en Linux/macOS.

Definir `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN` con las credenciales temporales de AWS Academy **en el entorno de la terminal**, nunca en archivos versionados.

```powershell
.\mvnw.cmd spring-boot:run
```

Puerto: 8081. Salud: `http://localhost:8081/actuator/health`.

Variables opcionales: `PORT`, `JWT_ISSUER_URI`, `CATALOG_BASE_URL` (por defecto `http://localhost:8082`). Cognito está configurado en us-east-1 para el pool del proyecto; sus identificadores públicos no son credenciales.

## Docker

```powershell
docker build -t campuslab-bff .
docker run --rm -p 8081:5000 -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_SESSION_TOKEN -e CATALOG_BASE_URL=http://host.docker.internal:8082 campuslab-bff
```

`host.docker.internal` corresponde a Docker Desktop. Ajustar el host cuando se use Linux. El Dockerfile usa el puerto interno 5000.

## Validación y alcance

```powershell
.\mvnw.cmd test
```

Login real probado con Cognito. Actualmente BFF y Angular se ejecutan localmente; el CRUD Angular usa API Gateway hacia catalog en EC2. El BFF todavía no implementa escrituras CRUD ni se desplegó en AWS.

[Documentación, diagramas y despliegue](https://github.com/Unluccky/CampusLab-Documentacion)

Verificación de contexto ejecutada el 22/09/2026: 1 prueba aprobada.
