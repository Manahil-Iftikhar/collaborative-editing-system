package com.collab;

import com.collab.service.VersionService;
import com.collab.security.DocumentOwnerAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VersionConflictHttpTest {
    @Autowired MockMvc mvc;
    @MockBean VersionService service;
    @MockBean DocumentOwnerAccess access;

    @Test void createAndRevertReturnSanitizedConflict() throws Exception {
        when(access.requireOwner(1L, "Bearer owner")).thenReturn(7L);
        var failure = new DataIntegrityViolationException("private SQL constraint detail");
        when(service.createVersion(any())).thenThrow(failure);
        when(service.revertToVersion(1L, 1, 7L)).thenThrow(failure);
        var requests = new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{
            post("/api/versions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentId\":1,\"content\":\"new\"}"),
            post("/api/versions/revert?documentId=1&versionNumber=1")
        };
        for (var request : requests) {
            mvc.perform(request.header("Authorization", "Bearer owner"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                    "Snapshot write conflict; reload history and retry."));
        }
    }
}
