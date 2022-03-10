package com.markelliot.barista.oauth2;

import com.markelliot.barista.Bundle;
import com.markelliot.barista.endpoints.Endpoints;
import com.markelliot.barista.handlers.DelegatingHandler;
import java.util.Optional;

public final class OAuth2Bundle implements Bundle {
    private final String contextPath;
    private final OAuth2Client client;
    private final OAuth2Configuration config;

    public OAuth2Bundle(String contextPath, OAuth2Client client, OAuth2Configuration config) {
        this.contextPath = contextPath;
        this.client = client;
        this.config = config;
    }

    @Override
    public String name() {
        return "OAuth2";
    }

    @Override
    public Optional<DelegatingHandler> handler() {
        return Optional.of(AuthDelegatingHandler.of(contextPath, client, () -> config));
    }

    @Override
    public Endpoints endpoints() {
        return null;
    }
}
