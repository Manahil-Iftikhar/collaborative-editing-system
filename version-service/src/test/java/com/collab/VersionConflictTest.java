package com.collab;

import com.collab.dto.VersionCreateRequest;
import com.collab.repository.DocumentVersionRepository;
import com.collab.repository.UserContributionRepository;
import com.collab.service.VersionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class VersionConflictTest {
    @Autowired VersionService service;
    @SpyBean DocumentVersionRepository versions;
    @Autowired UserContributionRepository contributions;

    @BeforeEach void clean() {
        versions.deleteAll();
        contributions.deleteAll();
    }

    @Test void concurrentFirstSnapshotsKeepOneSnapshotAndOneContribution() throws Exception {
        collide(null, false);
    }

    @Test void concurrentCreateAndRevertKeepOneNewSnapshotAndContribution() throws Exception {
        service.createVersion(new VersionCreateRequest(1L, "original", 7L, "initial"));
        collide(1, true);
    }

    private void collide(Integer observedMax, boolean revert) throws Exception {
        // Force both independent service transactions to observe the same MAX.
        // Only this read is stubbed; writes, constraints and commits use real H2.
        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            barrier.await(10, TimeUnit.SECONDS);
            return observedMax;
        }).when(versions).findMaxVersionNumber(1L);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> create = () -> attempt(false);
            Future<Boolean> first = pool.submit(create);
            Future<Boolean> second = pool.submit(() -> attempt(revert));
            int successes = (first.get(20, TimeUnit.SECONDS) ? 1 : 0)
                + (second.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            int expected = observedMax == null ? 1 : 2;
            var history = service.getVersionHistory(1L);
            assertEquals(expected, history.size());
            assertEquals(expected, history.get(0).getVersionNumber());
            var counters = service.getUserContributions(1L);
            assertEquals(1, counters.size());
            assertEquals(expected, counters.get(0).getVersionsCreated());
            assertEquals(expected, counters.get(0).getEditsCount());
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private boolean attempt(boolean revert) {
        try {
            if (revert) service.revertToVersion(1L, 1, 7L);
            else service.createVersion(new VersionCreateRequest(1L, "new", 7L, "update"));
            return true;
        } catch (DataIntegrityViolationException expected) {
            return false;
        }
    }
}
