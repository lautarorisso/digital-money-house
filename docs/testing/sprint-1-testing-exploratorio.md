# Testing exploratorio — Sprint 1

**Proyecto:** Digital Money House (billetera virtual)
**Fecha:** 17/09/2026
**Entorno:** stack completo en Docker Compose (gateway, users-service, account-service, Keycloak, MySQL)

## Alcance

Se realizó testing exploratorio sobre:

- Registro de usuarios
- Login
- Logout

## Registro

### Caso 1 — Registro con datos válidos

**Tour:** Datos válidos
**Escenario:** registrar un usuario con nombre, apellido, DNI, email, teléfono y contraseña válidos.
**Resultado esperado:** el registro se completa y devuelve HTTP 201 con CVU y alias.
**Resultado obtenido:** HTTP 201 con id, CVU y alias.

### Caso 2 — Registro con campos obligatorios faltantes

**Tour:** Campos vacíos
**Escenario:** registrar sin nombre, apellido, DNI y contraseña.
**Resultado esperado:** HTTP 400 con mensajes claros por cada campo.
**Resultado obtenido:** HTTP 400.

### Caso 3 — Registro con email con formato inválido

**Tour:** Datos inválidos
**Escenario:** registrar con un email que no tiene formato válido.
**Resultado esperado:** HTTP 400.
**Resultado obtenido:** HTTP 400.

### Caso 4 — Registro con email ya registrado

**Tour:** Usuario/email existente
**Escenario:** registrar con un email que ya existe.
**Resultado esperado:** HTTP 400 indicando que el email ya está registrado.
**Resultado obtenido:** HTTP 400.

### Caso 5 — Registro con contraseña de un solo carácter

**Tour:** Datos inválidos
**Escenario:** registrar con la contraseña `"1"`.
**Resultado esperado:** el sistema debe rechazar la contraseña por no cumplir la política de contraseñas.
**Resultado obtenido:** el sistema acepta el registro y devuelve HTTP 201.
**Hallazgo:** BUG-001 — La contraseña de un solo carácter es aceptada.
**Estado:** corregido y re-testeado; ver ficha BUG-001.

### Caso 6 — Registro con JSON malformado

**Tour:** Datos inválidos
**Escenario:** enviar un cuerpo con JSON malformado.
**Resultado esperado:** HTTP 400, porque el error es del cliente.
**Resultado obtenido:** HTTP 500.
**Hallazgo:** BUG-002 — El JSON malformado se responde como error de servidor.
**Estado:** corregido y re-testeado; ver ficha BUG-002.

### Caso 7 — Registro con caracteres especiales en el nombre

**Tour:** Datos inválidos
**Escenario:** registrar con un nombre que contiene `()`, `;`, `&`, `<`, `>`.
**Resultado esperado:** HTTP 400 con un mensaje que indique el campo rechazado.
**Resultado obtenido:** HTTP 500 sin explicación.
**Hallazgo:** BUG-003 — Esos caracteres son rechazados por Keycloak y el error no se traduce.
**Estado:** corregido y re-testeado; ver ficha BUG-003.

## Login

### Caso 8 — Login con credenciales válidas

**Tour:** Credenciales válidas
**Escenario:** iniciar sesión con email y contraseña correctos.
**Resultado esperado:** HTTP 200 con access token y refresh token.
**Resultado obtenido:** HTTP 200 con ambos tokens.

### Caso 9 — Login con usuario inexistente

**Tour:** Credenciales inválidas
**Escenario:** iniciar sesión con un email no registrado.
**Resultado esperado:** HTTP 404.
**Resultado obtenido:** HTTP 404.

### Caso 10 — Login con contraseña incorrecta

**Tour:** Credenciales inválidas
**Escenario:** iniciar sesión con un email válido y contraseña incorrecta.
**Resultado esperado:** HTTP 400.
**Resultado obtenido:** HTTP 400.

### Caso 11 — Login con campos vacíos

**Tour:** Datos incompletos
**Escenario:** iniciar sesión con email y contraseña vacíos.
**Resultado esperado:** HTTP 400.
**Resultado obtenido:** HTTP 400.

## Logout

### Caso 12 — Logout con refresh token válido

**Tour:** Logout
**Escenario:** cerrar sesión enviando el refresh token en el header `Authorization`.
**Resultado esperado:** HTTP 200 con revocación de la sesión.
**Resultado obtenido:** HTTP 200 con `Authorization: Bearer <refresh token>`; el refresh token queda inutilizado en Keycloak.

### Caso 13 — Logout sin token

**Tour:** Logout
**Escenario:** cerrar sesión sin enviar token.
**Resultado esperado:** HTTP 200 — sin token no hay nada que revocar.
**Resultado obtenido:** HTTP 200 con `Authorization` ausente.

## Workflow principal

Registro
→ Login
→ Obtención del token
→ Logout

El camino feliz funciona de punta a punta y el logout cierra la sesión en Keycloak.

## Bugs encontrados

### BUG-001 — Falta de validación de la longitud mínima de la contraseña

**Tipo:** Validación / lógica de negocio
**Descripción:** el endpoint de registro permite crear usuarios con contraseñas con menos caracteres de los permitidos.
**Resultado esperado:** la contraseña debe cumplir la política definida (mínimo y máximo de caracteres).
**Resultado obtenido:** el usuario es creado correctamente (HTTP 201).
**Severidad:** Media
**Estado:** Corregido
**Corrección:** se incorporó la política de contraseñas a la validación de entrada del registro: se exige una longitud mínima de 8 caracteres y una máxima de 64.
**Retest:** contraseñas con menos de 8 caracteres (`1`, `Ab1`, `1234567`) y una de más de 64 responden HTTP 400 con el mensaje de la regla incumplida.

### BUG-002 — El JSON malformado devuelve 500 en lugar de 400

**Tipo:** Manejo de errores
**Descripción:** ante un cuerpo con JSON malformado, el handler genérico de excepciones responde 500 en lugar de 400.
**Resultado esperado:** HTTP 400, porque el error es del cliente.
**Resultado obtenido:** HTTP 500.
**Severidad:** Alta
**Estado:** Corregido
**Corrección:** se agregó el manejo de `HttpMessageNotReadableException` en el `GlobalExceptionHandler`, que responde HTTP 400 con el mensaje "Malformed JSON request body". Antes caía en el handler genérico de `Exception`, que responde 500 a cualquier error no contemplado.
**Retest:** el registro con JSON malformado (`{"nombre": "Ana",`), con cuerpo vacío y con un JSON válido que no es un objeto (`[]`) responden HTTP 400 con el mensaje del handler.

### BUG-003 — Caracteres especiales rechazados sin mensaje claro

**Tipo:** Validación / integración
**Descripción:** nombres con `()`, `;`, `&`, `<`, `>` producen HTTP 500 sin explicación; Keycloak los rechaza y el error no se traduce al contrato de respuesta.
**Resultado esperado:** HTTP 400 con un mensaje que indique el campo rechazado.
**Resultado obtenido:** HTTP 500 "An unexpected error occurred".
**Severidad:** Media
**Estado:** Corregido
**Corrección:** se tradujo el rechazo de Keycloak al contrato de la API. `KeycloakClient.createUser` ahora captura el `400 Bad Request` del proveedor, lee su `errorMessage` y responde `400` con un mensaje que indica el campo.
**Retest:** nombres con `()`, `<>`, `;`, `&`, `[` y `%` responden HTTP 400 "Name contains invalid characters"; un apellido con `&` responde HTTP 400 "Last name contains invalid characters".

## Conclusión

Se exploraron las funcionalidades incorporadas durante el sprint: registro, login y logout, verificando la persistencia contra MySQL y Keycloak. El camino feliz funciona de punta a punta, los datos se guardan de forma consistente, y los casos de error definidos por la consigna se cumplen. Los hallazgos están en el manejo de errores y en la validación; quedan documentados como BUG-001, BUG-002 y BUG-003 con su corrección y retest.
