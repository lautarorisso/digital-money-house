# Digital Money House

## Inicio rápido

### Requisitos

- Docker
- Docker Compose
- Git
- Puertos libres: `3306`, `8080`, `8081`, `8083` y `8761`

1. Clonar el repositorio.

   ```bash
   git clone https://github.com/lautarorisso/digital-money-house
   cd digital-money-house
   ```

2. **Configurar el entorno.** Copiar `.env.example` a `.env` y reemplazar los valores antes de levantar el stack:

   ```bash
   cp .env.example .env
   ```

   | Variable                                 | Qué es                                                                   | De dónde la sacás                                                          |
   | ---------------------------------------- | ------------------------------------------------------------------------ | -------------------------------------------------------------------------- |
   | `KEYCLOAK_ADMIN_PASSWORD`                | Contraseña para entrar a la consola de admin de Keycloak                 | La elegís vos                                                              |
   | `KEYCLOAK_BACKEND_CLIENT_SECRET`         | Secreto compartido entre Keycloak y el backend (debe ser igual en ambos) | Se genera con `openssl rand -hex 32` en la terminal y se pega el resultado |
   | `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` | Contraseñas de la base de datos MySQL                                    | Las elegís vos                                                             |

3. Levantar todo el stack:

   ```bash
   docker compose up -d --build
   ```

4. **Verificar que arrancó:**

   ```bash
   docker compose ps
   ```

Listo para probar.

## Puertos y servicios

| Puerto | Servicio                   |
| ------ | -------------------------- |
| `3306` | MySQL                      |
| `8080` | **api-gateway**            |
| `8081` | Keycloak                   |
| `8083` | users-service              |
| `8761` | service-discovery (Eureka) |
| `8082` (interno) | account-service; acceso por el gateway |

> Todo entra por el gateway (`8080`).

## Logs y Swagger

La documentación se abre sin token desde el gateway:

- [Swagger de usuarios](http://localhost:8080/users-service/swagger-ui/index.html)
- [Swagger de cuentas y tarjetas](http://localhost:8080/accounts-service/swagger-ui/index.html)

Iniciar sesión con `POST /users-service/auth/login`. Para probar endpoints protegidos, pegar el `accessToken` en **Authorize → bearerAuth**; para logout, usar el `refreshToken` en **Authorize → refreshToken**. Ambos se ingresan sin el prefijo `Bearer`.

Los logs de consola registran login, logout, depósitos y transferencias exitosos, sin contraseñas, tokens ni datos de tarjetas:

```bash
docker compose logs -f users-service account-service
```

## Probar los endpoints con Postman

### Importar la colección

1. Postman → File → Import…
2. Seleccionar los dos archivos de `docs/postman/`:
   - `Digital-Money-House.postman_collection.json`
   - `Digital-Money-House.postman_environment.json`
3. Elegir el environment "Digital Money House — Local" (ya trae `baseUrl = http://localhost:8080`).

La colección incluye las carpetas **Registro**, **Login** y **Logout** (Sprint 1), **Sprint 2**, **Sprint 3** y **Sprint 4**. Para Sprint 1, ejecutar Registro → Login → Logout. Para Sprint 2, 3 o 4, seleccionar directamente la carpeta correspondiente y pulsar **Run**; cada una prepara sus propios datos automáticamente.

Antes de ejecutar, esperar a que el stack esté listo y los servicios estén registrados en Eureka (`http://localhost:8761`).

## Informes de testing

- [QA SignOff — Sprints 1 a 4](docs/testing/qa-signoff.md)

### Sprint 1

- [Informe de testing exploratorio](docs/testing/sprint-1-testing-exploratorio.md)
- [Planilla de casos de prueba](docs/testing/sprint-1-testing-manual.md)

### Sprint 2

- [Planilla de casos de prueba](docs/testing/sprint-2-testing-manual.md)

### Sprint 3

- [Informe de testing exploratorio](docs/testing/sprint-3-testing-exploratorio.md)
- [Planilla de casos de prueba](docs/testing/sprint-3-testing-manual.md)

### Sprint 4

- [Informe de testing exploratorio](docs/testing/sprint-4-testing-exploratorio.md)
- [Planilla de casos de prueba](docs/testing/sprint-4-testing-manual.md)

## Testing automatizado

Sprint 1 (Postman):

```bash
postman collection run docs/postman/Digital-Money-House.postman_collection.json -e docs/postman/Digital-Money-House.postman_environment.json -i "Registro" -i "Login" -i "Logout" --no-report-events
```

Sprint 2:

```bash
./mvnw -pl services/api-gateway -Dtest=Sprint2SmokeIT -Ddmh.baseUrl=http://localhost:8080 test
```

Sprint 3:

```bash
./mvnw -pl services/api-gateway -Dtest=Sprint3ActivityIT -Ddmh.baseUrl=http://localhost:8080 test
```

Sprint 4 (Postman):

```bash
postman collection run docs/postman/Digital-Money-House.postman_collection.json -e docs/postman/Digital-Money-House.postman_environment.json -i "Sprint 4" --no-report-events
```

Los tests habituales siguen ejecutándose con `./mvnw test`.

## Notas

- Los datos persisten en volúmenes Docker: se conservan con `docker compose down` y se borran solo con `docker compose down -v`.
- Detener el stack: `docker compose down`.
