package barista.authz;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

final class DefaultAuthz implements Authz {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final Clock clock;

    public DefaultAuthz(String secret, String issuer, Clock clock) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.clock = clock;
        this.verifier = JWT.require(algorithm).build();
    }

    @Override
    public AuthToken newSession(String userId) {
        return AuthTokens.of(
                JWT.create()
                        .withIssuer(issuer)
                        .withSubject(userId)
                        .withExpiresAt(Date.from(clock.instant().plus(1, ChronoUnit.DAYS)))
                        .sign(algorithm));
    }

    @Override
    public Optional<VerifiedAuthToken> check(AuthToken token) {
        try {
            return Optional.of(verifier.verify(token.token()))
                    .map(t -> new VerifiedAuthToken(token, t.getSubject()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
