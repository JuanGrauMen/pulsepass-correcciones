# PulsePass

PulsePass es una plataforma académica para consultar venues, eventos y artistas, registrar usuarios y gestionar entradas. El proyecto integra persistencia relacional, reglas de negocio y una API REST.

## Tecnologías

- Java 21 y Spring Boot 4.1.1
- Maven
- Spring Web MVC y Bean Validation
- Spring Data JPA / Hibernate
- PostgreSQL y Flyway
- MapStruct 1.6.3 para el mapeo de DTOs
- JUnit 5, Mockito, AssertJ y MockMvc
- Testcontainers para pruebas de integración con PostgreSQL

## Arquitectura

El proyecto separa responsabilidades en tres capas principales:

- **Persistencia:** entidades JPA, relaciones, repositories y migraciones Flyway.
- **Service:** reglas de negocio, transacciones, DTOs y mapeo de respuestas.
- **Controller:** API REST, validación de solicitudes HTTP y traducción de errores a respuestas HTTP.

Las respuestas de la API utilizan DTOs; los controllers no exponen directamente entidades JPA ni acceden a repositories.

## Requisitos previos

- JDK 21.
- Docker en ejecución para las pruebas de integración que utilizan Testcontainers.
- PostgreSQL disponible para ejecutar la aplicación localmente.

## Cómo ejecutar las pruebas

Desde la raíz del repositorio:

```bash
./mvnw clean test
```

Este comando ejecuta las pruebas unitarias y de controller descubiertas por Surefire.

Para una verificación completa en una sola ejecución, incluyendo todas las clases de integración terminadas en `IT`, usa:

```bash
./mvnw clean verify
```

Maven Failsafe ejecuta las pruebas de integración durante las fases `integration-test` y `verify`. Estas pruebas utilizan PostgreSQL mediante Testcontainers, por lo que Docker debe estar instalado y en ejecución. Si `clean verify` falla, revisa si el error corresponde a compilación, pruebas unitarias/controller o integración con PostgreSQL.

En Windows también puedes ejecutar el wrapper desde PowerShell con `./mvnw.cmd` o desde Git Bash con `./mvnw`.

## Cómo ejecutar la API localmente

Configura una base de datos PostgreSQL y las siguientes variables de entorno antes de iniciar la aplicación. Sustituye los valores de ejemplo por los de tu entorno.

| Variable | Obligatoria | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Sí | URL JDBC de la base de datos. |
| `SPRING_DATASOURCE_USERNAME` | Sí | Usuario de la base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | Sí | Contraseña de la base de datos. |
| `PULSEPASS_JWT_SECRET_BASE64` | Sí | Clave HMAC en Base64 con al menos 32 bytes. Sin ella la aplicación no arranca; no tiene valor por defecto. |
| `PULSEPASS_JWT_ISSUER` | No | Emisor de los tokens. Por defecto `pulsepass-api`. |

Para generar una clave JWT de 48 bytes:

```bash
openssl rand -base64 48
```

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

En Git Bash, Linux o macOS:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pulsepass
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=tu_clave
export PULSEPASS_JWT_SECRET_BASE64=$(openssl rand -base64 48)
./mvnw spring-boot:run
```

En PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/pulsepass"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "tu_clave"
$env:PULSEPASS_JWT_SECRET_BASE64 = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
.\mvnw.cmd spring-boot:run
```

La base de datos indicada debe existir y las credenciales deben tener permisos suficientes para que Flyway ejecute las migraciones y JPA acceda al esquema. Las pruebas no necesitan ninguna de estas variables: usan Testcontainers y un secreto JWT de prueba definido en `src/test/resources/config/application.properties`.

La configuración de la aplicación establece `spring.jpa.hibernate.ddl-auto=validate`: Flyway crea/evoluciona el esquema y Hibernate valida su correspondencia con las entidades, sin generar ni modificar tablas.

## Migraciones

Flyway es el único responsable del esquema. Las migraciones están en `src/main/resources/db/migration` y se aplican en orden de versión:

| Versión | Archivo | Qué hace |
|---|---|---|
| V1 | `V1__create_schema.sql` | Crea `venues`, `events`, `artists`, `event_artists`, `users`, `user_profiles` y `tickets`, con claves foráneas y restricciones únicas. |
| V2 | `V2__insert_initial_artists.sql` | Inserta los cinco artistas iniciales. |
| V3 | `V3__add_streaming_url_to_event.sql` | Agrega la columna opcional `streaming_url` a `events`. |
| V4 | `V4__enforce_domain_integrity.sql` | Restricciones `CHECK` de categorías y estados, edad mínima no negativa, e índice único sobre `LOWER(email)`. |
| V5 | `V5__security_credentials_roles.sql` | Crea `user_credentials` (hash de contraseña) y `user_roles` (`USER`, `STAFF`, `ADMIN`). |
| V6 | `V6__artist_stage_name_case_insensitive_unique.sql` | Índice único sobre `LOWER(stage_name)`: el nombre artístico no distingue mayúsculas. |

`FlywayMigrationIT` aplica todas las migraciones sobre una base PostgreSQL vacía y comprueba que las reglas de integridad existen y que la base rechaza, por ejemplo, un evento con una categoría no admitida.

## Endpoints REST

La API expone 22 operaciones: 20 de los servicios y 2 de autenticación. Todas devuelven cuerpos JSON; los errores usan la estructura común descrita en la sección de errores. Quién puede llamar a cada ruta se resume en la sección [Seguridad](#seguridad).

### Venues

| Método | Endpoint | Respuesta exitosa |
|---|---|---|
| GET | `/api/venues/{code}` | 200 OK |
| GET | `/api/venues/active` | 200 OK |

### Eventos

| Método | Endpoint | Respuesta exitosa |
|---|---|---|
| POST | `/api/events` | 201 Created |
| GET | `/api/events/{eventCode}` | 200 OK |
| GET | `/api/events/published` | 200 OK |
| PATCH | `/api/events/{eventCode}/publish` | 200 OK |
| POST | `/api/events/{eventCode}/artists/{artistId}` | 200 OK |
| GET | `/api/events/by-artist?stageName=...` | 200 OK |

### Artistas

| Método | Endpoint | Respuesta exitosa |
|---|---|---|
| GET | `/api/artists/{id}` | 200 OK |
| GET | `/api/artists/by-stage-name?stageName=...` | 200 OK |
| GET | `/api/artists/active` | 200 OK |

### Usuarios

| Método | Endpoint | Respuesta exitosa |
|---|---|---|
| POST | `/api/users` | 201 Created |
| GET | `/api/users/by-email?email=...` | 200 OK |
| GET | `/api/users/by-username?username=...` | 200 OK |

### Tickets

| Método | Endpoint | Respuesta exitosa |
|---|---|---|
| POST | `/api/tickets` | 201 Created |
| GET | `/api/tickets/{ticketCode}` | 200 OK |
| GET | `/api/tickets/by-user?email=...` | 200 OK |
| GET | `/api/events/{eventCode}/tickets/paid` | 200 OK |
| PATCH | `/api/tickets/{ticketCode}/cancel` | 200 OK |
| PATCH | `/api/tickets/{ticketCode}/use` | 200 OK |

### Autenticación

| Método | Endpoint | Respuesta exitosa |
|---|---|---|
| POST | `/api/auth/login` | 200 OK |
| GET | `/api/auth/me` | 200 OK |

## Seguridad

La API es sin estado y usa JWT firmado con HS256 en el encabezado `Authorization: Bearer <token>`. Se obtiene con `POST /api/auth/login`. En cada solicitud el filtro valida el token y vuelve a consultar en la base de datos si el usuario sigue activo y qué roles tiene, de modo que un cambio de rol surte efecto de inmediato.

| Acceso | Rutas |
|---|---|
| Público | `POST /api/users`, `POST /api/auth/login`, venues, artistas, `GET /api/events/published`, `GET /api/events/by-artist` y el detalle de un evento publicado |
| Usuario autenticado | `GET /api/auth/me` |
| `USER` propietario | comprar, consultar y cancelar sus propios tickets; consultar su propio usuario |
| `STAFF` | detalle de eventos en cualquier estado, tickets pagados de un evento, consultar un ticket, marcarlo como usado |
| `ADMIN` | crear y publicar eventos, asociar artistas, marcar tickets como usados, y consultar o cancelar los tickets y consultar los datos de cualquier usuario. La compra de tickets es solo para `USER` |

`GET /api/events/by-artist` devuelve solo eventos publicados al público y los eventos de todos los estados a `STAFF` y `ADMIN`.

## Validación y manejo de errores

Las solicitudes se validan antes de llamar al Service. El `GlobalExceptionHandler` devuelve respuestas con una estructura común `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `details`).

- **400 Bad Request:** validación de campos, JSON mal formado o parámetros ausentes o de tipo incorrecto. La contraseña (mínimo 12 caracteres, máximo 72 bytes UTF-8) siempre produce 400.
- **401 Unauthorized:** token ausente, inválido o expirado, o credenciales incorrectas.
- **403 Forbidden:** usuario autenticado sin el rol o la propiedad requeridos.
- **404 Not Found:** recurso no encontrado o ruta inexistente.
- **405 Method Not Allowed** y **415 Unsupported Media Type:** método o tipo de contenido no admitido.
- **409 Conflict:** recurso duplicado o regla de negocio incumplida.
- **500 Internal Server Error:** error inesperado, sin exponer detalles internos.

Las pruebas de controller utilizan `@WebMvcTest`, `@MockitoBean` y `MockMvc` para comprobar códigos HTTP, contenido JSON y delegación a los servicios.

## Modelo de dominio

Las entidades principales son `Venue`, `Event`, `Artist`, `User`, `UserProfile` y `Ticket`.

Relaciones principales:

```text
Venue 1 ─────── N Event
Event N ─────── M Artist
User 1 ─────── 1 UserProfile
User 1 ─────── N Ticket
Event 1 ─────── N Ticket
```

Campos clave de cada entidad (los detalles están en `V1__create_schema.sql`):

| Entidad | Campos principales |
|---|---|
| `Venue` | `code` (único), `name`, `city`, `address`, `capacity` (> 0), `active` |
| `Event` | `eventCode` (único), `name`, `description`, `category`, `status`, `eventDate`, `minimumAge`, `streamingUrl` (opcional), `venue` |
| `Artist` | `stageName` (único sin distinguir mayúsculas), `country`, `genre`, `active` |
| `User` | `username` (único), `email` (único sin distinguir mayúsculas), `active`; sus datos personales viven en `UserProfile` |
| `Ticket` | `ticketCode` (único), `type`, `price` (≥ 0), `status`, `purchaseDate`, `user`, `event` |

Estados de evento: `DRAFT`, `PUBLISHED`, `SOLD_OUT`, `CANCELLED`, `FINISHED`. Estados de ticket: `RESERVED`, `PAID`, `CANCELLED`, `USED`.

## Consultas de los repositories

Todos los repositories extienden `JpaRepository`. Las consultas se resuelven con *query methods* de Spring Data y, cuando hace falta un `JOIN`, con JPQL mediante `@Query` (sin SQL nativo).

| Repository | Consulta | Para qué sirve |
|---|---|---|
| `VenueRepository` | `findByCode` | Buscar una sede por su código. |
| | `findByActiveTrueOrderByNameAsc` | Sedes activas ordenadas por nombre. |
| `ArtistRepository` | `findByStageNameIgnoreCase` | Buscar un artista por nombre artístico sin distinguir mayúsculas. |
| | `findByActiveTrueOrderByStageNameAsc` | Artistas activos ordenados por nombre. |
| `EventRepository` | `findByEventCode`, `existsByEventCode` | Buscar o comprobar un evento por código. |
| | `findByStatusOrderByEventDateAsc` | Eventos de un estado, por fecha ascendente. |
| | `findByVenueCode` | Eventos de una sede. |
| | `findByArtistStageName` (JPQL) | Eventos de un artista, sin distinguir mayúsculas. |
| | `findByArtistStageNameAndStatus` (JPQL) | Eventos de un artista en un estado, por ejemplo solo publicados. |
| | `findByCityAndArtistStageName` (JPQL) | Eventos de un artista en una ciudad. |
| | `findRecommendedEvents` (JPQL) | Eventos futuros de un estado, ciudad y texto de artista, por fecha. |
| `UserRepository` | `findByEmailIgnoreCase`, `existsByEmailIgnoreCase` | Buscar o comprobar un usuario por correo, sin distinguir mayúsculas. |
| | `findByUsername`, `existsByUsername` | Buscar o comprobar un usuario por nombre de usuario. |
| `TicketRepository` | `findByTicketCode` | Buscar un ticket por código. |
| | `findByUserEmailIgnoreCaseOrderByPurchaseDateDesc` | Tickets de un usuario, del más reciente al más antiguo. |
| | `findByUserEmailIgnoreCaseAndStatus` | Tickets de un usuario en un estado. |
| | `findByEventEventCodeAndStatus` | Tickets de un evento en un estado, por ejemplo los pagados. |
| | `countByEventEventCodeAndStatus`, `countByEventCodeAndStatus` (JPQL) | Contar los tickets de un evento en un estado; sirve para validar la capacidad. |
| | `findByEventEventDateAfterOrderByEventEventDateAsc` | Tickets de eventos posteriores a una fecha. |

Cada consulta tiene una prueba de integración con PostgreSQL real: `VenuePersistenceIT`, `EventRepositoryIT`, `EventArtistIT`, `UserProfileIT`, `TicketRepositoryIT`, `TicketRelationshipIT`, `EventSearchIT`, `ArtistCaseInsensitivityIT` y `RepositoryQueriesIT`.

## Pruebas

| Tipo | Dónde | Qué comprueba |
|---|---|---|
| Unitarias de servicio | `service/*ServiceImplTest` | Reglas de negocio con Mockito, sin Spring. |
| Unitarias de validación | `dto/`, `security/JwtServiceTest` | Reglas de contraseña y validación de tokens JWT. |
| Controller | `controller/*ControllerTest`, `exception/GlobalExceptionHandlerTest` | Códigos HTTP y cuerpos JSON con `@WebMvcTest`. |
| Integración | `*IT` | Persistencia, migraciones y seguridad contra PostgreSQL real. `SecurityIT` recorre las 22 rutas por rol y los casos negativos del filtro JWT. |

## Alcance actual

Esta versión incluye persistencia, servicios, API REST, seguridad con Spring Security y JWT, y pruebas. Todavía no incluye el frontend ni la colección de pruebas end-to-end con Postman.
