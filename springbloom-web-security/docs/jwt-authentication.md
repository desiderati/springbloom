# JWT Authentication

This document provides detailed information about the JWT (JSON Web Token) authentication functionality
provided by the `springbloom-web-security` module.

## Overview

JWT (JSON Web Token) is an open standard (RFC 7519) that defines a compact and self-contained way
for securely transmitting information between parties as a JSON object. This information can be verified
and trusted because it is digitally signed.

The `springbloom-web-security` module provides a comprehensive implementation of JWT authentication
for Spring-based applications, allowing for secure, stateless authentication.

## Key Features

- Self-contained JWT authentication
- Configurable token expiration
- Support for various encryption methods
- Multi-tenant support
- Integration with Spring Security and OAuth2 Resource Server

## Configuration

### Basic Configuration

To enable JWT authentication, you need to create a `jwt.pub` and `jwt.key` files in your
application's `/src/main/resources` directory and add the following property in your `application.properties`
or `application.yml` file:

```properties
# It defines if JWT Authentication is enabled or not. Process of retrieving the user/password
# from the Request Body and verifying if the password matches. Here we will get all roles
# configured for the user.
spring.web.security.jwt.authentication.enabled=true
```

You can generate the necessary keys using the following commands:

```bash
# Generate RSA key pair
ssh-keygen -t rsa -b 4096 -m PEM -f jwtRS512.key

# Extract public key
openssl rsa -in jwtRS512.key -pubout -outform PEM -out jwt.pub
cat jwt.pub

# Convert private key to PKCS8 format
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in jwtRS512.key -out jwt.key
cat jwt.key
```

You also need to add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Advanced Configuration

The following properties can be configured in your `application.properties` or `application.yml` file:

```properties
# Defines the issuer of the JWT tokens used for authentication.
# This value is included in the "iss" claim of each token and validated by the Resource Server.
# Must match the expected issuer URI configured in the JwtDecoder to ensure token integrity and trust.
spring.web.security.jwt.authentication.issuer=https://springbloom.dev

# Defines the expected audience value in the JWT, separating multiple values by comma.
# This value is included in the "aud" claim of each token and ensures the JWT token was issued for the right service.
# It helps prevent the use of valid tokens by unauthorized services.
#spring.web.security.jwt.authentication.audience=api.my-service.com

# The RSA private key used for signing JWT tokens when using asymmetric encryption.
#spring.web.security.jwt.authentication.keys.public-key=classpath:jwt.pub

# The RSA private key used for signing JWT tokens when using asymmetric encryption.
#spring.web.security.jwt.authentication.keys.private-key=classpath:jwt.key

# The secret key used for both signing and verifying JWT tokens when using symmetric encryption.
#spring.web.security.jwt.authentication.keys.secret-key=classpath:secret.key

# Sets the URL that determines if authentication is required.
spring.web.security.jwt.authentication.base-path-login=/authenticate

# The period of time in which the JWT token expires or terminates, in hours (-1 means no expiration).
spring.web.security.jwt.authentication.expiration-period=1

# It defines the encryption method to be used to encrypt/decrypt the JWT Token. Possible values: asymmetric, symmetric.
spring.web.security.jwt.authentication.encryption-method=asymmetric

# It defines the parameter name that will be used to extract the authorities for the JWT Token.
spring.web.security.jwt.authentication.authorities.parameter=authorities

# It defines the parameter name that will be used to extract the information
# if a JWT Token was generated for an administrator user.
spring.web.security.jwt.authentication.authorities.parameter-administrator=administrator

# Set the HTTP methods to allow, e.g., GET, POST, etc. The special value '*' allows all methods.
spring.web.security.jwt.authentication.cors.allowed-methods=POST

# Set the list of headers that a preflight request can list as allowed for use during an actual request.
# The special value '*' may be used to allow all headers. By default, all headers are allowed.
spring.web.security.jwt.authentication.cors.allowed-headers=*

# The list of allowed origins that be specific origins, e.g. 'https://domain1.com', or '*' for all origins.
# By default, all origins are allowed.
spring.web.security.jwt.authentication.cors.allowed-origins=*

# Set the list of response headers other than 'simple', headers, i.e., Cache-Control, Content-Language,
# Content-Type, Expires, Last-Modified, or Pragma, that an actual response might have and can be exposed.
# Note that '*' is not supported on this property.
spring.web.security.jwt.authentication.cors.exposed-headers=Authorization
```

## JWT Service

The module provides a `JwtService` that handles JWT token generation, validation, and parsing.
This service is automatically configured and can be injected into your components, if needed!

## JWT Authentication Filter

The module includes a `JwtAuthenticationFilter` that automatically extracts the username/password
from the request and generates the JWT token in the authentication response.
This filter is automatically configured and added to the security filter chain.

## JWT Claims

The JWT token contains the following standard claims:

- `sub`: The subject of the token (typically the username).
- `aud`: The audience (aud) claim, which identifies the recipient(s) that the JWT is intended for.
- `iss`: The issuer of the JWT tokens used for authentication.
- `jti`: The JWT ID (jti) claim, which provides a unique identifier for the JWT.
- `iat`: The time the token was issued.
- `exp`: The expiration time of the token.
- `authorities`: The authorities (roles) of the user.

You can customize the claims included inside the token by implementing
a custom `JwtAuthenticationClaimsConfigurer`:

```java
import java.util.UUID;

@Component
public class CustomJwtClaimsConfigurer implements JwtAuthenticationClaimsConfigurer {

    @Override
    public Map<String, Object> configureClaims(String username, Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        // The nonce (short for number used once) is a unique, random, and unpredictable value generated
        // for each request or token to protect against replay attacks — situations where a valid token
        // is intercepted and maliciously reused.
        claims.put("nonce", UUID.randomUUID().toString());
        return claims;
    }
}
```

## Authentication Delegation

The module supports delegating authentication to an external service. This is useful when you want to validate
tokens against an external authentication service. To enable authentication delegation, set the following properties:

```properties
# It defines if JWT Authentication Delegation is enabled or not. The process of retrieving the user/password
# from the Request Body and verifying if the password matches, it is delegated to another API.
spring.web.security.jwt.authentication.delegation.enabled=false

# It defines the base path for the JWT Authentication Delegation API.
# Must be defined if authentication delegation is enabled.
#spring.web.security.jwt.authentication.delegation.base-path-url=

# Sets the URL that determines if authentication is required for the JWT Authentication Delegation API.
spring.web.security.jwt.authentication.delegation.base-path-login=/authenticate
```

## Security Considerations

### Token Storage

JWT tokens should be stored securely on the client side. For web applications, this typically means storing
the token in an HTTP-only cookie or in the browser's local storage.

### Token Expiration

JWT tokens should have a short expiration time to minimize the risk of token theft. The default expiration
time is 1 hour (3600 seconds), but you can configure it to be shorter or longer depending on your security
requirements.

### Token Revocation

Since JWT tokens are stateless, they cannot be directly revoked once issued. To implement token revocation, you can:

1. Use short expiration times
2. Implement a token blacklist
3. Include a version number in the token that can be invalidated server-side

## Troubleshooting

### Common Issues

#### Invalid Signature

If you're seeing "Invalid signature" errors, check that:
- The public key in `jwt.pub` matches the private key used to sign the tokens.
- The encryption method (e.g., RS512) is consistent between token generation and validation.

#### Token Expired

If you're seeing "Token expired" errors, check that:
- The client is using a valid, non-expired token.
- The server and client clocks are synchronized.
- The expiration period is appropriate for your application.

#### Missing Authentication Header

If the authentication is failing because the token is not being sent, check that:
- The client is including the `Authorization` header with the `Bearer` prefix.
- There are no proxy or gateway issues stripping the header.

## Further Reading

- [JWT.io](https://jwt.io/) — Introduction to JSON Web Tokens
- [RFC 7519](https://tools.ietf.org/html/rfc7519) — JSON Web Token (JWT) specification
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html) — Spring Security is
  a framework that provides authentication, authorization, and protection against common attacks.
