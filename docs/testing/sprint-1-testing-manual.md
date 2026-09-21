# Planilla de casos de prueba (Testing manual) — Sprint 1

## Resultado general

- Total de casos: 13
- Aprobados: 13 / 13
- Fallidos: 0
- Suite de humo: 3 casos (registro, login y logout con datos válidos)
- Suite de regresión: 13 casos (todos los del sprint, incluidos error/validación y retests de defectos corregidos)

## Registro

| ID     | Caso de prueba                                  | Precondiciones                           | Pasos y datos                                                                          | Resultado esperado                                         | Resultado obtenido                                                                      | Estado   | Suite            |
| ------ | ----------------------------------------------- | ---------------------------------------- | -------------------------------------------------------------------------------------- | ---------------------------------------------------------- | --------------------------------------------------------------------------------------- | -------- | ---------------- |
| Caso 1 | Registro con datos válidos                      | No existe un usuario con el email a usar | `POST /users/register` con nombre, apellido, DNI, email, teléfono y contraseña válidos | HTTP 201 con id, CVU y alias                               | HTTP 201 con id, CVU y alias                                                            | Aprobado | Humo y regresión |
| Caso 2 | Registro con campos obligatorios faltantes      | —                                        | `POST /users/register` sin nombre, apellido, DNI y contraseña                          | HTTP 400 con mensajes por cada campo                       | HTTP 400 con mensajes por cada campo                                                    | Aprobado | Regresión        |
| Caso 3 | Registro con email con formato inválido         | —                                        | `POST /users/register` con email sin formato válido                                    | HTTP 400                                                   | HTTP 400                                                                                | Aprobado | Regresión        |
| Caso 4 | Registro con email duplicado                    | Existe un usuario con el email a usar    | `POST /users/register` con un email ya registrado                                      | HTTP 400 indicando que el email ya está registrado         | HTTP 400 indicando que el email ya está registrado                                      | Aprobado | Regresión        |
| Caso 5 | Registro con contraseña débil                   | —                                        | `POST /users/register` con contraseña de 1 carácter                                    | HTTP 400 por política de contraseñas (mínimo 8, máximo 64) | HTTP 400 por política de contraseñas (defecto corregido; retest aprobado)               | Aprobado | Regresión        |
| Caso 6 | Registro con JSON malformado                    | —                                        | `POST /users/register` con cuerpo JSON malformado                                      | HTTP 400, error del cliente                                | HTTP 400 con el mensaje del handler (defecto corregido; retest aprobado)                | Aprobado | Regresión        |
| Caso 7 | Registro con caracteres especiales en el nombre | —                                        | `POST /users/register` con nombre que contiene `()`, `;`, `&`, `<`, `>`                | HTTP 400 con mensaje que indica el campo rechazado         | HTTP 400 con mensaje que indica el campo rechazado (defecto corregido; retest aprobado) | Aprobado | Regresión        |

## Login

| ID      | Caso de prueba                  | Precondiciones     | Pasos y datos                                               | Resultado esperado                        | Resultado obtenido                        | Estado   | Suite            |
| ------- | ------------------------------- | ------------------ | ----------------------------------------------------------- | ----------------------------------------- | ----------------------------------------- | -------- | ---------------- |
| Caso 8  | Login con credenciales válidas  | Usuario registrado | `POST /auth/login` con email y contraseña correctos         | HTTP 200 con access token y refresh token | HTTP 200 con access token y refresh token | Aprobado | Humo y regresión |
| Caso 9  | Login con usuario inexistente   | —                  | `POST /auth/login` con email no registrado                  | HTTP 404                                  | HTTP 404                                  | Aprobado | Regresión        |
| Caso 10 | Login con contraseña incorrecta | Usuario registrado | `POST /auth/login` con email válido y contraseña incorrecta | HTTP 400                                  | HTTP 400                                  | Aprobado | Regresión        |
| Caso 11 | Login con campos vacíos         | —                  | `POST /auth/login` con email y contraseña vacíos            | HTTP 400                                  | HTTP 400                                  | Aprobado | Regresión        |

## Logout

| ID      | Caso de prueba                  | Precondiciones  | Pasos y datos                                                         | Resultado esperado                   | Resultado obtenido                           | Estado   | Suite            |
| ------- | ------------------------------- | --------------- | --------------------------------------------------------------------- | ------------------------------------ | -------------------------------------------- | -------- | ---------------- |
| Caso 12 | Logout con refresh token válido | Sesión iniciada | `POST /user/logout` con el refresh token en el header `Authorization` | HTTP 200 con revocación de la sesión | HTTP 200; el refresh token queda inutilizado | Aprobado | Humo y regresión |
| Caso 13 | Logout sin token                | —               | `POST /user/logout` sin enviar token                                  | HTTP 400 — el logout exige token                                            | HTTP 200 (defecto corregido; retest aprobado)              | Aprobado | Regresión        |

## Suite de prueba ejecutable

La suite ejecutable es la colección de Postman `Digital-Money-House.postman_collection.json`: contiene un request por caso de prueba, con el mismo nombre que el caso, y se ejecuta en el siguiente orden:

1. Registro exitoso
2. Registro con email duplicado
3. Registro con campos obligatorios faltantes
4. Registro con email con formato inválido
5. Registro con contraseña débil
6. Registro con JSON malformado
7. Registro con caracteres especiales en el nombre
8. Login exitoso
9. Login usuario inexistente
10. Login contraseña incorrecta
11. Login con campos vacíos
12. Logout con refresh token
13. Logout sin token

La suite de humo se forma con los requests del camino feliz: Registro exitoso → Login exitoso → Logout con refresh token.

## Mantenimiento de la planilla

- Todo caso nuevo del sprint se agrega con los mismos campos y se clasifica en humo y/o regresión.
- Al corregir un defecto, se re-ejecuta su caso asociado y se actualiza el resultado obtenido y el estado.
- La planilla se ejecuta completa antes de cada entrega (regresión) y tras cada deploy o cambio de infraestructura (humo).
