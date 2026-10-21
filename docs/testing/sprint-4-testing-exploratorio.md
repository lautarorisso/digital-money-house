# Testing exploratorio — Sprint 4

## Alcance

Transferencias por CVU y alias, destinatarios recientes, validaciones, permisos y concurrencia.

## Transferencias

### Escenario 1 — Transferencias y destinatarios recientes

**Escenario:** depositar 100 y realizar siete transferencias de 1 a seis destinos distintos, repitiendo uno; consultar destinatarios y actualizar su alias.
**Resultado esperado:** HTTP 200; débitos y créditos correctos, cinco destinos únicos por uso más reciente y alias actual.
**Resultado obtenido:** HTTP 200; límite, orden, unicidad y alias correctos, total de saldos conservado.

### Escenario 2 — Datos inválidos y permisos

**Escenario:** operar sin autorización, sobre cuenta ajena, con destino o monto inválido y sin fondos.
**Resultado esperado:** HTTP 401, 403, 400 y 410 respectivamente; saldos y movimientos sin cambios.
**Resultado obtenido:** códigos esperados y ausencia de efectos en ambas cuentas.

### Escenario 3 — Transferencias concurrentes

**Escenario:** enviar transferencias opuestas en paralelo y luego dos débitos que compiten por fondos insuficientes para ambos.
**Resultado esperado:** ambas opuestas aceptadas; en competencia, una aceptada y otra rechazada, sin saldo negativo ni creación de dinero.
**Resultado obtenido:** HTTP 200/200 y 200/410 respectivamente, total de saldos conservado en 100.

## Conclusión

Transferencias, destinatarios, validaciones y escenarios concurrentes con los resultados esperados.
