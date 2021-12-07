# barista

An opinionated library to simplify making Java services and helping developers focus on business 
logic instead of server configuration and setup. By default, Barista listens on port 8443 and
enables TLS, using `var/security/key.pem` as the private key and `var/security/trust.pem` as the
trust store and CA certificates.

Depend on barista via Maven Central at coordinates 
[`com.markelliot.barista:barista:<version>`](https://search.maven.org/artifact/com.markelliot.barista/barista).

## Authoring Endpoints
Endpoints implement either the `Endpoints.VerifiedAuth` or `Endpoints.Open` classes. The former
causes Barista to validate and pass as an argument the authz token for the request and the latter 
performs no such validation.

A greeter endpoint not requiring any form of authentication or authorization might be
implemented as follows:
```java
/** An endpoint that returns a greeting parameterized by the request's name field. */
public final class GreeterEndpoint implements Endpoints.Open<Request, Response> {
    public record Request(String name) {}
    public record Response(String string) {}
    
    public Response call(Request request) {
        return new Response("Hello " + request.name + "!");
    }
    
    public Class<Request> requestClass() {
        return Request.class;
    }
    
    public String path() {
        return "/api/hello-world";
    }
    
    public HttpMethod method() {
        return HttpMethod.PUT;
    }
}
```

## Creating and Starting the Server

Then, to create and start the server, one can use the `Server` builder to register the endpoint,
correctly set allowed CORS origins and then ultimately start the server: 
```java
Authz authz = Authz.denyAll(); // not using authz, so this is a no-op implementation
Server.builder()
    .authz(authz)
    .endpoint(new GreeterEndpoint())
    .allowOrigin("https://example.com")
    .allowOrigin("http://localhost:8080") // for development
    .start();
```
