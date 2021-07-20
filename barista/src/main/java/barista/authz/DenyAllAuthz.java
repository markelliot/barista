package barista.authz;

import java.util.Optional;

final class DenyAllAuthz implements Authz {

    @Override
    public AuthToken newSession(String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<VerifiedAuthToken> check(AuthToken token) {
        return Optional.empty();
    }
}
