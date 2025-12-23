[![Build Status](https://github.com/desiderati/springbloom/workflows/Build/badge.svg)](https://github.com/desiderati/springbloom/actions)
[![Version](https://img.shields.io/badge/Version-4.0.0.RC10-red.svg)](https://github.com/desiderati/springbloom/releases)
[![GitHub Stars](https://img.shields.io/github/stars/desiderati/springbloom.svg?label=GitHub%20Stars)](https://github.com/desiderati/springbloom/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

# Spring Bloom — Tools to let your Spring projects blossom 🌱

Spring Bloom is a collection of open source libraries that help you **accelerate the creation of
Spring-based applications** with clarity, consistency, and minimal boilerplate.

## Features
- Modular libraries for reusable building blocks
- Ready-to-use configurations for common Spring patterns
- Rapid setup for new microservices
- Focus on code clarity and maintainability

## Why Spring Bloom?
Just like Spring makes Java development more pleasant, **Spring Bloom helps your ideas blossom faster**,
removing repetitive setup steps so you can focus on solving real problems.

## Philosophy
Spring Bloom embraces the “convention over configuration” mindset, aiming to:
- Reduce friction when starting a project
- Improve collaboration in multi-team setups
- Enforce good practices without extra complexity

## Documented Modules

* [springbloom-core](springbloom-core/README.md)
* [springbloom-google](springbloom-google/README.md)
* [springbloom-jms](springbloom-jms/README.md)
* [springbloom-logging](springbloom-logging/README.md)
* [springbloom-logging-test](springbloom-logging-test/README.md)
* [springbloom-web](springbloom-web/README.md)
* [springbloom-web-security](springbloom-web-security/README.md)

## Author

Felipe Desiderati <felipedesiderati@springbloom.dev> (https://github.com/desiderati)

## Changelog

All project changes will be documented in this file.

#### [4.0.0-RC9] - 2025-08-01
- Reorganized security module structure for better organization and clarity:
  * Moved authentication-related classes to `dev.springbloom.web.security.auth` package
  * Grouped JWT, OAuth2, and sign request components in dedicated subpackages
  * Relocated GraphQL security components to `dev.springbloom.web.security.graphql` package
- Added new security features:
  * Created `IsAdministrator` annotation for admin authorization checks
  * Added `AuthenticatedUsernameArgumentResolver` for resolving usernames in controllers
  * Implemented `SecurityGraphQLExceptionHandlerController` for GraphQL security exception handling
- Added internationalization support for security exceptions:
  * Added English (US) and Portuguese (BR) translations

#### [4.0.0.RC8] - 2025-07-15
- Added `UrlUtils` class with comprehensive URL manipulation and standardization methods:
  * `sanitizeUrl()` - Removes duplicate separators and trailing separators
  * `sanitize()` - Ensures URLs start with a separator and removes duplicates
  * `appendDoubleAsterisk()` - Adds "/**" to paths for wildcard URL matching
  * `appendSlash()` - Ensures URLs end with a trailing slash
- Added comprehensive test suite for URL utilities

#### [4.0.0.RC7] - 2025-06-08
- Added `DefaultEmail` validation annotation and validator.
- Updated copyright headers across files.
- Enhanced Javadoc/comments for clarity.

#### [4.0.0.RC6] - 2025-05-01
- Rebranding to **SpringBloom.dev**.
- Migrate project structure, packages, and identifiers from `io.herd.common` to `dev.springbloom`,
  ensuring consistent naming across modules, resources, and configurations.
- Removed legacy files and improved project clarity and organization.

#### [4.0.0.RC5] - 2025-05-30
- Improved documentation in `common-web-security` module:
  * Added documentation for JWT authentication.
  * Added documentation for sign request functionality.
  * Added documentation for user data resolver.
- Enhanced security components in `common-web-security` module:
  * Renamed **SignRequestAuthorizationException** to **SignRequestException**.
  * Renamed **SignRequestAuthorizedClientRepository** to **SignRequestAuthorizationClientRepository**.
  * Improved **JwtService** implementation.
- Improved notification system in `common-web-notification` module:
  * Enhanced **NotificationService** implementation.
  * Updated SpringDoc properties configuration.
- Moved Google Calendar API secret from main resources to test resources in `common-google` module.

#### [4.0.0.RC4] - 2025-04-15
- Enhanced notification system in `common-web-notification` module:
  * Improved **BroadcastController** and **NotificationController**.
  * Refactored **NotificationService** implementation.
  * Added SpringDoc properties configuration.
- Significant security improvements in `common-web-security` module:
  * Added **HttpClientsSecurityConfiguration** for secure client connections.
  * Created **WebSecurityCorsConfiguration** for better CORS handling.
  * Added **CsrfTokenController** for CSRF protection.
  * Enhanced JWT service and security configurations.
- Web module enhancements:
  * Updated OpenAPI configuration.
  * Improved SpringDoc properties.
  * Enhanced **WebAutoConfiguration**.
- Validation improvements:
  * Enhanced **TypedValidationEntity** in `common` module.
  * Updated corresponding tests.

#### [4.0.0.RC3] - 2025-03-21
- Introduced annotations and resolvers for handling user authentication and user data in web and GraphQL contexts
- Added stricter error handling in JWT authentication and improved maintainability by consolidating configurations
  with Spring Security’s OAuth2 integration.

#### [4.0.0.RC2] - 2025-03-17
- Remove the legacy JWT implementation in favor of Spring's OAuth2 support.
- Removed custom JWT classes and configurations, consolidating the project to use
  Spring Security's OAuth2 JWT Resource Server integration. This improves maintainability, reduces redundancy,
  and aligns with widely accepted security standards. Also updated property mappings to reflect this change.
- Added the possibility of configuring the charset encoding for i18n files.
- Renamed **RestApiClientConfiguration** to **HttpClientsConfiguration**.
- Created a custom **HandlerMethodArgumentResolver** for resolving Pageable method parameters
  in Spring GraphQL controllers.
- Initial idea for a custom OAuth2 Service.

#### [4.0.0.RC1] - 2025-02-27
- Created a class which provides static access to the Spring application context.
- Defined a mechanism for context propagation when working with asynchronous tasks our suspended methods.
- Defined a way of retrieving (statically) an internationalized message.
- Defined a way to validate entities using typed errors and _ArrowKt_.
- Added an ability to verify if the authenticated used is an administrator.
- Updated Copyright
- Migrated to Spring GraphQL.
  * Configured the Altair and Voyager for GraphQL.
  * Defined a way of registering custom scalars automatically.
  * Better configuration for `@Pageable` directive.

#### [3.3.0.RELEASE] - 2025-01-23
- Added support to retrieve the authorities passed by the delegated authentication provider.
- Added support for 'pre/post' authorizations on GraphQL calls.
- Added Hamkrest and Spek as test dependencies.
- Added initial support for ArrowKt.
- Added the pageable functionality for GraphQL.
- Removed dependency with `org.reflections`.
- Updated some dependency versions.
- Update the copyright.
- Updated the Java version on `buildspec.yml`.
- Maven version should be 3.8.8 because of GitHub Actions.
- Created the **BroadcastController** on module `common-web-notification`.
- Created the module `common-web-notification-client`.
- Renamed AbstractEntity.kt to **AbstractIdentity.kt**.
- Revised all module `common-jms`.
- Fixed the input in `pageable.graphqls`.
- Better Liquibase Maven Plugin integration.
- Updated the dependencies. Fixed some typos. Fixed minor warnings.
- Enabled a way of setting custom properties for a test annotated with `@ServiceJpaTest`.
- Created a class which provides static access to the Spring application context.
- Defined a mechanism for context propagation when working with asynchronous tasks our suspended methods.
- Defined a way of retrieving (statically) an internationalized message.
- Added an ability to verify if the authenticated used is an administrator.

#### [3.2.1.RELEASE] - 2023-12-13
- Added support to create request scoped beans within async threads.

#### [3.2.0.RELEASE] - 2023-11-29
- Some improvements on Google Captcha Service.
- Added support to RESTful API client configuration.
- Defined a default **ObjectMapper**.
- Code-review.

#### [3.1.3.RELEASE] - 2023-11-29
- Some improvements on GraphQL asynchronous configuration.

#### [3.1.2.RELEASE] - 2023-11-27
- Some improvements on GraphQL ErrorHandling.

#### [3.1.1.RELEASE] - 2023-11-23
- Now the notification message for Atmosphere can be any object.
- Added the Void GraphQL Scalar Type.
- Some improvements in the security configuration involving the GraphQL.
- JWT Token can be configured without expiration.

#### [3.1.0.RELEASE] - 2023-10-30
- Added support for JWT Authentication Delegation.
- Added the class **GoogleCaptchaService**.

#### [3.0.1.RELEASE] - 2023-08-02
- Fixed a minor bug involving the CORS configuration and circular dependencies.

#### [3.0.0.RELEASE] - 2023-04-03
- Added Spring Cloud Support.
- Migrated from Apache ActiveMQ to Apache Artemis.
- Migrated from Swagger to Open API.
- Migration from JavaX project to Jakarta EE.
- Migration to Spring Boot 3 and Spring 6.
- New Version of Mongo Driver.
- Removed support from Swagger, added support for Open API.
- Support for Gradle 8.0.2 and Maven 3.9.1.
- Support for Java 17 and Kotlin 1.18.
- Support for the Lombok Project integrated with Kotlin.

#### [2.5.2] - 2022-10-19
- Fixed a Bug with the **ID** property on **AbstractEntity** class.

#### [2.5.1] - 2022-10-05
- Fixed a Bug in the `equals(...)` method on **AbstractEntity** class.

#### [2.5.0] - 2022-10-04
- Fixed a Bug involving the auto-configuration order between **WebAutoConfiguration**
  and **HibernateJpaAutoConfiguration**.
- Created the modules: `common-data-jpa`, `common-data-multitenant`, `common-web`,
  `common-web-notification` and `common-web-security`.

#### [2.4.5] - 2022-09-22
- Configured a new exception handler for GraphQL.
- Moved some classes to package: `io.herd.common.web`.
- Renamed package from `io.herd.common.tenant`, to: `io.herd.common.data.multitenant`.
- Fixed a bug while compiling both Kotlin and Java files in the same module.

#### [2.4.4] - 2021-09-17
- Fixed a bug in the **ExceptionHandlingController** related to `shouldLogAsWarning` functionality.

#### [2.4.3] - 2021-09-15
- Improvements in better logging while using AWS.

#### [2.4.2] - 2021-09-14
- Added the extra args to **ResponseExceptionDTO**.
  Fixed a bug in the **ExceptionHandlingController** related to **ApiException** handling.

#### [2.4.1] - 2021-08-18
- Added support to Spring + Kotlin. Changed the GraphQL version.

#### [2.4.0] - 2021-08-13
- Enabled the support to asynchronous processing.

#### [2.3.23] - 2021-08-11
- Enabled the option to use the Log Pattern when configured through `application.properties`.

#### [2.3.22] - 2021-08-05
- Now it's possible to define the exception message which should be recorded in the log as a warning.

#### [2.3.21] - 2021-05-31
- Now it's possible to define an exception list which should be recorded in the log as a warning.

#### [2.3.20] - 2020-08-12
- Node 12.18.2 and NPM 6.14.5.
- SpringFox v3.0.0.

#### [2.3.19] - 2020-07-01
- Replaced the annotation `@EnableSwagger2WebMvc` by `@EnableOpenApi`.

#### [2.3.18] - 2020-06-30
- Created the class **AutoConfigureCommonWeb**, which would be used mostly during tests.
- Moved from Swagger Specification (v2) to OpenAPI Specification (v3).

#### [2.3.17] - 2020-06-26
- Fixed a bug with the Springfox Swagger (The webjars path has changed).

#### [2.3.16] - 2020-06-26
- Fixed a bug with the Springfox Swagger.

#### [2.3.15] - 2020-06-26
- Fixed a bug with the **MockitoLoader**, and the **ServiceJpaTest** classes.

#### [2.3.14] - 2020-06-19
- Now the Thymeleaf configuration uses the same **MessageSource** defined by default.

#### [2.3.13] - 2020-06-15
- Added the property: `${swagger.generation.keep-superfluous-files}`

#### [2.3.12] - 2020-06-11
- Reverting version: 2.3.11.

#### [2.3.11] - 2020-06-10
- Removed the classes: **SignRequestWrapper** and **SignRequestServletInputStream**.
  Now we are using: **ContentCachingRequestWrapper**!

#### [2.3.10] - 2020-06-10
- Added support to PushOver Notifications.

#### [2.3.9] - 2020-06-07
- Configured the log level for some classes to avoid verbosity.
- Better Swagger Api exception handling.
- Created a new converter to handle messages while logging on AWS CloudWatch.

#### [2.3.8] - 2020-05-21
- Added handling for **UndeclaredThrowableException**.

#### [2.3.7] - 2020-05-19
- Some code review.

#### [2.3.6] - 2020-05-14
- Fixed a bug related to the `banner.txt` file creation.
  There's a bug with the Maven Flatten Plugin, which replaces the placeholders when defined inside the `<profile>` tag.

#### [2.3.5] - 2020-05-08
- Configured the Maven Flatten Plugin. The `${revision}` placeholder will only work if you use this plugin.
  See: https://blog.soebes.de/blog/2017/04/02/maven-pom-files-without-a-version-in-it/

#### [2.3.4] - 2020-05-08 (Do not use this version, it will not work!)
- Configured the `${revision}` placeholder.
- Configured the Maven Version Plugin, which will be responsible for updating the POM version.

#### [2.3.3] - 2020-04-30
- Created the functionality of cleaning up the database state between tests.

#### [2.3.2] - 2020-04-22
- Better Undertow configuration.

#### [2.3.1] - 2020-04-15
- Better Spring Security configuration.

#### [2.3.0] - 2020-04-15
- Spring 5.3.5 and Spring Boot 2.2.6. Other dependencies update.
- Node 12.16.2 and NPM 6.14.4.
- Fixed a bug related to Swagger configuration. It was not taking into account the fact that the context path
  could be different from `/`.
- Fixed a bug related to Commons Herd.io Notification's auto-configuration.
- Fixed a bug when the table prefix was configured blank. See: **DefaultPhysicalNamingStrategy**.
- Created class **MongoContainer**.
- Changed the **MockJwtAuthorizedUserSecurityContextFactory**. Now, it's mandatory the JWT token bean configuration.

#### [2.2.10] - 2020-04-08
- Better auto configuration setup. Aiming to execute test slicing technique available on Spring Boot correctly.
- Created the classes **ServiceJpaTest** and **MockitoLoader**.
- Support to DBUnit.

#### [2.2.9] - 2020-04-08
- Fixed a bug with the Hikari and Liquibase.
- Some Undertow tweaks.
- Better Api Base Path sanitization.

#### [2.2.8] - 2020-04-02
- Undertow access log configuration.

#### [2.2.7] - 2020-04-02
- Fully translated to English.

#### [2.2.6] - 2020-04-01
- Fixed a bug related to `@ComponentScan` annotation while using it with **WebMvcTest** annotation.

#### [2.2.5] - 2020-04-01
- Fixed a bug related to `@ComponentScan` annotation.

#### [2.2.4] - 2020-04-01
- Fixed a problem while generating the file `spring-configuration-metadata.json`.
  Custom properties weren't being recognized.

#### [2.2.3] - 2020-03-31
- **WebSecurityConfigurerAdapterAutoConfiguration** will only be configured if there's no other bean
  of the same type inside the application context.

#### [2.2.2] - 2020-03-31
- **ResponseExceptionDTOHttpMessageConverter** must always return an `application/json`.
  It will ensure when the controller throws an exception, even if the response content-type is different
  from `application/json`, the response entity will be configured to return an `application/json` content type.

#### [2.2.1] - 2020-03-30
- Support for Apollo GraphQL.
- Added a custom **ResponseExceptionDTOHttpMessageConverter**.
- Updated Commons Compress: 1.8 → 1.19.

#### [2.2.0] - 2020-03-25
- Migration to Java 11.
- Better Spring Boot auto configuration. Removed **EnableAutoConfiguration** annotations.
- Better rethrowing functionality. See **ThrowingConsumer** for further information.
- Some language translations.
- Minor code review.
- Better _Multi Tenancy_ support.

#### [2.1.18] - 2020-03-23
- Fixed a Bug related to allowing calls to the public API.

#### [2.1.17] - 2020-03-21
- Added support to MapStruct.
- Now it is possible to use a different profile when starting the application to access the Swagger API.

#### [2.1.16] - 2020-03-20
- Fixed a bug related when an exception was thrown before the **ExceptionHandlingFilter**, generating
  this way a response message not readable by the **ExceptionHandlingController**. Now if the response body
  could not be deserialized into a **ResponseExceptionDTO**, it will be deserialized using a `Map<String, Object>`.
- Now it is possible to disable the default authentication.

#### [2.1.15] - 2020-03-19
- ~~Fixed a bug related to the authentication. We must set authentication as **true** after calling
  the authentication services (JWT and Sign Request).~~

#### [2.1.14] - 2020-03-11
- Added support to mail messages.
- Added support to Apache Commons Validator and Apache Commons Compress.
- Added support for symmetric encryption while handling JWT Tokens.
- Fixed a bug related to how the default behavior was managed by the customized **WebSecurityAutoConfiguration**.

#### [2.1.13] - 2020-03-04
- Added support to CORS configuration through the `application.properties` file.

#### [2.1.12] - 2020-02-21
- Now there is no need to add the Commons Herd.io version in children's projects.

#### [2.1.11] - 2020-02-21
- Added the class **MockSignRequestAuthorizedClient**.
- Fixed some minor bugs regarding the **JwtAuthorizationService** class instantiation.

#### [2.1.10] - 2020-02-20
- Changed the **ExceptionHandlingController** class to add support to **HttpStatusCodeException**.
- Better logger information while printing exceptions handled by the Swagger Client.
- Fixed a Bug while reading the `swagger-client.properties` file. Prefix was not specified.
- Now the class **SignRequestAuthorizationService** is enabled by default, as it can be used to sign requests
  when used by a Swagger client.
- Now it is possible to add additional properties to the **SignRequestAuthorizedClient**.

#### [2.1.9] - 2020-02-11
- Added the **SecurityExceptionHandlingController** class.
- Added support to authorities and credentials on JWT Authentication/Authorization Component.
- Now it is possible to configure the expiration period while generating the JWT Token.

#### [2.1.8] - 2020-01-31
- Added the **AwsExtendedThrowableProxyConverter** class, which is responsible for grouping the stack trace
  in just one message while using the AWS Cloud Watch.

#### [2.1.7] - 2020-01-30
- Fixed some **Sonarlint** issues. Better **Hikari** configuration.
- Better name for property: `commons-herd.io.version`.

#### [2.1.6] - 2020-01-27
- Added support to the use of profiles with **LogBack**.

#### [2.1.5] - 2020-01-27
- Disabled default log configuration. This was an erroneous deployment!

#### [2.1.4] - 2020-01-25
- Better Swagger Thin Server configuration.

#### [2.1.3] - 2020-01-24
- Added an empty `application.properties` file.

#### [2.1.2] - 2020-01-22
- Fixed a bug related to refreshing properties and local validation.

#### [2.1.1] - 2020-01-22
- Added support for default properties.
- Added support for refreshable properties.
- Added dependency with Spring Cloud Starter.
- Minor changes.
- Improved the Swagger client properties configuration. Now there is no need of extending
  the **SwaggerClientProperties** class.
- Changed the **DefaultPhysicalNamingStrategy** class. Now there is no need of extending it,
  just to configure the property `app.database.table-prefix`.
- Better messages while handling authorization and authentication.

#### [2.1.0] - 2020-01-16
- Added support to Sign Request Authorization.
- Added support to JWT Authorization.

#### [2.0.2] - 2020-01-14
- Now the base path configuration is also applied to Spring Data Rest Repositories.
  Fixed Bug: [The configuration of the default path ("/api") is not working
  while using Spring Data Rest](https://github.com/desiderati/springbloom/issues/2)

- New Spring Boot Version 2.2.2.
- Some translation and minor fixes.
- Better Swagger support. Now it will be available by default.
  Fixed Bug: [Swagger auto-configuration not working
  with Spring Data Rest](https://github.com/desiderati/springbloom/issues/4)

- Support for new exceptions: **ResourceNotFoundException** and **HttpRequestMethodNotSupportedException**.
- Added support to Spring Actuator for all Spring Web applications.

#### [2.0.1] - 2020-01-11
- Now it is possible to configure the **EntityScan** and **JpaRepositoryScan**.

#### [2.0.0] - 2020-01-10
- Improved file generation for Swagger clients: it is now possible to download the OpenAPI JSON specification
  directly from the server application.
- CRC16 calculation feature.
- Improvements in the class **ExceptionHandlingController**: handling of new exceptions.
- MIT license added.
- Creation of the **CustomRepositoryRestConfiguration** class.
- Spring Data Rest support.
- Support for Java 11.
- Creation of **ThrowingRunnable** class.
- Creating the annotation for tests in the service layer.
- Implementation of the _Multi Tenancy_ feature.
- Creation of **ThrowingConsumer**, **ThrowingSupplier** and **ThrowingFunction** classes.
- Creation of a `common-parent-static` module.
- Addition of standard error pages. (**40X.html** and **50X.html**)
- Improvements in the way we treat null parameters in **Repository** classes.
- Improvements in the **ExceptionHandlingController** class: corrections related to the way we handle exceptions.
  Separation between exceptions for controllers and services.
- Migration to Spring 5 and SpringBoot 2. Including dependencies.
- Application configuration improvements (Auto Configuration).
- Improvements in `pom.xml`.
- Improvements in the notification component. It will now be possible to handle more than one broadcast
  per resource. And vice versa!

#### [1.1.0] - 2018-07-05
- Improvements to log messages.
- The **ModelMapper** beans are configured with the prototype scope.
- Correction of a bug that occurred when handling **ApiException**.

#### [1.0.0] - 2018-06-27
- Creation of Google Calendar Service.
- Creation of a **MultipartRequest** class.
- Creation of CPF/CNPJ field validations.
- Creation of the generic exception handling functionality. (**ExceptionHandlingController**)
- Creation of JPA repositories to handle null method parameters (_Null-Safe Parameters_), including MongoDB.
- Creation of **AbstractRepository** class to help in the creation of personalized data repositories.
- Creation of **AbstractPersistableIdentity** class to help in the creation of JPA entities.
- Creation of the functionality responsible for defining the name strategy to use with databases. (_Naming Strategy_)
- Creation of classes to use with automated tests.
- Added support for internationalization.
- Added support for templates: Thymeleaf Framework.
- Added support for Swagger.
- Added support for default application configuration via Spring Boot.
- Added support for Liquibase.
- Added support for Atmosphere.
- Added support for queue management.
- Added support for default log configuration.
- Added support for QR Code readers.
- Added security support via JWT.
