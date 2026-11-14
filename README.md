# Backend Clínica PetSalud

API REST con Spring Boot 3, Spring Security con autenticación JWT (stateless), JPA y MySQL.

## Novedades de la Semana 3 (rama `semana3-seguridad`)

| Criterio de la pauta | Qué se agregó | Dónde |
|---|---|---|
| 1 y 7. Análisis e informe | Amenazas, requerimientos, matriz de roles, estrategias, guía y explicación técnica | `docs/semana3/INFORME_SEMANA3.md` |
| 2. Configuración | Monitoreo de actividad sospechosa con log en archivo, cabeceras de seguridad y CORS restringido | `ActividadSospechosaService`, `AuditoriaRequestFilter`, `SecurityConfig` |
| 3. JWT | El token incluye y exige la audiencia (`aud=petsalud-api`) además del emisor | `JwtUtil` |
| 4. Roles | `@PreAuthorize` en los 43 endpoints; eliminar expedientes queda solo para ADMIN | `ReglasAcceso`, controladores |
| 5. Amenazas | DTO validados (sin `<`, `>`, comillas ni asignación masiva) y pruebas de SQL Injection, XSS y CSRF | `dto/`, `AmenazasComunesTest`, `InyeccionSqlRepositorioTest`, colección Postman de la Semana 3 |
| 6. GitHub | Este README, el informe y las evidencias | `docs/semana3/` |

Resultado: **97 pruebas automáticas sin fallos** (42 nuevas) y 23 pruebas manuales contra la API con base de datos real (`docs/semana3/evidencias/`).

## Novedades de la Semana 4 (rama `semana4-pruebas`): pruebas unitarias

| Requerimiento | Qué se agregó | Dónde |
|---|---|---|
| Gestión de ventas | Solo se registra una venta si **todos** los productos tienen stock (un stock nulo cuenta como sin stock); el stock se descuenta únicamente cuando la venta completa es válida. El total es la suma de los precios | `VentaServiceImpl` |
| Validación de usuarios | El usuario titular de la venta debe tener nombre, apellido, email y dirección, y rol `ADMIN` o `CLIENTE` (un `VETERINARIO` recibe acceso denegado) | `VentaServiceImpl` |
| Relación Producto-Proveedor | Nueva entidad `Proveedor` y relación `@ManyToOne` desde `Producto` | `Proveedor`, `Producto` |
| Simulación de dependencias | Mockito reemplaza los repositorios y `AutorizacionService`; no se usa MySQL | `VentaServiceImplTest`, `UsuarioServiceImplTest` |
| Cobertura | JaCoCo genera el informe y `verify` falla si `VentaServiceImpl` o `UsuarioServiceImpl` bajan de 80 % de líneas | `pom.xml` |

Herramientas: JUnit 5, Mockito (ambas incluidas en `spring-boot-starter-test`, versiones administradas por Spring Boot) y JaCoCo 0.8.12.

### Cómo ejecutar las pruebas de la Semana 4

Solo las pruebas unitarias (no necesitan MySQL ni variables de entorno):

```powershell
.\mvnw.cmd test "-Dtest=VentaServiceImplTest,UsuarioServiceImplTest,UsuarioTest,VentaControllerTest"
```

Suite sin la prueba de contexto (ejecucion parcial), con comprobacion JaCoCo del 80 %:

```powershell
.\mvnw.cmd verify "-Dtest=!BackendpetsaludApplicationTests"
```

Suite completa con comprobacion JaCoCo del 80 % (la prueba de contexto usa H2; no requiere servidor MySQL durante las pruebas, aunque pueden necesitarse variables de entorno de seguridad):
```powershell
.\mvnw.cmd verify
```


El informe de cobertura queda en `target/site/jacoco/index.html`.

### Registro tecnico de pruebas y cobertura

El inventario detallado de las pruebas, sus escenarios, resultados y porcentajes de cobertura se encuentra en [Registro de pruebas y cobertura - Semana 4](docs/semana4/Registro_Pruebas_Cobertura_S4.md).

El documento consolida 19 pruebas unitarias de Venta, 11 de Usuario y 1 prueba de integracion de rollback con H2. Tambien incluye las mediciones historicas y finales de JaCoCo y las referencias a las evidencias. Estas 31 pruebas forman parte de la bateria completa de 133 pruebas aprobadas; el registro no las presenta a todas como nuevas.

### Integridad transaccional de ventas

El metodo `realizarVenta()` de `VentaServiceImpl` utiliza `@Transactional` para revertir los cambios de stock si ocurre una excepcion durante la operacion. Antes de descontar existencias, comprueba que todos los productos tengan stock disponible, considerando `null` como stock no disponible.

La prueba de regresion `realizarVenta_segundoProductoSinStock_noDescuentaPrimero` comprueba que un producto sin stock impida descontar los anteriores. La prueba `realizarVenta_stockNulo_lanzaError` verifica el rechazo del stock nulo sin guardar descuentos ni ventas.

La prueba de integracion `VentaRollbackIntegrationTest`, mediante `errorAlGuardarVenta_revierteDescuentosEnBaseDeDatos`, utiliza una base H2 aislada y simula un fallo al guardar la venta. Se verifico que las existencias de los dos productos vuelven a sus valores iniciales (5 y 3). Esta comprobacion no necesita MySQL.

| Clase de prueba | Qué comprueba |
|---|---|
| `VentaServiceImplTest` | Total de la venta, stock insuficiente o nulo (sin descontar stock de ningún producto), datos incompletos del usuario, roles permitidos y no permitidos, relación Venta-Usuario, Venta-Producto y Producto-Proveedor, y las consultas de ventas |
| `UsuarioServiceImplTest` | Registro (clave cifrada y rol `CLIENTE`), email repetido, actualización sin cambiar email ni clave, consulta, listado y eliminación |
| `UsuarioTest` | Construcción y comparación de la entidad `Usuario` |
| `VentaControllerTest` | Códigos HTTP y JSON del endpoint `/api/ventas` con el servicio simulado |

## 1. Requisitos

| Herramienta | Detalle |
|---|---|
| JDK | 17 o 21 (recomendado). Ya que se utilizó una versión superior se configura Lombok en el `pom.xml` |
| MySQL | Servidor encendido y accesible |
| Postman | Para probar los endpoints |
| Maven | No se instala: el proyecto incluye el Maven Wrapper (`mvnw.cmd`) |

Para comprobar Java, ejecuta `java -version` en PowerShell o en la terminal de vscode.

## 2. Configuración de la base de datos

Editar `src/main/resources/application.properties`. Se configuran los datos de user y pw segun las variables de entornos de los participantes, al gestionar la base de datos de no existir se crea para trabajar.

```properties
spring.application.name=backendpetsalud
spring.datasource.url=jdbc:mysql://localhost:3307/petsalud_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

jwt.secret=${JWT_SECRET}
jwt.expiration-ms=900000
jwt.issuer=PetSalud

login.max-attempts=5
login.lock-minutes=5

# Semana 3
jwt.audience=petsalud-api
logging.file.name=logs/petsalud.log
seguridad.monitor.max-solicitudes=200
seguridad.monitor.ventana-minutos=15
cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

Los valores `${...}` no se escriben en el archivo: Spring los lee de las variables de entorno del equipo.

| Variable | Se usa en | Qué contiene |
|---|---|---|
| `${DB_USERNAME}` | `spring.datasource.username` | Usuario de MySQL |
| `${DB_PASSWORD}` | `spring.datasource.password` | Contraseña de MySQL |
| `${JWT_SECRET}` | `jwt.secret` | Clave de firma de los JWT (Base64, mínimo 32 bytes) |

- El proyecto está configurado con el puerto `3307`. Cámbialo (por ejemplo a `3306`) si tu MySQL usa otro.
- Las variables `${DB_USERNAME}`, `${DB_PASSWORD}` y `${JWT_SECRET}` deben existir como variables de entorno **antes de ejecutar la aplicación y en la misma terminal (o configuración de ejecución del IDE) desde donde se ejecuta**. Sin `${JWT_SECRET}` la aplicación no arranca.
- La duración del token se define en `jwt.expiration-ms` (por defecto 15 minutos) y el emisor en `jwt.issuer`.
- El bloqueo del login se configura con `login.max-attempts` (5 intentos) y `login.lock-minutes` (5 minutos).
- `createDatabaseIfNotExist=true` crea la base `petsalud_db` si no existe. Las tablas las crea Hibernate al iniciar.

## 3. Cómo ejecutar

1. Abre la terminal (powershell o vscode) en la carpeta del proyecto (la que contiene `mvnw.cmd`).
2. Configura primero las variables de entorno en esta misma terminal. Reemplaza `tu_clave_mysql` por tu contraseña local; se generará una clave JWT aleatoria de 32 bytes en Base64:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="tu_clave_mysql"
$b = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
$env:JWT_SECRET=[Convert]::ToBase64String($b)
```

3. Ejecuta con el perfil `prueba`, que crea los usuarios de prueba:

```powershell
.\mvnw.cmd clean spring-boot:run "-Dspring-boot.run.profiles=prueba"
```

- La primera vez tarda unos minutos porque descarga Maven y las dependencias.
- Está listo cuando aparece una línea como `Started BackendpetsaludApplication`.
- La API queda en `http://localhost:8080`.

Para detenerla, presiona `Ctrl + C` en la terminal.

### Si la aplicación no arranca

Si aparece un error de conexión a MySQL, verifica que el servidor esté encendido, que el puerto configurado (3307 o 3306, según el equipo) sea correcto y que las credenciales coincidan con las variables definidas en el paso 3.

## 4. Usuarios de prueba (perfil `prueba`)

Se crean automáticamente al iniciar, si no existen. La contraseña de los tres es `Petsalud2026!`.

| Usuario (email) | Rol | Equivale a |
|---|---|---|
| `admin@petsalud.cl` | ADMIN | Gerente |
| `vet@petsalud.cl` | VETERINARIO | Empleado |
| `cliente@petsalud.cl` | CLIENTE | Cliente |

## 5. Pruebas en Postman

### Cómo configurar cada petición

1. Crea una petición nueva (**New → HTTP**) con el método y la URL indicados (De no funcionar localhost:8080, probar 127.0.0.1:8080)
2. Para autenticarte, primero haz login: `POST http://localhost:8080/api/auth/login` con Body → raw → JSON, por ejemplo `{"email": "{{correoPrueba}}", "password": "{{clavePrueba}}"}`. La respuesta trae `{"token": "..."}`.
3. En las demás peticiones, pestaña **Authorization**: tipo **Bearer Token** y pega el token (o usa una variable, ver más abajo). Para probar sin credenciales, deja el tipo en **No Auth**.
4. Para las peticiones POST con datos: pestaña **Body → raw → JSON**.
5. Presiona **Send** y revisa el código de estado (arriba a la derecha de la respuesta). Guarda una captura de cada prueba para el informe.

Se recomienda crear una **Collection** llamada "PetSalud" y guardar ahí las pruebas.

**Colección Postman disponible:** `postman/PetSalud_Semana2.postman_collection.json`. Se importa desde Postman mediante **Import** y contiene 35 solicitudes organizadas en 11 carpetas (`00` a `10`), correspondientes a autenticación JWT, controles sin autenticación, usuarios, mascotas, citas, expedientes, productos, ventas, notificaciones y recompensas, además de la carpeta `10 - Evidencias del informe` (Figuras 4, 8, 10, 11, 14 y 17).

La colección utiliza variables como `baseUrl`, `correoPrueba`, `clavePrueba`, `jwtToken` e identificadores de recursos. Las credenciales y los tokens no se incluyen en el archivo exportado; deben configurarse localmente. Primero se ejecuta el login para obtener un JWT y luego las solicitudes correspondientes. Algunas consultas requieren datos previamente existentes en MySQL.

Las solicitudes de las Figuras 18 y 19 permiten comprobar manualmente el rechazo de una firma JWT alterada y de un token vencido. Para la Figura 19 se requiere configurar temporalmente una vigencia de 60 segundos y reutilizar el mismo token antes y después de su expiración. No se ha validado una ejecución automática conjunta de las 35 solicitudes, por lo que deben comprobarse individualmente según sus requisitos.

### Uso del token JWT

La colección incluye la solicitud `01 - Login JWT`, ubicada en `00 - Autenticación JWT`.

Antes de ejecutarla, configura las variables de colección `correoPrueba` y `clavePrueba` con las credenciales del usuario de prueba correspondiente.

Al realizar un login exitoso, el script de Postman guarda automáticamente el JWT en la variable de colección `jwtToken`.

Las solicitudes protegidas utilizan **Authorization → Bearer Token** con el valor `{{jwtToken}}`.

Para comprobar permisos de otro rol, inicia sesión con las credenciales correspondientes y obtén un nuevo token. La vigencia normal es de 15 minutos.

Las pruebas de las Figuras 18 y 19 utilizan variables adicionales (`jwtTokenAlterado` y `jwtTokenFijo19`), descritas en sus solicitudes.

### Orden recomendado para reproducir las evidencias

Antes de comenzar, iniciar PETSALUD con la base de datos disponible, importar la colección y comprobar `baseUrl` (por defecto `http://localhost:8080`). Configurar localmente `correoPrueba` y `clavePrueba` con cuentas de prueba. Las contraseñas y tokens reales no se distribuyen con la colección. Si se necesitan usuarios de prueba, utilizar el perfil `prueba` documentado en este README.

1. **Figura 4:** registrar un usuario nuevo con el correo indicado en la solicitud. Se espera **200** y rol **CLIENTE**, aunque el cuerpo solicite ADMIN. Si el correo ya existe, elegir otro y actualizarlo también en la Figura 14.
2. **Figura 14:** repetir el registro utilizando exactamente el mismo correo de la Figura 4. Se espera **400** por correo duplicado.
3. **Figura 8:** consultar el catálogo de productos sin token. Se espera **200**.
4. **Figura 10:** configurar credenciales de un CLIENTE, ejecutar `01 - Login JWT` para actualizar `jwtToken` y probar la creación de producto. Se espera **403**.
5. **Figura 11:** cambiar las credenciales por las de un ADMIN, ejecutar nuevamente `01 - Login JWT` para reemplazar `jwtToken` y crear el producto. Se espera **200**; esta solicitud incorpora datos a la base.
6. **Figura 17:** configurar `correoPrueba` con una cuenta CLIENTE de pruebas y ejecutar una vez el login con clave incorrecta. Se espera **401**. Cinco intentos fallidos consecutivos pueden activar un bloqueo temporal de cinco minutos, por lo que no deben repetirse salvo para comprobar expresamente ese mecanismo.

Las Figuras 18 y 19 tienen instrucciones adicionales sobre firma alterada y expiración del JWT. Para repetir la Figura 19 desde cero, limpiar la variable `jwtTokenFijo19`, obtener un nuevo JWT con vigencia temporal de 60 segundos y ejecutar la solicitud antes y después del vencimiento. Restaurar posteriormente la vigencia normal de 15 minutos. Las 35 solicitudes no deben considerarse una secuencia automática de ejecución: requieren roles, datos y condiciones diferentes.

### Pruebas de seguridad

| # | Petición | Autenticación (Bearer Token del usuario indicado) | Resultado esperado |
|---|---|---|---|
| 1 | `GET http://localhost:8080/api/usuarios` | Ninguna | **401 Unauthorized** |
| 2 | `GET http://localhost:8080/api/usuarios` | `cliente@petsalud.cl` | **403 Forbidden** (rol insuficiente) |
| 3 | `GET http://localhost:8080/api/usuarios` | `admin@petsalud.cl` | **200 OK**, lista de usuarios **sin** el campo `password` |
| 4 | `POST http://localhost:8080/api/auth/login` con la contraseña de `admin@petsalud.cl` incorrecta | Ninguna | **401** ("Credenciales invalidas") y en la consola aparece `LOGIN_FALLIDO` con la IP y los fallos seguidos |
| 4a | `POST http://localhost:8080/api/auth/login` con clave incorrecta, **5 veces seguidas** (usa `cliente@petsalud.cl`) | Ninguna | Intentos 1 a 5: **401**. En el 5.º aparece `CUENTA_BLOQUEADA` en la consola y en `logs/petsalud.log` |
| 4b | `GET http://localhost:8080/api/usuarios` | Bearer Token del admin con un carácter alterado | **401** (firma inválida) |
| 4c | El mismo login (ahora **con la clave correcta**) inmediatamente después | Ninguna | **429 Too Many Requests**, con encabezado `Retry-After` y el mensaje de cuenta bloqueada |
| 4d | El mismo login con la clave correcta pasados **5 minutos** | Ninguna | **200 OK** con token (bloqueo terminado) |
| 5 | `GET http://localhost:8080/api/productos` | Ninguna | **200 OK** (el catálogo es público) |
| 6 | `POST http://localhost:8080/api/productos` | `cliente@petsalud.cl` | **403 Forbidden** |
| 7 | `POST http://localhost:8080/api/productos` | `admin@petsalud.cl` | **200 OK**, producto creado |
| 8 | `GET http://localhost:8080/api/expedientes/mascota/1` | `cliente@petsalud.cl` | **403 Forbidden** (solo personal) |
| 9 | `GET http://localhost:8080/api/expedientes/mascota/1` | `vet@petsalud.cl` | Autorizado: **200 OK** (lista vacía si no hay datos) |
| 10 | `GET http://localhost:8080/api/mascotas/{id}` de una mascota de **otro** cliente | `cliente@petsalud.cl` | **403 Forbidden** (no puede ver datos ajenos); con una mascota propia: **200** |
| 11 | `GET http://localhost:8080/api/ventas/usuario/{id}` con el id de **otro** usuario | `cliente@petsalud.cl` | **403 Forbidden**; con su propio id: **200** |
| 12 | `GET http://localhost:8080/api/recompensas/{id}` de otro cliente, y `DELETE http://localhost:8080/api/recompensas/{id}` | `cliente@petsalud.cl` | **403 Forbidden** en ambas (eliminar y crear recompensas es solo del ADMIN) |
| 13 | `GET http://localhost:8080/api/mascotas/{id}` de un cliente | `vet@petsalud.cl` | **200 OK**: el veterinario puede consultar para la atención clínica |
| 14 | `POST http://localhost:8080/api/expedientes?mascotaId={id}&veterinarioId={id de otro usuario}` | `vet@petsalud.cl` | **403 Forbidden**; con su **propio** `veterinarioId`: **200** |

> Para las pruebas 10 a 14 se necesitan datos: registra una mascota con `POST /api/mascotas/propietario/{idDelCliente}` y usa el id que devuelve. Para ver ids de usuarios, consulta `GET /api/usuarios` como `admin@petsalud.cl`.

Body de la prueba 7:

```json
{
  "nombre": "Alimento perro 15 kg",
  "descripcion": "Alimento premium",
  "precio": 24990,
  "stock": 20,
  "categoria": "Alimentos"
}
```

### Bloqueo por intentos fallidos (lockout)

Tras **5 intentos de login fallidos** con el mismo correo y desde la misma IP, esa combinación queda bloqueada **5 minutos**:

- Intentos 1 a 5 con clave incorrecta: **401 Unauthorized**.
- Desde el 6.º intento, durante el bloqueo, el login responde **429 Too Many Requests** (con `Retry-After` en segundos), aunque la clave sea correcta. Los intentos durante el bloqueo no lo extienden.
- Pasados los 5 minutos, la combinación de correo e IP queda desbloqueada. Un login correcto antes del 5.º fallo reinicia el contador, y los fallos más antiguos que 5 minutos no se acumulan.
- El bloqueo es por correo + IP (sin distinguir mayúsculas en el correo) y se guarda en memoria: se reinicia si se reinicia la aplicación.
- Para una demostración rápida, arranca con 1 minuto de bloqueo: `.\mvnw.cmd clean spring-boot:run "-Dspring-boot.run.profiles=prueba" "-Dspring-boot.run.arguments=--login.lock-minutes=1"`.
- Usa `cliente@petsalud.cl` para probar, para no bloquear la cuenta del admin que necesitas en las demás pruebas.

### Prueba de registro y escalada de privilegios

1. `POST http://localhost:8080/api/usuarios/registro`, sin autenticación (**No Auth**), con este body. Se envía `"rol": "ADMIN"` a propósito:

```json
{
  "email": "prueba@correo.cl",
  "password": "Clave1234",
  "nombre": "Prueba",
  "apellido": "Cliente",
  "telefono": "+56911112222",
  "direccion": "Santiago",
  "rol": "ADMIN"
}
```

2. Resultado esperado: **200 OK** y la respuesta muestra `"rol": "CLIENTE"` (el sistema ignora el rol enviado) y no incluye `password`.
3. Comprobación: haz login con `prueba@correo.cl` / `Clave1234` y usa el token en `GET http://localhost:8080/api/usuarios`: debe dar **403**, porque es CLIENTE.
4. Repetir el mismo registro devuelve **400 Bad Request** porque el email ya existe.

### Comprobar el cifrado de contraseñas en MySQL

```sql
USE petsalud_db;
SELECT email, password, rol FROM usuarios;
```

La columna `password` muestra un hash que empieza con `$2a$`, nunca el texto original.

## 6. Roles y permisos

| Quién | Rutas |
|---|---|
| Público | `POST /api/usuarios/registro`, `GET /api/productos` y `/api/productos/**` (excepto `stock-bajo`) |
| ADMIN (gerente) | `/api/usuarios/**`, crear/editar/borrar productos, `GET /api/ventas`, `GET /api/citas/entre-fechas`, crear y eliminar recompensas |
| VETERINARIO y ADMIN | `/api/expedientes/**`, `GET /api/productos/stock-bajo`, `GET /api/citas/veterinario/**`, `PUT /api/citas/*/estado` |
| Cliente autenticado | Solo **sus propios** datos: sus mascotas, sus citas, sus ventas, sus notificaciones y sus recompensas |
| VETERINARIO (empleado) | **Consulta** mascotas, expedientes y recompensas de los clientes; registra expedientes **a su propio nombre**; cambia el estado solo de **sus citas asignadas**. **No** modifica ni elimina mascotas, no consulta las ventas de los clientes ni gestiona citas de otros veterinarios |

La autorización tiene tres capas:

1. Las reglas por ruta y rol de `SecurityConfig`.
2. **Semana 3:** `@PreAuthorize` en cada método de los controladores, con las reglas de `ReglasAcceso` (`PUBLICO`, `AUTENTICADO`, `PERSONAL` y `GERENTE`). Si una ruta quedara mal configurada, la anotación igual bloquea al rol incorrecto. `PreAuthorizeTest` falla si algún endpoint queda sin regla.
3. La verificación de **propiedad** dentro de los servicios (`AutorizacionService`), que evita que un cliente cambie el id en la URL y vea datos ajenos (IDOR). Se aplica en mascotas, citas, ventas, notificaciones, recompensas y expedientes.

Desde la Semana 3, eliminar un expediente médico (`DELETE /api/expedientes/{id}`) es solo para ADMIN.


### Decisión de diseño: nombres de roles

Los nombres de los roles en el código no coinciden con los del negocio. Se mantuvieron por compatibilidad con los datos y los tokens ya existentes, y la equivalencia queda documentada aquí y en `ReglasAcceso`:

| Rol en el código | Rol en el negocio | Alcance |
|---|---|---|
| `CLIENTE` | Cliente | Solo sus propios recursos (mascotas, citas, ventas, notificaciones y recompensas) |
| `VETERINARIO` | Empleado | Atención de citas y expedientes; consulta de productos y de datos de clientes |
| `ADMIN` | Gerente | Gestión completa del sistema |

En `ReglasAcceso`, `PERSONAL` equivale a empleado o gerente y `GERENTE` equivale a `ADMIN`.

Si en el futuro se decide renombrar los roles, la migración controlada sería: (1) agregar los nombres nuevos al enum `Rol` y aceptar ambos temporalmente; (2) actualizar con SQL la columna `rol` de la tabla `usuarios`; (3) esperar a que expiren los tokens vigentes (duran 15 minutos) o invalidarlos; (4) actualizar `@PreAuthorize`, `SecurityConfig` y las pruebas, y retirar los nombres antiguos.

## 7. Pruebas automatizadas

El proyecto incluye pruebas automatizadas de autorización en servicios, validación JWT, autenticación y autorización HTTP, además del bloqueo de intentos de inicio de sesión. Para ejecutar un subconjunto de las pruebas de seguridad, utiliza:

```powershell
.\mvnw.cmd test "-Dtest=*SecurityTest,AutorizacionServiceTest,Jwt*Test,SecurityConfigHttpTest,LoginAttemptServiceTest,AuthLockoutTest"
```

Para ejecutar la suite completa, utiliza `.\mvnw.cmd test`. Con la Semana 3 la suite tiene **97 pruebas, sin fallos, errores ni pruebas omitidas, con resultado BUILD SUCCESS** (en la Semana 2 eran 55).

Con la Semana 4, la suite completa suma **133 pruebas** (36 adicionales respecto de las 97 documentadas en Semana 3), con **0 fallos, 0 errores y 0 omitidas**. La ejecucion limpia de `mvn verify` finalizo con **BUILD SUCCESS** y **All coverage checks have been met**. Cobertura JaCoCo de lineas: `VentaServiceImpl` **98,0 % (49/50)** y `UsuarioServiceImpl` **100,0 % (19/19)**; cobertura de ramas: **96,4 %** y **100,0 %**, respectivamente. Ambas clases superan el minimo obligatorio de **80 % de lineas**. En esta rama, `BackendpetsaludApplicationTests` utiliza H2 aislado, por lo que las pruebas automatizadas no requieren MySQL; la ejecucion normal de la API si requiere la base de datos configurada.

Pruebas nuevas de la Semana 3:

| Clase | Qué comprueba |
|---|---|
| `PreAuthorizeTest` | Todos los endpoints tienen `@PreAuthorize` y los roles incorrectos reciben 403 sin llegar al servicio |
| `AmenazasComunesTest` | SQL Injection (login, registro, búsqueda e id), XSS (mascota, diagnóstico y motivo de cita), cabeceras de seguridad y CSRF (cookie sin Bearer, login sin sesión y CORS) |
| `InyeccionSqlRepositorioTest` | Cargas de inyección contra una base H2 real y que ningún repositorio use SQL nativo |
| `MonitoreoSeguridadTest` y `ActividadSospechosaServiceTest` | Los eventos de seguridad quedan registrados con IP y sin contraseñas ni tokens |
| `AuthLockoutTest` y `JwtUtilTest` (1 nueva cada una) | Cinco fallos desde una IP no bloquean al mismo usuario desde otra IP, y el JWT dura 15 minutos |

Solo las pruebas de la Semana 3 (no necesitan MySQL):

```powershell
.\mvnw.cmd test "-Dtest=PreAuthorizeTest,AmenazasComunesTest,InyeccionSqlRepositorioTest,MonitoreoSeguridadTest,ActividadSospechosaServiceTest"
```
 Esta ejecución incluye `BackendpetsaludApplicationTests`, por lo que requiere MySQL disponible y las variables de conexión y `JWT_SECRET` correctamente configuradas. En el entorno local de revisión se utilizó MySQL en el puerto 3306.

## 8. Limitaciones conocidas

- **CSRF desactivado:** la credencial viaja solo en el header `Authorization`, que el navegador no agrega por su cuenta, y la API no usa cookies ni sesión. Se complementa con CORS restringido a `cors.allowed-origins`. Si algún día el token se guarda en una cookie, hay que reactivar CSRF.
- **Citas:** el veterinario asignado no se valida contra su rol, y el estado de una cita solo se valida como texto (letras, 3 a 20 caracteres), no contra una lista cerrada. El veterinario tampoco agenda citas (las agenda el cliente o el ADMIN).
- **JWT sin revocación ni refresh:** un token es válido hasta que expira (15 minutos); no hay lista de tokens revocados ni token de refresco.
- **Bloqueo en memoria:** el contador de intentos fallidos no se comparte entre varias instancias ni sobrevive a un reinicio. La combinación de correo + IP reduce el riesgo de bloqueo provocado desde otra IP, pero un atacante puede distribuir intentos entre distintas IP. Detrás de un proxy, debe configurarse correctamente la identificación de la IP del cliente.
- **Monitoreo en memoria:** el conteo de solicitudes por IP también vive en memoria. En producción, el archivo `logs/petsalud.log` debería enviarse a un sistema de monitoreo (IDS/IPS o SIEM).
- **Proveedores:** la entidad `Proveedor` y su relación con `Producto` existen, pero todavía no hay endpoints para crearlos o asignarlos, y la venta no exige que el producto tenga proveedor.
- **Pruebas:** hasta la Semana 3 el proyecto contaba con 97 pruebas automatizadas aprobadas que incluyen validación de JWT, autenticación, autorización por roles, controles de los servicios y bloqueo de intentos de inicio de sesión. Las verificaciones manuales con Postman complementan esta cobertura, pero no se ha comprobado exhaustivamente el funcionamiento de todos los endpoints y escenarios posibles.

## 9. Monitoreo de actividad sospechosa (Semana 3)

`ActividadSospechosaService` escribe un evento por línea en el logger `SEGURIDAD`, en la consola y en `logs/petsalud.log`. Cada línea trae fecha, hora, IP, usuario, método y ruta. Nunca registra contraseñas ni tokens, y reemplaza los saltos de línea para que nadie pueda falsificar líneas del log.

| Evento | Cuándo |
|---|---|
| `LOGIN_OK` / `LOGIN_FALLIDO` | Login correcto o incorrecto (incluye fallos seguidos) |
| `CUENTA_BLOQUEADA` / `LOGIN_RECHAZADO_BLOQUEO` | Quinto fallo seguido / intento durante el bloqueo |
| `JWT_INVALIDO` | Token alterado, vencido, de otro emisor o audiencia |
| `SIN_AUTENTICACION` / `ACCESO_DENEGADO` | Respuestas 401 / 403 |
| `ENTRADA_RECHAZADA` | Datos que no pasan la validación (solo los nombres de los campos) |
| `OPERACION` | POST, PUT o DELETE exitoso, con el usuario que lo hizo |
| `RAFAGA_SOLICITUDES` | Más de 200 solicitudes desde una IP en 15 minutos |

Ejemplo real (`docs/semana3/evidencias/log-seguridad.txt`):

```text
WARN ACCESO_DENEGADO usuario=vet@petsalud.cl ip=127.0.0.1 DELETE /api/expedientes/1
WARN JWT_INVALIDO ip=127.0.0.1 GET /api/productos/stock-bajo motivo=SignatureException
WARN CUENTA_BLOQUEADA usuario=otro.cliente@petsalud.cl ip=127.0.0.1 minutos=5 motivo=posible_fuerza_bruta
```

## 10. Pruebas de amenazas en Postman (Semana 3)

Importa `postman/PetSalud_Semana3_Amenazas.postman_collection.json` (si ya estaba importada, elige **Replace**). Configura la variable `clavePrueba` con la clave del perfil `prueba` y ejecuta la colección completa con el **Collection Runner** (clic derecho > **Run collection**). La carpeta `00 - Preparación` deja todo listo sin ingresar valores a mano: inicia sesión con los tres roles (`tokenCliente`, `tokenVet` y `tokenAdmin`), obtiene los IDs de los usuarios y busca la mascota `MascotaPrueba` y su expediente de prueba; si no existen, los crea en la base. Cada solicitud trae una prueba con el código esperado.

En la primera ejecución se crean la mascota y el expediente (24 solicitudes, 35 pruebas). En las siguientes, las dos solicitudes de creación se omiten y no se duplican datos (31 pruebas).

| Carpeta | Solicitudes | Resultado esperado |
|---|---|---|
| 00 - Preparación | Logins por rol, IDs de usuarios, mascota y expediente de prueba (se crean solo si no existen) | 200 |
| 01 - SQL Injection | Login, registro, búsqueda por categoría e id de ruta con inyección | 401 en el login y 400 en el resto |
| 02 - XSS | Script o HTML en mascota, diagnóstico y motivo de cita; cabeceras de la respuesta | 400 y cabeceras `Content-Security-Policy`, `nosniff` y `DENY` |
| 03 - CSRF | Cookie sin Bearer, login sin `Set-Cookie` y preflight CORS desde otro sitio | 401, sin cookie y 403 |
| 04 - Roles con @PreAuthorize | Cliente envía notificación, veterinario elimina expediente, gerente envía notificación | 403, 403 y 200 |
| 05 - Monitoreo | JWT con la firma alterada | 401 y `JWT_INVALIDO` en el log |
