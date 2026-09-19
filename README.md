# Digital Money House

## Inicio rápido

El stack completo (API Gateway, service discovery, users-service, account-service, Keycloak y MySQL) se levanta con un solo comando de Docker.

### Requisitos

- Docker
- Docker Compose
- Git

1. Clonar el repositorio.

   ```bash
   git clone <url-repositorio>
   cd Digital-Money-House
   ```

2. **Configurar el entorno.** Copiar `.env.example` a `.env` y editar los valores (contraseñas, realm y clientes de Keycloak, credenciales de MySQL):

   ```bash
   cp .env.example .env
   ```

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

> Todo entra por el gateway (`8080`). `8082` (account-service) es solo de comunicación interna entre servicios; no se prueba desde afuera.

## Probar los endpoints con Postman

### Importar la colección

1. Postman → File → Import…
2. Seleccionar los dos archivos de `docs/postman/`:
   - `Digital-Money-House.postman_collection.json`
   - `Digital-Money-House.postman_environment.json`
3. Elegir el environment "Digital Money House — Local" (ya trae `baseUrl = http://localhost:8080`).

### Orden de ejecución

Se corre carpeta por carpeta, en este orden — cada request tiene aserciones automáticas, así que el resultado se ve en el test (verde si pasa, rojo si falla):

1. **Registro** (7 requests): `POST /users-service/users/register`
2. **Login** (4 requests): `POST /users-service/auth/login`
3. **Logout** (2 requests): `POST /users-service/user/logout`

> El primer request genera un email único por corrida, así que la colección se puede correr todas las veces que quieras sin repetir datos.

## Planilla de casos de prueba

- [Planilla de casos de prueba (Sprint 1)](docs/testing/sprint-1-testing-manual.md)

## Notas

- Los datos persisten en volúmenes Docker: se conservan con `docker compose down` y se borran solo con `docker compose down -v`.
- Detener el stack: `docker compose down`.
