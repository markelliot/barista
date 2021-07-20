package barista.authz;

public record VerifiedAuthToken(AuthToken token, String userId) {}
