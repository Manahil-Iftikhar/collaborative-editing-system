package com.collab.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RemoteIdentityTest {
    @Test
    void forwardsBearerAndUsesValidatedAccount() {
        RestTemplate http = new RestTemplate();
        var server = MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("http://identity/api/users/me"))
            .andExpect(header("Authorization", "Bearer example"))
            .andRespond(withSuccess("{\"id\":7,\"active\":true}", MediaType.APPLICATION_JSON));
        assertEquals(7L, new RemoteIdentity(http,"http://identity").requireUser("Bearer example"));
        server.verify();
    }

    @Test
    void missingTokenMakesNoRequest() {
        var http = new RestTemplate();
        var server = MockRestServiceServer.bindTo(http).build();
        var identity = new RemoteIdentity(http,"http://identity");
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> identity.requireUser(null)).getStatusCode().value());
        server.verify();
    }

    @Test
    void invalidTokenAndServiceFailureAreDenied() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.UNAUTHORIZED,HttpStatus.SERVICE_UNAVAILABLE}) {
            RestTemplate http = new RestTemplate();
            var server = MockRestServiceServer.bindTo(http).build();
            server.expect(requestTo("http://identity/api/users/me")).andRespond(withStatus(status));
            var error = assertThrows(ResponseStatusException.class,
                () -> new RemoteIdentity(http,"http://identity").requireUser("Bearer example"));
            assertEquals(status.value(), error.getStatusCode().value());
            server.verify();
        }
    }

    @Test
    void malformedOrInactiveIdentityFailsClosed() {
        for (String body : new String[]{"{}", "{\"id\":7,\"active\":false}", "{\"id\":\"bad\",\"active\":true}"}) {
            RestTemplate http = new RestTemplate();
            var server = MockRestServiceServer.bindTo(http).build();
            server.expect(requestTo("http://identity/api/users/me")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> new RemoteIdentity(http,"http://identity").requireUser("Bearer example")).getStatusCode().value());
            server.verify();
        }
    }
}
