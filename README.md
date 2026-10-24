# Backend Clínica PetSalud

API REST con Spring Boot 3, Spring Security (usuario y contraseña con HTTP Basic), JPA y MySQL.

## 1. Requisitos

| Herramienta | Detalle |
|---|---|
| JDK | 17 o 21 (recomendado). Ya que se utilizó una versión superior se configura Lombok en el `pom.xml` 
| MySQL | Servidor encendido y accesible |
| Postman | Para probar los endpoints |
| Maven | No se instala: el proyecto incluye el Maven Wrapper (`mvnw.cmd`) |

Para comprobar Java, ejecuta `java -version` en PowerShell o en la terminal de vscode.

## 2. Configuración de la base de datos

Editar `src/main/resources/application.properties`. Se configuran los datos de user y pw segun las variables de entornos de los participantes, al gestionar la base de datos de no existir se crea para trabajar.

```properties
spring.application.name=backendpetsalud
spring.datasource.url=jdbc:mysql://localhost:3306/petsalud_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

- Cambia el puerto `3306` si tu MySQL usa otro (por ejemplo `3307`).
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

PENDIENTE crear una **Collection** llamada "PetSalud" y guardar ahí las pruebas.

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
| 8 | `GET http://localhost:8080/api/expedientes/mascota/1` | `cliente@petsalud.cl` | **403 Forbidden** (la mascota 1 no es suya: solo personal o el dueño) |
| 9 | `GET http://localhost:8080/api/expedientes/mascota/1` | `vet@petsalud.cl` | Autorizado: **200 OK** (lista vacía si no hay datos) |
| 10 | `GET http://localhost:8080/api/auth/me` | cualquier usuario | **200 OK**, muestra email y rol con que quedó autenticada la petición |
| 11 | `GET http://localhost:8080/api/mascotas/{id}` de una mascota de **otro** cliente | `cliente@petsalud.cl` | **403 Forbidden** (no puede ver datos ajenos) |
| 12 | `GET http://localhost:8080/api/usuarios/{id}` con su **propio** id | `cliente@petsalud.cl` | **200 OK**; con el id de otro usuario: **403** |
| 13 | 5 veces `GET /api/auth/me` con contraseña incorrecta y luego la correcta | `cliente@petsalud.cl` | **401** también con la correcta: cuenta bloqueada 15 minutos (consola: `CUENTA BLOQUEADA`) |
| 14 | `GET http://localhost:8080/api/cualquier-cosa` | `admin@petsalud.cl` | **403**: toda ruta no declarada se bloquea (`denyAll`) |

> Para la prueba 11: registra una mascota con un cliente (`POST /api/mascotas/propietario/{suId}`) y luego intenta verla autenticado como otro cliente.

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
4. Repetir el mismo registro devuelve **400** porque el email ya existe.
5. Si el email no es válido, la contraseña tiene menos de 8 caracteres o no mezcla letras y números, o el nombre trae caracteres como `<script>`, la respuesta es **400** e indica qué campo falló.

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
| ADMIN (gerente) | `/api/usuarios/**`, crear/editar/borrar productos, `GET /api/ventas` |
| VETERINARIO y ADMIN | `/api/expedientes/**`, `GET /api/productos/stock-bajo`, `GET /api/citas/entre-fechas`, `GET /api/citas/veterinario/**`, `PUT /api/citas/*/estado` |
| Cliente autenticado | Solo **sus propios** datos: su usuario, sus mascotas, las citas y expedientes de sus mascotas, sus ventas, recompensas y notificaciones (se verifica con `@PreAuthorize` y `SeguridadService`) |
| Cualquier otra ruta | Bloqueada (`anyRequest().denyAll()`) |

La autorización tiene dos capas: reglas por URL y verbo en `SecurityConfig`, y `@PreAuthorize` en cada endpoint para verificar que el recurso pertenezca al usuario (evita que un cliente cambie el id en la URL y vea datos ajenos, ataque IDOR).

## 7. Pruebas automatizadas

```powershell
.\mvnw.cmd test
```

Usan una base H2 en memoria (no necesitan MySQL ni variables de entorno). `SeguridadIntegrationTest` valida 16 casos: 401 y 403 por rol, IDOR, registro seguro, validación de datos, bloqueo por intentos, rutas no declaradas, ausencia del ciclo JSON y cabeceras de seguridad.


## 8. Limitaciones conocidas

- **CSRF desactivado:** se justifica porque la API es stateless y cada petición envía el encabezado `Authorization`. Si en el futuro un frontend web consume la API desde el navegador, conviene reactivarlo (token en cookie `XSRF-TOKEN`).
- **Bloqueo por intentos en memoria:** se reinicia al reiniciar la aplicación; con varias instancias habría que guardarlo en una base compartida.
