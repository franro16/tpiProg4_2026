-Sistema de Subastas Online con Cierre Temporal y Puja Segura

- Integrantes

- Victoria Basso Peretti
- Franco Romero

- Descripción

Este proyecto corresponde al Trabajo Práctico Integrador de Programación IV.  
Consiste en el desarrollo de un sistema de subastas online donde los usuarios pueden registrarse, iniciar sesión, publicar productos, crear subastas, realizar pujas y gestionar disputas.

El sistema fue desarrollado principalmente como backend mediante una API REST, incorporando seguridad, manejo de roles, reglas de negocio, control de concurrencia y persistencia en base de datos. Además, se agregó una interfaz web básica para interactuar con las funcionalidades principales.

- Tecnologías utilizadas

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- PostgreSQL
- Flyway
- Maven
- Lombok
- HTML, CSS y JavaScript

- Roles del sistema

El sistema contempla los siguientes roles:

- `USER`: puede consultar subastas y realizar pujas.
- `SELLER`: puede crear productos y subastas.
- `ADMIN`: puede gestionar usuarios, categorías y resolver disputas.

Un usuario puede tener más de un rol.

- Funcionalidades principales

- Registro e inicio de sesión de usuarios.
- Autenticación mediante JWT.
- Contraseñas almacenadas con hash seguro.
- Bloqueo temporal por intentos fallidos de login.
- Gestión de productos y categorías.
- Creación, publicación y cancelación de subastas.
- Cambio automático de estados de subasta.
- Registro de pujas con validaciones de negocio.
- Control de concurrencia en pujas mediante locking pesimista.
- Determinación de ganador.
- Registro de historial de cambios de estado.
- Notificaciones internas en la aplicación.
- Apertura y resolución de disputas.
- Manejo global de errores.

- Estados de una subasta

Las subastas pueden encontrarse en los siguientes estados:

- `BORRADOR`
- `PUBLICADA`
- `ACTIVA`
- `FINALIZADA`
- `CANCELADA`
- `ADJUDICADA`
- `EN_DISPUTA`

Los cambios de estado se realizan respetando las reglas de negocio y quedan registrados en el historial de estados.

-Estructura del proyecto

El proyecto está organizado por capas:

- `controller`: expone los endpoints de la API.
- `service`: contiene la lógica de negocio.
- `repository`: gestiona el acceso a la base de datos.
- `domain/entity`: contiene las entidades JPA.
- `domain/enums`: contiene los enums del sistema.
- `dto`: contiene los objetos de entrada y salida de datos.
- `mapper`: convierte entidades en respuestas DTO.
- `security`: contiene la configuración de JWT y autenticación.
- `config`: contiene configuraciones generales del proyecto.
- `exception`: contiene el manejo de errores personalizado.
- `scheduler`: contiene las tareas automáticas de subastas.
- `db/migration`: contiene los scripts Flyway.
- `static`: contiene el frontend básico.

-Configuración de base de datos

El proyecto utiliza PostgreSQL.  
Antes de ejecutar la aplicación, crear una base de datos local y configurar el archivo:

```properties
src/main/resources/application.properties

-Ejemplo de configuración:

spring.datasource.url=jdbc:postgresql://localhost:5432/subastas_db
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD

Reemplazar TU_PASSWORD por la contraseña correspondiente de PostgreSQL.

-Ejecución del proyecto

Desde la raíz del proyecto ejecutar:

mvn clean install

-Luego iniciar la aplicación con:

mvn spring-boot:run

-La aplicación estará disponible en:

http://localhost:8080
Frontend

-La interfaz web básica se encuentra en:

src/main/resources/static

-Se puede acceder desde el navegador ingresando a:

http://localhost:8080
Swagger

-La documentación de la API puede consultarse desde:

http://localhost:8080/swagger-ui/index.html

-Decisiones técnicas destacadas

Se utilizó una arquitectura por capas para separar responsabilidades.
Se implementó seguridad con JWT y control de permisos por rol.
Las contraseñas se almacenan utilizando BCrypt.
La base de datos se versiona mediante Flyway.
Las pujas se manejan de forma transaccional.
Se utiliza locking pesimista para evitar inconsistencias ante pujas simultáneas.
Los cambios automáticos de estado se realizan desde el backend mediante un scheduler.
Las notificaciones se guardan en base de datos y se consultan desde la aplicación.
