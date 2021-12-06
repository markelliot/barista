/*
 * (c) Copyright 2021 Mark Elliot. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markelliot.barista.authz;

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
