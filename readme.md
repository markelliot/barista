# barista

An opinionated library to simplify making Java services and helping developers focus on business 
logic instead of server configuration and setup. By default, Barista listens on port 8443 and
enables TLS, using `var/security/key.pem` as the private key, `var/security/trust.pem` as the
trust store, and `var/security/cas.pem` as the CA certificates.

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

## Integrating with Conjure

Barista is compatible with [Conjure](https://github.com/palantir/conjure), and conjure-undertow generated
endpoint classes. To use Conjure endpoints, take an additional dependency on `barista-conjure`:

```gradle
dependencies {
    implementation "com.markelliot.barista:barista-conjure"
}
```

And use the `ConjureAdapter` class to go from conjure-undertow generated classes to
Barista-compatible endpoints:

```java
UndertowRuntime conjureRuntime = ConjureUndertowRuntime.builder().build();
Endpoints endpoints = ConjureAdapter.adapt(
        SampleServiceEndpoints.of(new DefaultSampleService()), conjureRuntime);
Server.builder()
        .endpoints(endpoints)
        ...
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

## Generating Self-signed Certificates

Create a `domains.ext` file:
```
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
# Add more domains as necessary
# DNS.2 = myotherdomain.local 
```

And then generate the `cas.pem`, `key.pem` and `trust.pem` files:
```bash
# Create the root CA
openssl req -x509 -nodes -new -sha256 -days 1024 -newkey rsa:2048 -keyout cas.key -out cas.pem -subj "/C=US/CN=Barista-Root-CA"

# Generate the keypair
openssl req -new -nodes -newkey rsa:2048 -keyout localhost.key -out localhost.csr -subj "/C=US/ST=YourState/L=YourCity/O=Example-Certificates/CN=localhost" -reqexts SAN -config <(cat /etc/ssl/openssl.cnf <(printf "[SAN]\nsubjectAltName = DNS:localhost"))

# Generate the certificate
openssl x509 -req -sha256 -days 1024 -in localhost.csr -CA cas.pem -CAkey cas.key -CAcreateserial -extfile domains.ext -out localhost.crt

# Render the key and cert into the key file, and link the trust file to the CAs file.
cat localhost.key localhost.crt > key.pem
cp cas.pem trust.pem
```
