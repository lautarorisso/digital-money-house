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

## Filtros opcionales de actividad

Los casos 1 a 14 conservan sus resultados originales. Los casos 15 a 19 se ejecutaron el 2026-10-09 con la carpeta nativa Sprint 3 de Postman contra gateway, account-service y MySQL. La nueva ejecución preparó las cuentas 36 y 37 y reutilizó los ingresos de 1000.50, 250.00 y cuatro de 0.25.

`GET /accounts/{id}/activity` admite `from` y `to` en formato ISO `YYYY-MM-DD` (años 0001 a 9999), `type=CREDIT|DEBIT`, `minAmount` y `maxAmount`, todos opcionales y combinados con AND en la consulta de base de datos. Las fechas incluyen días completos según `transactionDate`, sin conversión de zona horaria: desde las 00:00 de `from` hasta antes de las 00:00 del día siguiente a `to`. Para `to=9999-12-31` no se aplica límite superior, porque MySQL no almacena fechas posteriores; años fuera del rango devuelven 400. Sin filtros se conserva el historial completo y su orden `transactionDate DESC, id DESC`.

Los montos comparan la magnitud `abs(amount)`: mínimo exclusivo y máximo inclusivo. Los rangos son `[0,1000]`, `(1000,5000]`, `(5000,20000]`, `(20000,100000]` y `>100000`. Nota: `minAmount=0` incluye también un eventual movimiento de monto cero para representar literalmente el primer rango; los depósitos actuales son siempre positivos.

### Caso 15 — Filtros combinados

**Tour:** Intersección de filtros
**Escenario:** consultar la actividad propia con `from=2026-10-09&to=2026-10-09&type=CREDIT&minAmount=1000&maxAmount=5000`. Las fechas se toman de `transactionDate` de los ingresos creados; repetir con `type=DEBIT` y los mismos límites de monto.
**Resultado esperado:** HTTP 200 con solo el ingreso de 1000.50 para CREDIT y lista vacía para DEBIT.
**Resultado obtenido:** HTTP 200 con el movimiento esperado para CREDIT; HTTP 200 con `[]` para DEBIT. Los ingresos desde tarjeta DEBIT son movimientos CREDIT, no egresos.

### Caso 16 — Días inclusivos y límites de fecha opcionales

**Tour:** Límites de calendario
**Escenario:** consultar desde el día del primer ingreso hasta el día del último, luego solo `from`, solo `to`, `to=9999-12-31`, `to` del día anterior al primero y `from` del día posterior al último. Las fechas se obtienen de las respuestas, no del reloj local.
**Resultado esperado:** HTTP 200 con los seis movimientos en orden descendente para los primeros cuatro pedidos; lista vacía para los dos pedidos fuera del intervalo.
**Resultado obtenido:** HTTP 200 con los seis IDs esperados en orden para los cuatro pedidos inclusivos; HTTP 200 con `[]` para ambos pedidos fuera del intervalo. La fecha máxima admitida no produjo 500.

### Caso 17 — Límites de monto y resultados vacíos

**Tour:** Fronteras de rangos
**Escenario:** consultar `minAmount=250`, `maxAmount=250`, `minAmount=0&maxAmount=1000`, límites iguales de 250, los tres rangos superiores sin ingresos y `type=DEBIT` sin otros filtros.
**Resultado esperado:** el mínimo excluye 250.00 y deja solo 1000.50; el máximo incluye 250.00 y los cuatro ingresos de 0.25; el primer rango contiene esos cinco movimientos. Límites iguales positivos, rangos superiores y DEBIT devuelven lista vacía.
**Resultado obtenido:** HTTP 200 con los IDs exactos y en orden en las ocho variantes. No se crearon ingresos adicionales ni egresos artificiales para probar los límites.

### Caso 18 — Filtros inválidos

**Tour:** Datos inválidos
**Escenario:** enviar fecha malformada, fecha imposible `2026-02-30`, fechas invertidas, fecha fuera de rango `+999999999-12-31`, tipo UNKNOWN, mínimo negativo, máximo negativo, mínimo mayor que máximo y monto no numérico.
**Resultado esperado:** HTTP 400 en las nueve variantes, sin modificar saldos ni registrar movimientos.
**Resultado obtenido:** HTTP 400 con cuerpo de error en las nueve variantes. Las comprobaciones posteriores mantuvieron el saldo propio de 1251.50, los seis movimientos y la cuenta secundaria vacía con saldo cero.

### Caso 19 — Actividad ajena con filtros

**Tour:** Permisos
**Escenario:** consultar la actividad de la cuenta 37 con el token de la cuenta 36 y filtros válidos de fecha, DEBIT y monto `(1000,5000]`, aunque esos filtros no tengan coincidencias.
**Resultado esperado:** HTTP 403, no una lista vacía que omita el control de propiedad.
**Resultado obtenido:** HTTP 403 sin entregar movimientos de la cuenta ajena.

## Verificación de filtros

- `./mvnw -B -ntp -pl services/account-service -DskipTests package`: compilación y empaquetado correctos.
- `docker compose up -d --no-deps --build account-service`: solo account-service reconstruido y recreado, sin reconstruir dependencias.
- `./mvnw -B -ntp -pl services/api-gateway -Dtest=Sprint3ActivityIT test`: 19 tests, 0 fallos, 0 errores, 0 omitidos; conserva los 14 casos originales y agrega cinco grupos.
- `postman collection run docs/postman/Digital-Money-House.postman_collection.json -e docs/postman/Digital-Money-House.postman_environment.json -i 'Sprint 3' --no-report-events --disable-unicode`: 61 requests y 122 assertions, sin fallos; incluye preparación, variantes y limpieza de tarjetas. No requiere publicación en Postman cloud.
- `./mvnw -B -ntp test`: 13 tests de regresión existentes, sin fallos, errores ni omitidos. Este comando no selecciona las clases `*IT`; Sprint3ActivityIT se ejecutó explícitamente por separado.

Limitaciones: no hay egresos reales ni movimientos cero en estos datos; no se verifica con fixtures la magnitud de un futuro monto negativo ni una transacción exactamente a medianoche. Los límites exactos de monto se prueban con 250.00, sin agregar depósitos en cada frontera de los rangos. La consulta JPQL evita filtrado en memoria y nuevas abstracciones; no se midió su rendimiento en historiales grandes ni se agregaron índices.

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
