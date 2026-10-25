# Backend Clínica PetSalud

API REST con Spring Boot 3, Spring Security (usuario y contraseña con HTTP Basic), JPA y MySQL.

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
```

- El proyecto está configurado con el puerto `3307`. Cámbialo (por ejemplo a `3306`) si tu MySQL usa otro.
- Las variables de entorno `DB_USERNAME` y `DB_PASSWORD` deben existir antes de abrir la terminal.
- `createDatabaseIfNotExist=true` crea la base `petsalud_db` si no existe. Las tablas las crea Hibernate al iniciar.

## 3. Cómo ejecutar

1. Abre la terminal (powershell o vscode) en la carpeta del proyecto (la que contiene `mvnw.cmd`).
2. Ejecuta con el perfil `prueba`, que crea los usuarios de prueba:

```powershell
.\mvnw.cmd clean spring-boot:run "-Dspring-boot.run.profiles=prueba"
```

- La primera vez tarda unos minutos porque descarga Maven y las dependencias.
- Está listo cuando aparece una línea como `Started BackendpetsaludApplication`.
- La API queda en `http://localhost:8080`.

Para detenerla, presiona `Ctrl + C` en la terminal.

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
2. Pestaña **Authorization**: tipo **Basic Auth**. En *Username* escribe el email y en *Password* la contraseña. Para probar sin credenciales, deja el tipo en **No Auth**.
3. Para las peticiones POST con datos: pestaña **Body → raw → JSON**.
4. Presiona **Send** y revisa el código de estado (arriba a la derecha de la respuesta). Guarda una captura de cada prueba para el informe.

Se recomienda crear una **Collection** llamada "PetSalud" y guardar ahí las pruebas.

### Pruebas de seguridad

| # | Petición | Autenticación | Resultado esperado |
|---|---|---|---|
| 1 | `GET http://localhost:8080/api/usuarios` | Ninguna | **401 Unauthorized** |
| 2 | `GET http://localhost:8080/api/usuarios` | `cliente@petsalud.cl` | **403 Forbidden** (rol insuficiente) |
| 3 | `GET http://localhost:8080/api/usuarios` | `admin@petsalud.cl` | **200 OK**, lista de usuarios **sin** el campo `password` |
| 4 | `GET http://localhost:8080/api/usuarios` | `admin@petsalud.cl` con contraseña incorrecta | **401** y en la consola de Spring aparece `LOGIN FALLIDO` |
| 5 | `GET http://localhost:8080/api/productos` | Ninguna | **200 OK** (el catálogo es público) |
| 6 | `POST http://localhost:8080/api/productos` | `cliente@petsalud.cl` | **403 Forbidden** |
| 7 | `POST http://localhost:8080/api/productos` | `admin@petsalud.cl` | **200 OK**, producto creado |
| 8 | `GET http://localhost:8080/api/expedientes/mascota/1` | `cliente@petsalud.cl` | **403 Forbidden** (solo personal) |
| 9 | `GET http://localhost:8080/api/expedientes/mascota/1` | `vet@petsalud.cl` | Autorizado: **200 OK** (lista vacía si no hay datos) |
| 10 | `GET http://localhost:8080/api/mascotas/{id}` de una mascota de **otro** cliente | `cliente@petsalud.cl` | **403 Forbidden** (no puede ver datos ajenos); con una mascota propia: **200** |
| 11 | `GET http://localhost:8080/api/ventas/usuario/{id}` con el id de **otro** usuario | `cliente@petsalud.cl` | **403 Forbidden**; con su propio id: **200** |
| 12 | `GET http://localhost:8080/api/recompensas/{id}` de otro cliente, y `DELETE http://localhost:8080/api/recompensas/{id}` | `cliente@petsalud.cl` | **403 Forbidden** en ambas (eliminar y crear recompensas es solo del ADMIN) |
| 13 | `GET http://localhost:8080/api/mascotas/{id}` de un cliente | `vet@petsalud.cl` | **200 OK**: el veterinario puede consultar para la atención clínica |
| 14 | `PUT http://localhost:8080/api/mascotas/{id}` de un cliente | `vet@petsalud.cl` | **403 Forbidden**: el veterinario no modifica mascotas (el dueño y el ADMIN sí) |
| 15 | `POST http://localhost:8080/api/expedientes?mascotaId={id}&veterinarioId={id de otro usuario}` | `vet@petsalud.cl` | **403 Forbidden**; con su **propio** `veterinarioId`: **200** |

> Para las pruebas 10 a 15 se necesitan datos: registra una mascota con `POST /api/mascotas/propietario/{idDelCliente}` y usa el id que devuelve. Para ver ids de usuarios, consulta `GET /api/usuarios` como `admin@petsalud.cl`.

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
3. Comprobación: `GET http://localhost:8080/api/usuarios` con Basic Auth `prueba@correo.cl` / `Clave1234` debe dar **403**, porque es CLIENTE.
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

La autorización tiene dos capas: las reglas por ruta y rol de `SecurityConfig`, y la verificación de **propiedad** dentro de los servicios (`AutorizacionService`). La segunda evita que un cliente cambie el id en la URL y vea datos ajenos (IDOR) y se aplica en mascotas, citas, ventas, notificaciones, recompensas y expedientes.


## 7. Pruebas automatizadas

Las pruebas unitarias verifican las reglas de autorización de los servicios (`AutorizacionServiceTest` y las `*SecurityTest`) y no necesitan base de datos:

```powershell
.\mvnw.cmd test "-Dtest=*SecurityTest,AutorizacionServiceTest"
```

`.\mvnw.cmd test` sin filtro también ejecuta `BackendpetsaludApplicationTests`, que levanta la aplicación completa y **requiere MySQL encendido y las variables `DB_USERNAME` y `DB_PASSWORD`**.

## 8. Limitaciones conocidas

- **CSRF desactivado:** se justifica porque la API es stateless y cada petición envía el encabezado `Authorization`.
- **Citas:** el veterinario asignado no se valida contra su rol y los estados de una cita no están restringidos a una lista. El veterinario tampoco agenda citas (las agenda el cliente o el ADMIN).
- **Sin bloqueo por intentos fallidos:** los logins fallidos solo se registran en el log (`LOGIN FALLIDO`).
- **Pruebas:** la verificación de rutas y roles completa se hace con Postman; las pruebas automáticas cubren solo las reglas de los servicios.
