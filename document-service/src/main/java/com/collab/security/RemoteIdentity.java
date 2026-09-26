package com.collab.security;

import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RemoteIdentity {
    private final RestTemplate http;
    private final String base;

    @Autowired
    public RemoteIdentity(RestTemplateBuilder builder,
            @Value("${USER_SERVICE_URL:http://localhost:8081}") String base) {
        this(builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(3)).build(), base);
    }

    RemoteIdentity(RestTemplate http, String base) {
        this.http = http;
        this.base = base;
    }

    public Long requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token required");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        try {
            Map<?, ?> body = http.exchange(base + "/api/users/me", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class).getBody();
            if (body == null || !(body.get("id") instanceof Number)
                    || !Boolean.TRUE.equals(body.get("active"))) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity response unavailable");
            }
            return ((Number) body.get("id")).longValue();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service unavailable");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service unavailable");
        }
    }
}
