# TDD en la Semana 6: ciclos rojo, verde y refactor

En cada ciclo primero se escribió la prueba (que debía fallar), después se hizo la implementación mínima para que pasara, y al final se limpió el código sin cambiar el comportamiento.

| Ciclo | Rojo: prueba escrita primero | Verde: implementación mínima | Refactor |
|---|---|---|---|
| 1. Estados cerrados | `EstadoCitaTest`: acepta `Agendada`, `cancelada` o `" Completada "`; rechaza `Pendiente`, vacío y nulo | `EstadoCita.desdeTexto` | Se eliminan las constantes de texto (`"Agendada"`, `"Cancelada"`) de `CitaServiceImpl` |
| 2. Cancelar por estado | `CitaServiceImplTest.cancelarCita_propietario_cambiaElEstadoYConservaElRegistro` y `CitaCancelacionIntegrationTest` ("cancelo y reagendo en el mismo horario") | `cancelarCita`: `setEstado(CANCELADA)` y `save`, sin `deleteById` | Las pruebas de error de cancelación pasan a verificar que no se guarda nada |
| 3. Restricción de horario | `CitaRestriccionHorarioIntegrationTest` y `CitaServiceImplTest.agendarCita_choqueDeHorarioEnLaBase_lanzaError` | `Cita.horarioActivo` (columna única, nula si la cita está cancelada) y `guardarSinChoqueDeHorario` | El mismo método protege el agendamiento y la reactivación de una cita |
| 4. Expediente completo | `ExpedienteMedicoServiceImplTest`: faltan descripción, diagnóstico o tratamiento; el firmante no es veterinario | `validarRegistroCompleto` y la comprobación de rol en `ExpedienteMedicoServiceImpl` | `ExpedienteMedicoServiceImplSecurityTest` usa un registro completo y un veterinario con rol |

## Cómo reproducir el rojo y el verde

1. Crear la rama: `git checkout -b semana6-tdd`.
2. **Rojo.** Copiar el contenido del paquete `rojo` sobre el proyecto y ejecutar `.\mvnw.cmd test`. Fallan las pruebas nuevas de `EstadoCitaTest`, `CitaServiceImplTest` y `ExpedienteMedicoServiceImplTest`. Con `.\mvnw.cmd verify` también fallan las de `CitaCancelacionIntegrationTest` y `CitaRestriccionHorarioIntegrationTest`. Guardar la captura y hacer el commit `TDD rojo: pruebas de la semana 6`.
3. **Verde.** Copiar el contenido del paquete `verde` y ejecutar `.\mvnw.cmd clean verify`. Debe terminar en `BUILD SUCCESS`. Guardar la captura y hacer el commit `TDD verde: implementación mínima`.
4. **Refactor.** Cualquier limpieza posterior va en un tercer commit, con la suite en verde.

Los commits quedan en el historial de GitHub como evidencia del ciclo.
