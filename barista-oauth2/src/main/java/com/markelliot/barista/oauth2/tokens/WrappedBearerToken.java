package com.markelliot.barista.oauth2.tokens;

import com.markelliot.barista.authz.AuthToken;
import com.palantir.tokens.auth.BearerToken;

public record WrappedBearerToken(BearerToken delegate) implements AuthToken {
    @Override
    public String token() {
        return delegate().getToken();
    }

    public static AuthToken of(BearerToken delegate) {
        return new WrappedBearerToken(delegate);
    }

    public static AuthToken of(String token) {
        return new WrappedBearerToken(BearerToken.valueOf(token));
    }

    public static AuthToken fromAuthorizationHeader(String headerValue) {
        return new WrappedBearerToken(
                BearerToken.valueOf(headerValue.startsWith("Bearer ") ? headerValue.substring(7) : headerValue));
    }
}
