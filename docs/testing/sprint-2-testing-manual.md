# Planilla de casos de prueba (Testing manual) — Sprint 2

## Cómo ejecutar

1. Levantar el proyecto con `docker compose up -d --build` y esperar que los servicios estén disponibles.
2. Importar la colección y el environment local de `docs/postman/` en Postman.
3. Seleccionar el environment y pulsar **Run** sobre la carpeta **Sprint 2** completa.

Postman registra dos usuarios, inicia sesión y guarda los datos necesarios automáticamente. No hay que escribir credenciales ni IDs. La cuenta principal se usa para los casos; la segunda permite comprobar permisos. La preparación verifica las cuentas por CVU y alias antes de continuar; si no coinciden, detiene la ejecución.

## Resultado general

- Total de casos: 16
- Probados: 16/16
- Fallidos: 0
- Suite de Humo: casos 15–23 y 28–29 (11 casos)
- Suite de Regresión: 16 casos
- Los seis requests iniciales son preparación, no casos nuevos

## Dashboard y perfil

| ID      | Caso de prueba                   | Precondiciones         | Pasos y datos                                               | Resultado esperado                                  | Resultado obtenido                                   | Estado   | Suite            |
| ------- | -------------------------------- | ---------------------- | ----------------------------------------------------------- | --------------------------------------------------- | ---------------------------------------------------- | -------- | ---------------- |
| Caso 15 | Consultar saldo, CVU y alias     | Preparación automática | `GET /accounts-service/accounts/{{accountId}}`              | HTTP 200 con datos de la cuenta propia y saldo cero | HTTP 200; CVU y alias propios, saldo 0.00            | Aprobado | Humo y regresión |
| Caso 16 | Consultar cuenta sin movimientos | Cuenta nueva           | `GET /accounts-service/accounts/{{accountId}}/transactions` | HTTP 200 con lista vacía                            | HTTP 200 con `[]`                                    | Aprobado | Humo y regresión |
| Caso 17 | Consultar perfil propio          | Preparación automática | `GET /users-service/users/{{userId}}`                       | HTTP 200 con datos personales, sin credenciales     | HTTP 200 con datos propios, sin contraseña ni tokens | Aprobado | Humo y regresión |

## Tarjetas

| ID      | Caso de prueba                      | Precondiciones                           | Pasos y datos                                                   | Resultado esperado                            | Resultado obtenido                              | Estado   | Suite            |
| ------- | ----------------------------------- | ---------------------------------------- | --------------------------------------------------------------- | --------------------------------------------- | ----------------------------------------------- | -------- | ---------------- |
| Caso 18 | Listar tarjetas sin asociaciones    | Cuenta nueva                             | `GET /cards/accounts/{{accountId}}/cards`                       | HTTP 200 con lista vacía                      | HTTP 200 con `[]`                               | Aprobado | Humo y regresión |
| Caso 19 | Crear tarjeta de crédito            | Número generado automáticamente          | `POST /cards`, tipo `credit`                                    | HTTP 201 con ID y CREDIT, sin número completo | HTTP 201 con ID y CREDIT                        | Aprobado | Humo y regresión |
| Caso 20 | Asociar tarjeta existente           | Caso 19                                  | `POST /accounts/{{accountId}}/cards`, mismo número              | HTTP 201; conserva la tarjeta                 | HTTP 201; mismo ID, asociación confirmada       | Aprobado | Humo y regresión |
| Caso 21 | Crear y asociar tarjeta de débito   | Número generado automáticamente          | `POST /accounts/{{accountId}}/cards`, tipo `dEbIt`              | HTTP 201 con ID y DEBIT                       | HTTP 201; tarjeta creada y asociada             | Aprobado | Humo y regresión |
| Caso 22 | Listar tarjetas asociadas           | Casos 20 y 21                            | `GET /cards/accounts/{{accountId}}/cards`                       | HTTP 200 con ambas tarjetas                   | HTTP 200; CREDIT y DEBIT, sin números completos | Aprobado | Humo y regresión |
| Caso 23 | Consultar detalle de tarjeta propia | Tarjeta de crédito asociada              | `GET /cards/accounts/{{accountId}}/cards/{{creditCardId}}`      | HTTP 200 con ID y tipo correctos              | HTTP 200 con ID y CREDIT                        | Aprobado | Humo y regresión |
| Caso 24 | Crear tarjeta con número inválido   | Usuario autenticado                      | `POST /cards`, número `123` y DEBIT                             | HTTP 400                                      | HTTP 400; número de tarjeta inválido            | Aprobado | Regresión        |
| Caso 25 | Repetir asociación de tarjeta       | Tarjeta ya asociada                      | Repetir `POST /accounts/{{accountId}}/cards`                    | HTTP 409                                      | HTTP 409; tarjeta ya asociada                   | Aprobado | Regresión        |
| Caso 26 | Consultar tarjetas de cuenta ajena  | Segunda cuenta preparada automáticamente | `GET /cards/accounts/{{otherAccountId}}/cards`, token principal | HTTP 403                                      | HTTP 403; acceso rechazado                      | Aprobado | Regresión        |
| Caso 27 | Consultar tarjeta inexistente       | Cuenta propia                            | Consultar tarjeta `9223372036854775807`                         | HTTP 404                                      | HTTP 404; tarjeta no encontrada                 | Aprobado | Regresión        |
| Caso 28 | Eliminar tarjeta de débito          | Tarjeta creada por la suite              | `DELETE /cards/accounts/{{accountId}}/cards/{{debitCardId}}`    | HTTP 200 sin cuerpo                           | HTTP 200 sin cuerpo                             | Aprobado | Humo y regresión |
| Caso 29 | Eliminar tarjeta de crédito         | Tarjeta creada por la suite              | `DELETE /cards/accounts/{{accountId}}/cards/{{creditCardId}}`   | HTTP 200 sin cuerpo                           | HTTP 200 sin cuerpo                             | Aprobado | Humo y regresión |
| Caso 30 | Consultar tarjeta eliminada         | Caso 29                                  | Volver a consultar la tarjeta de crédito                        | HTTP 404                                      | HTTP 404; eliminación confirmada                | Aprobado | Regresión        |

## Suite y mantenimiento

La carpeta **Sprint 2** contiene la preparación y estos 16 casos, en orden. Captura tokens e IDs y elimina las tarjetas al finalizar. Cada ejecución crea usuarios y números de tarjeta nuevos, por lo que no depende de la ejecución de Sprint 1.
