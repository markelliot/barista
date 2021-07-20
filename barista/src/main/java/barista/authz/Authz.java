package barista.authz;

import java.time.Clock;
import java.util.Optional;

public interface Authz {
    AuthToken newSession(String userId);

    Optional<VerifiedAuthToken> check(AuthToken token);

    static Authz createDefault(String secret, String issuer, Clock clock) {
        return new DefaultAuthz(secret, issuer, clock);
    }

    static Authz denyAll() {
        return new DenyAllAuthz();
    }
}
