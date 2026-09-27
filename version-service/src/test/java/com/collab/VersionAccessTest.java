package com.collab;

import com.collab.security.DocumentOwnerAccess;
import com.collab.service.VersionService;
import com.collab.dto.VersionCreateRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VersionAccessTest {
    @Autowired MockMvc mvc;
    @Autowired VersionService versions;
    @MockBean DocumentOwnerAccess access;
    @BeforeEach void setup() {
        when(access.requireOwner(1L,null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        when(access.requireOwner(1L,"Bearer other")).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        when(access.requireOwner(1L,"Bearer owner")).thenReturn(7L);
        versions.createVersion(new VersionCreateRequest(1L,"Private snapshot",7L,"Initial"));
    }
    MockHttpServletRequestBuilder[] requests() {
        return new MockHttpServletRequestBuilder[]{
            post("/api/versions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentId\":1,\"content\":\"New\",\"userId\":999}"),
            post("/api/versions/revert?documentId=1&versionNumber=1&userId=999"),
            get("/api/versions/history/1"),get("/api/versions/1/1"),get("/api/versions/contributions/1")
        };
    }
    @Test void allEndpointsRejectAnonymousRequests() throws Exception {
        for(var request:requests()) mvc.perform(request).andExpect(status().isUnauthorized());
    }
    @Test void allEndpointsRejectNonOwners() throws Exception {
        for(var request:requests()) mvc.perform(request.header("Authorization","Bearer other"))
            .andExpect(status().isForbidden());
    }
    @Test void ownerCanReadSnapshotsHistoryAndContributions() throws Exception {
        mvc.perform(get("/api/versions/1/1").header("Authorization","Bearer owner"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Private snapshot"));
        mvc.perform(get("/api/versions/history/1").header("Authorization","Bearer owner")).andExpect(status().isOk());
        mvc.perform(get("/api/versions/contributions/1").header("Authorization","Bearer owner")).andExpect(status().isOk());
    }
    @Test void writesUseAuthenticatedIdAndFailClosed() throws Exception {
        mvc.perform(requests()[0].header("Authorization","Bearer owner")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdBy").value(7));
        mvc.perform(requests()[1].header("Authorization","Bearer owner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.createdBy").value(7));
        when(access.requireOwner(1L,"Bearer owner")).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        mvc.perform(get("/api/versions/history/1").header("Authorization","Bearer owner")).andExpect(status().isServiceUnavailable());
    }
}
