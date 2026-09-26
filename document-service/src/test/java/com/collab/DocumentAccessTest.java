package com.collab;

import com.collab.dto.DocumentCreateRequest;
import com.collab.service.DocumentService;
import com.collab.security.RemoteIdentity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentAccessTest {
    @Autowired MockMvc mvc;
    @Autowired DocumentService documents;
    @MockBean RemoteIdentity identity;
    Long privateId;
    Long publicId;

    @BeforeEach
    void setup() {
        when(identity.requireUser(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        when(identity.requireUser("Bearer owner")).thenReturn(1L);
        when(identity.requireUser("Bearer other")).thenReturn(2L);
        privateId = create(false);
        publicId = create(true);
    }
    Long create(boolean visible) {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setTitle("Sample"); request.setContent("Original");
        request.setOwnerId(1L); request.setPublic(visible);
        return documents.createDocument(request).getId();
    }

    @Test
    void anonymousPrivateReadsAndWritesAreDenied() throws Exception {
        mvc.perform(get("/api/documents/"+privateId)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/documents/"+privateId).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"changed\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/documents/"+privateId+"/changes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/documents").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"test\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void otherAccountCannotReadPrivateOrEditAnyOwnerDocument() throws Exception {
        mvc.perform(get("/api/documents/"+privateId).header("Authorization","Bearer other")).andExpect(status().isForbidden());
        for (Long id : new Long[]{privateId,publicId}) {
            mvc.perform(put("/api/documents/"+id).header("Authorization","Bearer other")
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"changed\",\"userId\":1}"))
                .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/documents/"+privateId).header("Authorization","Bearer owner"))
            .andExpect(jsonPath("$.content").value("Original"));
    }

    @Test
    void createAndEditDeriveIdentityFromToken() throws Exception {
        mvc.perform(post("/api/documents").header("Authorization","Bearer owner")
            .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Mine\",\"content\":\"x\",\"ownerId\":999}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(1));
        mvc.perform(put("/api/documents/"+privateId).header("Authorization","Bearer owner")
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Updated\",\"userId\":999}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lastEditedBy").value(1));
    }

    @Test
    void ownerListsAndChangeHistoryArePrivate() throws Exception {
        mvc.perform(get("/api/documents/owner/1").header("Authorization","Bearer other")).andExpect(status().isForbidden());
        mvc.perform(get("/api/documents/"+publicId+"/changes").header("Authorization","Bearer other")).andExpect(status().isForbidden());
        mvc.perform(get("/api/documents/owner/1").header("Authorization","Bearer owner")).andExpect(status().isOk());
        mvc.perform(get("/api/documents/"+privateId+"/changes").header("Authorization","Bearer owner")).andExpect(status().isOk());
    }

    @Test
    void publicContentRemainsReadableWithoutIdentityService() throws Exception {
        mvc.perform(get("/api/documents/"+publicId)).andExpect(status().isOk());
        mvc.perform(get("/api/documents/public")).andExpect(status().isOk());
        verifyNoInteractions(identity);
    }
}
