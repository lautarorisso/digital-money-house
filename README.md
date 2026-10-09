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
| `8082` | account-service            |

> Todo entra por el gateway (`8080`).

## Probar los endpoints con Postman

### Importar la colección

1. Postman → File → Import…
2. Seleccionar los dos archivos de `docs/postman/`:
   - `Digital-Money-House.postman_collection.json`
   - `Digital-Money-House.postman_environment.json`
3. Elegir el environment "Digital Money House — Local" (ya trae `baseUrl = http://localhost:8080`).

La colección incluye las carpetas **Registro**, **Login** y **Logout** (Sprint 1), **Sprint 2** y **Sprint 3**. Para Sprint 1, ejecutar Registro → Login → Logout. Para Sprint 2 o 3, seleccionar directamente la carpeta correspondiente y pulsar **Run**; cada una prepara sus propios datos automáticamente.

## Planilla de casos de prueba

- [Planilla de casos de prueba (Sprint 1)](docs/testing/sprint-1-testing-manual.md)
- [Planilla de casos de prueba (Sprint 2)](docs/testing/sprint-2-testing-manual.md)

## Testing automatizado

Sprint 2:

```bash
./mvnw -pl services/api-gateway -Dtest=Sprint2SmokeIT -Ddmh.baseUrl=http://localhost:8080 test
```

Sprint 3:

```bash
./mvnw -pl services/api-gateway -Dtest=Sprint3ActivityIT -Ddmh.baseUrl=http://localhost:8080 test
```

La suite registra e inicia sesión automáticamente; no requiere credenciales ni IDs. Los tests habituales siguen ejecutándose con `./mvnw test`.

## Notas

- Los datos persisten en volúmenes Docker: se conservan con `docker compose down` y se borran solo con `docker compose down -v`.
- Detener el stack: `docker compose down`.
