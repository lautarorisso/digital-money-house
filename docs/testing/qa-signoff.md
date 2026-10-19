# QA SignOff — Sprints 1 a 4

## Resultado general

68 casos ejecutados: 62 aprobados y 6 fallidos inicialmente; los 6 fallidos aprobaron en el retest.

| Sprint                          | Ejecutados | Aprobados iniciales | Fallidos iniciales | Retest de fallidos | Defectos reportados / resueltos |
| ------------------------------- | ---------- | ------------------- | ------------------ | ------------------ | ------------------------------- |
| [1](sprint-1-testing-manual.md) | 14         | 9                   | 5                  | 5 aprobados        | 5 / 5                           |
| [2](sprint-2-testing-manual.md) | 22         | 21                  | 1                  | 1 aprobado         | 1 / 1                           |
| [3](sprint-3-testing-manual.md) | 19         | 19                  | 0                  | No aplica          | Sin registro de defectos        |
| [4](sprint-4-testing-manual.md) | 13         | 13                  | 0                  | No aplica          | 0 / 0                           |
| **Total**                       | **68**     | **62**              | **6**              | **6 aprobados**    | **6 / 6**                       |

Fallaron inicialmente los casos 5, 6, 7, 13 y 14 de Sprint 1 ([BUG-001 a BUG-005](sprint-1-testing-exploratorio.md#bugs-encontrados)) y el 17 de Sprint 2; todos aprobaron en el retest.

## Automatización

| Sprint    | Casos automatizados                                 |
| --------- | --------------------------------------------------- |
| 1         | 14 con scripts; ejecución automatizada sin registro |
| 2         | 22 ejecutados; caso 17 aprobado en retest           |
| 3         | 19 ejecutados y aprobados                           |
| 4         | 13 ejecutados y aprobados                           |
| **Total** | **54 ejecutados; 14 adicionales con scripts**       |

Suites adicionales, sin sumarlas a los 68 casos únicos: users-service **7/7**, Java Sprint 2 (`Sprint2SmokeIT`) **17/17** y Java Sprint 3 (`Sprint3ActivityIT`) **19/19** ejecutados/aprobados.

## Cierre

Los seis casos fallidos conservan su resultado inicial y su aprobación en retest. Los seis defectos reportados están resueltos.
