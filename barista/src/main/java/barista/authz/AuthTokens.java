package barista.authz;

import java.util.regex.Pattern;

public final class AuthTokens {
    private static final Pattern BEARER = Pattern.compile("Bearer\s+");

    private AuthTokens() {}

    public static AuthToken fromAuthorizationHeader(String headerValue) {
        return new AuthToken(BEARER.matcher(headerValue).replaceFirst(""));
    }

    public static AuthToken of(String token) {
        return new AuthToken(token);
    }
}
