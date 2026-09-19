# Plan de pruebas (Testing kickoff) — Sprint 1

## 1. Cómo escribir un caso de prueba

Todo caso de prueba se documenta con estos campos:

| Campo              | Descripción                                                 |
| ------------------ | ----------------------------------------------------------- |
| ID                 | identificador y un título que resuma qué se prueba          |
| Tour               | categoría o tipo de prueba a la que pertenece               |
| Escenario          | qué se prueba y con qué datos concretos                     |
| Resultado esperado | la respuesta y el comportamiento correcto                   |
| Resultado obtenido | lo que ocurrió al ejecutar                                  |
| Hallazgo / Estado  | (solo si falla) referencia al defecto reportado y su estado |

Reglas:

- Un caso cubre un escenario; el resultado esperado se define antes de ejecutar y el obtenido se completa durante la ejecución.
- Si el resultado obtenido contradice al esperado, se reporta un defecto con la ficha de la sección 2.

## 2. Cómo reportar un defecto

Todo defecto se documenta con estos campos:

| Campo              | Descripción                                                            |
| ------------------ | ---------------------------------------------------------------------- |
| ID                 | identificador                                                          |
| Tipo               | categoría del problema                                                 |
| Descripción        | qué falla                                                              |
| Resultado esperado | lo que debería ocurrir                                                 |
| Resultado obtenido | lo que ocurre realmente                                                |
| Severidad          | Alta / Media / Baja                                                    |
| Estado             | ciclo de vida del defecto (abierto, en corrección, corregido, cerrado) |
| Corrección         | qué se cambió para resolverlo                                          |
| Retest             | cómo se validó la corrección y qué devolvió                            |

Reglas:

- El defecto se reporta solo cuando se reproduce, y siempre que el resultado obtenido contradiga al esperado.
- Severidad: Alta cuando bloquea el flujo principal o compromete datos; Media cuando es un comportamiento incorrecto; Baja cuando es cosmético o de mensaje.
- Cada corrección se valida con su retest; el defecto se cierra cuando el retest confirma la corrección.

## 3. Criterios para la suite de humo

La suite de humo incluye los caminos felices de punta a punta: un caso por flujo principal del producto (registro, acceso y cierre de sesión).

Se ejecuta tras cada deploy o cambio de infraestructura, para confirmar en pocos minutos que el sistema funciona.

## 4. Criterios para la suite de regresión

La suite de regresión incluye todos los casos aprobados del sprint, incluidos los de error y validación, más los retests de los defectos corregidos.

Se ejecuta completa antes de cada entrega; al corregir un defecto se re-ejecuta al menos el caso asociado. Su objetivo es confirmar que un cambio no rompió funcionalidad ya aprobada.
