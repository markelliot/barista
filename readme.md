# barista

An opinionated library to simplify making Java services and helping developers focus on business 
logic instead of server configuration and setup. By default, Barista listens on port 8443 and
enables TLS, using `var/security/key.pem` as the private key and `var/security/trust.pem` as the
trust store and CA certificates.

Depend on barista via Maven Central at coordinates 
[`com.markelliot.barista:barista:<version>`](https://search.maven.org/artifact/com.markelliot.barista/barista).

## Endpoints
Barista serves HTTP endpoints that:
 - use an HTTP method that's one of `GET`, `PUT`, `POST`, `DELETE`
 - serve requests on a path-like HTTP route
 - support path, querystring, header, and cookie parameters
 - support enforced user authentication

Generally, users of Barista use a code generator to make endpoint implementation, and Barista
comes with a built-in code generator. The built-in system works using Java's annotation processors.

To configure the annotation processor in Gradle:
```gradle
dependencies {
    implementation "com.markelliot.barista:barista-annotations"
    annotationProcessor "com.markelliot.barista:barista-annotations"
    annotationProcessor "com.markelliot.barista:barista-processor"
}
```

Then, use one of `@Http.{Get, Put, Post, Delete}` to label any method in any class. Labeled
methods from the same class will produce a class in the same package with the name
`<OriginalClassName>Endpoints` that implements `com.markelliot.barista.endpoints.Endpoints` and
collects labeled methods from `OriginalClassName`. `<OriginalClassName>Endpoints` comes with
a single, public constructor that accepts an implementation of `OriginalClassName`.

To register these endpoints with Barista, construct `<OriginalClassName>Endpoints`, pass it
an instance of `OriginalClassName`, and then call
`Server.builder()...endpoints(<instanceOfEndpoints>)`.

Customize endpoint function by:
 - HTTP method: select the matching annotation
 - Require authentication: include a method argument with type `VerifiedAuthToken`
 - Use a path parameter: specify path parameters in the route using `{placeholder}`, name arguments
   to the method with names that correspond to `placeholder`.
 - Use a querystring, header or cookie parameter: specify additional arguments to the method
   and annotate the arguments with `@Query("queryParamName") String param`, `@Header` or `@Cookie`.
   Use `Optional<String>` to allow these parameters to be optional and `String` to require the
   presence of the parameters.

   (Note: today, querystring, header and cookie parameters may only be of type `String`)
 - Use up to one unannotated non-path parameter as the body; Barista's code-generator will attempt
   to deserialize incoming requests to the type of this parameter.

   (Generics are not presently supported. `InputStream` is not presently supported.)
 - The return type of the method dictates behavior of the endpoint:
   - `HttpRedirect`: redirect according to the logic encoded by an instance of `HttpRedirect`
   - `void`: send an empty response with HTTP status 201 if the method does not error when executed
   - any concrete `T`: serialize the response with status code 200

   Future contemplated return types include:
   - `HttpResponse`: a richer return format that includes a body, headers and cookies
   - `InputStream`: a producer of raw bytes to write into the response

Some endpoint examples may be found in the
[processor tests](/barista-processor/src/test/java/com/markelliot/barista/processor/FooResource.java).

An example that returns the JSON string `"Hello, world!"`:
```java
final class GreeterResource {
    /** To greet by name, call GET /greet?name=Your+Name */
    @Http.Get("/greet")
    String greet(@Query Optional<String> name) {
        return "Hello, " + name.orElse("world") + "!";
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
    .endpoints(new GreeterResourceEndpoints(new GreeterResource()))
    .allowOrigin("https://example.com")
    .allowOrigin("http://localhost:8080") // for development
    .start();
```
