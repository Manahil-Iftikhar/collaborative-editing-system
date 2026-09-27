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
public class DocumentOwnerAccess {
    private final RestTemplate http;
    private final String users;
    private final String documents;

    @Autowired
    public DocumentOwnerAccess(RestTemplateBuilder builder,
            @Value("${USER_SERVICE_URL:http://localhost:8081}") String users,
            @Value("${DOCUMENT_SERVICE_URL:http://localhost:8082}") String documents) {
        this(builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(3)).build(), users, documents);
    }

    DocumentOwnerAccess(RestTemplate http, String users, String documents) {
        this.http = http; this.users = users; this.documents = documents;
    }

    public Long requireOwner(Long documentId, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token required");
        if (documentId == null || documentId <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid documentId required");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        Map<?, ?> account = get(users + "/api/users/me", headers);
        if (!(account.get("id") instanceof Number) || !Boolean.TRUE.equals(account.get("active")))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity response unavailable");
        long userId = ((Number) account.get("id")).longValue();
        Map<?, ?> document = get(documents + "/api/documents/" + documentId, headers);
        if (!(document.get("ownerId") instanceof Number) || !(document.get("id") instanceof Number)
                || ((Number) document.get("id")).longValue() != documentId)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Document response unavailable");
        if (((Number) document.get("ownerId")).longValue() != userId)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Document owner required");
        return userId;
    }

    private Map<?, ?> get(String url, HttpHeaders headers) {
        try {
            Map<?, ?> body = http.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
            if (body == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Upstream response unavailable");
            return body;
        } catch (HttpClientErrorException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401 || status == 403 || status == 404)
                throw new ResponseStatusException(HttpStatus.valueOf(status), "Access unavailable");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authorization service unavailable");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authorization service unavailable");
        }
    }
}
