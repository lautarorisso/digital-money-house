# Testing exploratorio — Sprint 3

## Alcance

Se realizó testing exploratorio sobre:

- Historial de actividad de la cuenta
- Detalle de una actividad
- Ingreso de dinero desde tarjetas de crédito y débito

## Mi actividad

### Caso 1 — Historial de una cuenta sin movimientos

**Tour:** Cuenta nueva
**Escenario:** consultar `GET /accounts/12/activity` con el token del propietario antes de ingresar dinero.
**Resultado esperado:** HTTP 200 con lista vacía.
**Resultado obtenido:** HTTP 200 con `[]`.

### Caso 2 — Historial completo desde el más reciente

**Tour:** Historial y orden
**Escenario:** después de los ingresos de los casos 9 y 10, registrar cuatro ingresos adicionales de 0.25 y consultar `GET /accounts/12/activity`.
**Resultado esperado:** HTTP 200 con los seis movimientos, desde el más reciente al más antiguo.
**Resultado obtenido:** HTTP 200 con los IDs 12, 11, 10, 9, 8 y 7, en orden de fecha descendente.

### Caso 3 — Historial de una cuenta ajena

**Tour:** Permisos
**Escenario:** consultar `GET /accounts/13/activity` con el token del propietario de la cuenta 12.
**Resultado esperado:** HTTP 403.
**Resultado obtenido:** HTTP 403.

### Caso 4 — Historial con ID inválido

**Tour:** Datos inválidos
**Escenario:** consultar `GET /accounts/invalid/activity` con un token válido.
**Resultado esperado:** HTTP 400 por el parámetro no numérico.
**Resultado obtenido:** HTTP 400 con `Invalid path parameter`.

## Detalle de actividad

### Caso 5 — Detalle de una actividad propia

**Tour:** Datos válidos
**Escenario:** consultar `GET /accounts/12/activity/7` con el token del propietario.
**Resultado esperado:** HTTP 200 con los datos de la transacción seleccionada.
**Resultado obtenido:** HTTP 200.

### Caso 6 — Transferencia inexistente

**Tour:** Recurso inexistente
**Escenario:** consultar `GET /accounts/12/activity/9223372036854775807` con el token del propietario.
**Resultado esperado:** HTTP 404.
**Resultado obtenido:** HTTP 404.

### Caso 7 — Detalle de una cuenta ajena

**Tour:** Permisos
**Escenario:** consultar `GET /accounts/13/activity/7` con el token del propietario de la cuenta 12.
**Resultado esperado:** HTTP 403 por no tener acceso a la cuenta indicada.
**Resultado obtenido:** HTTP 403.

### Caso 8 — Transferencia existente fuera de la cuenta propia

**Tour:** Asociación del recurso
**Escenario:** consultar `GET /accounts/13/activity/7` con el token del propietario de la cuenta 13. La transferencia 7 pertenece a la cuenta 12.
**Resultado esperado:** HTTP 404 sin entregar datos de la transferencia de otra cuenta.
**Resultado obtenido:** HTTP 404.

## Ingreso de dinero

### Caso 9 — Ingreso desde tarjeta de crédito

**Tour:** Datos válidos
**Escenario:** enviar `POST /accounts/12/transferences` con tarjeta CREDIT asociada, `cardId: 13` y `amount: 1000.50`.
**Resultado esperado:** HTTP 201, saldo acreditado y nuevo movimiento disponible en la actividad.
**Resultado obtenido:** HTTP 201.

### Caso 10 — Ingreso desde tarjeta de débito

**Tour:** Datos válidos
**Escenario:** enviar `POST /accounts/12/transferences` con tarjeta DEBIT asociada, `cardId: 14` y `amount: 250.00`.
**Resultado esperado:** HTTP 201 y aumento del saldo a 1250.50.
**Resultado obtenido:** HTTP 201.

### Caso 11 — Monto cero o negativo

**Tour:** Datos inválidos
**Escenario:** intentar ingresar dinero con la tarjeta asociada y montos `0` y `-10`.
**Resultado esperado:** HTTP 400 en ambos intentos, sin acreditar saldo ni registrar movimientos.
**Resultado obtenido:** HTTP 400.

### Caso 12 — Ingreso a cuenta inexistente

**Tour:** Recurso inexistente
**Escenario:** enviar `POST /accounts/9223372036854775807/transferences` con token válido, tarjeta existente y monto 10.
**Resultado esperado:** HTTP 404.
**Resultado obtenido:** HTTP 404.

### Caso 13 — Tarjeta no asociada a la cuenta

**Tour:** Asociación del recurso
**Escenario:** intentar ingresar 10 en la cuenta 12 con `cardId: 15`, tarjeta asociada a la cuenta 13.
**Resultado esperado:** HTTP 404 sin acreditar saldo.
**Resultado obtenido:** HTTP 404. Los saldos de ambas cuentas se mantienen.

### Caso 14 — Ingreso a una cuenta ajena

**Tour:** Permisos
**Escenario:** enviar `POST /accounts/13/transferences` con el token del propietario de la cuenta 12, tarjeta 15 y monto 10.
**Resultado esperado:** HTTP 403 sin modificar la cuenta ajena.
**Resultado obtenido:** HTTP 403.

## Workflow principal

→ Registro de Lautaro Risso
→ Login
→ Asociación de tarjetas
→ Consulta de actividad vacía
→ Ingreso desde CREDIT y DEBIT
→ Consulta de historial completo
→ Consulta de detalle

## Bugs encontrados

No se reprodujeron defectos en los 14 casos explorados.

## Conclusión

Se exploraron las funciones agregadas durante el sprint: historial completo, detalle e ingreso de dinero. Los ingresos por ambas clases de tarjeta actualizan el saldo y aparecen en la actividad; los recursos ajenos, inexistentes y los datos inválidos se rechazan con los códigos esperados.
