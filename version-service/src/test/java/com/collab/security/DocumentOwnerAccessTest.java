package com.collab.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DocumentOwnerAccessTest {
    void account(MockRestServiceServer server) {
        server.expect(requestTo("http://users/api/users/me")).andExpect(header("Authorization","Bearer example"))
            .andRespond(withSuccess("{\"id\":7,\"active\":true}",MediaType.APPLICATION_JSON));
    }
    @Test void ownerVerifiedAcrossBothServices() {
        RestTemplate http=new RestTemplate(); var server=MockRestServiceServer.bindTo(http).build();
        account(server);
        server.expect(requestTo("http://docs/api/documents/1")).andExpect(header("Authorization","Bearer example"))
            .andRespond(withSuccess("{\"id\":1,\"ownerId\":7}",MediaType.APPLICATION_JSON));
        assertEquals(7L,new DocumentOwnerAccess(http,"http://users","http://docs").requireOwner(1L,"Bearer example"));
        server.verify();
    }
    @Test void publicDocumentDoesNotGrantSnapshotAccessToAnotherUser() {
        RestTemplate http=new RestTemplate(); var server=MockRestServiceServer.bindTo(http).build(); account(server);
        server.expect(requestTo("http://docs/api/documents/1"))
            .andRespond(withSuccess("{\"id\":1,\"ownerId\":8,\"public\":true}",MediaType.APPLICATION_JSON));
        assertEquals(403,assertThrows(ResponseStatusException.class,
            ()->new DocumentOwnerAccess(http,"http://users","http://docs").requireOwner(1L,"Bearer example")).getStatusCode().value());
        server.verify();
    }
    @Test void missingIdentityAndInvalidIdsMakeNoRequests() {
        RestTemplate http=new RestTemplate(); var server=MockRestServiceServer.bindTo(http).build();
        var access=new DocumentOwnerAccess(http,"http://users","http://docs");
        assertEquals(401,assertThrows(ResponseStatusException.class,()->access.requireOwner(1L,null)).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->access.requireOwner(null,"Bearer example")).getStatusCode().value());
        server.verify();
    }
    @Test void upstreamErrorsAndMalformedDataNeverGrantAccess() {
        for(HttpStatus status:new HttpStatus[]{HttpStatus.UNAUTHORIZED,HttpStatus.FORBIDDEN,HttpStatus.NOT_FOUND,HttpStatus.SERVICE_UNAVAILABLE}) {
            RestTemplate http=new RestTemplate(); var server=MockRestServiceServer.bindTo(http).build(); account(server);
            server.expect(requestTo("http://docs/api/documents/1")).andRespond(withStatus(status));
            assertEquals(status.value(),assertThrows(ResponseStatusException.class,
                ()->new DocumentOwnerAccess(http,"http://users","http://docs").requireOwner(1L,"Bearer example")).getStatusCode().value());
            server.verify();
        }
        for(String body:new String[]{"{}","{\"id\":2,\"ownerId\":7}"}) {
            RestTemplate http=new RestTemplate(); var server=MockRestServiceServer.bindTo(http).build(); account(server);
            server.expect(requestTo("http://docs/api/documents/1")).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
            assertEquals(503,assertThrows(ResponseStatusException.class,
                ()->new DocumentOwnerAccess(http,"http://users","http://docs").requireOwner(1L,"Bearer example")).getStatusCode().value());
            server.verify();
        }
    }
}
