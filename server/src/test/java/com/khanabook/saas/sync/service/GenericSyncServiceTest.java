package com.khanabook.saas.sync.service;

import com.khanabook.saas.entity.Bill;
import com.khanabook.saas.entity.RestaurantProfile;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.repository.SyncRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GenericSyncServiceTest {

    @Mock private SyncRepository<Bill, Long> billRepo;
    @Mock private SyncRepository<RestaurantProfile, Long> profileRepo;

    @Captor private ArgumentCaptor<Iterable<Bill>> billSaveCaptor;
    @Captor private ArgumentCaptor<Iterable<RestaurantProfile>> profileSaveCaptor;

    private GenericSyncService service;

    private static final Long TENANT_ID = 42L;
    private static final String DEVICE_A = "DEVICE_A";

    @BeforeEach
    void setUp() {
        service = new GenericSyncService();
    }

    // ─── Null / empty guards ──────────────────────────────────────────────────

    @Test
    void nullTenantId_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.handlePushSync(null, List.of(bill(1, 1000L)), billRepo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant ID");
    }

    @Test
    void emptyPayload_returnsEmptyLists() {
        PushSyncResponse resp = service.handlePushSync(TENANT_ID, List.of(), billRepo);
        assertThat(resp.getSuccessfulLocalIds()).isEmpty();
        assertThat(resp.getFailedLocalIds()).isEmpty();
        verifyNoInteractions(billRepo);
    }

    // ─── New record insert ────────────────────────────────────────────────────

    @Test
    void newRecord_insertsAndAcknowledges() {
        Bill incoming = bill(1, 1000L);
        stubNoExisting();
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        PushSyncResponse resp = service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);

        assertThat(resp.getSuccessfulLocalIds()).containsExactly(1);
        assertThat(resp.getFailedLocalIds()).isEmpty();
        verify(billRepo).saveAll(billSaveCaptor.capture());
        Bill saved = billSaveCaptor.getValue().iterator().next();
        // Server must override tenantId
        assertThat(saved.getRestaurantId()).isEqualTo(TENANT_ID);
    }

    // ─── LWW — mobile newer wins ──────────────────────────────────────────────

    @Test
    void lww_mobileNewer_updatesRecord() {
        Bill existing = existingBill(5L, 1, 1000L);
        Bill incoming = bill(1, 2000L);
        stubExisting(List.of(existing));
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        PushSyncResponse resp = service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);

        assertThat(resp.getSuccessfulLocalIds()).containsExactly(1);
        verify(billRepo).saveAll(billSaveCaptor.capture());
        Bill saved = billSaveCaptor.getValue().iterator().next();
        assertThat(saved.getId()).isEqualTo(5L);       // must reuse existing server ID
        assertThat(saved.getUpdatedAt()).isEqualTo(2000L);
    }

    // ─── LWW — server newer, client still acknowledged ────────────────────────

    @Test
    void lww_serverNewer_clientAcknowledgedWithoutSave() {
        Bill existing = existingBill(5L, 1, 9000L);
        Bill incoming = bill(1, 1000L);  // older than server
        stubExisting(List.of(existing));

        PushSyncResponse resp = service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);

        // Client gets acknowledgment to stop retrying, but record is NOT saved
        assertThat(resp.getSuccessfulLocalIds()).containsExactly(1);
        verify(billRepo, never()).saveAll(any());
    }

    // ─── Tenant isolation ─────────────────────────────────────────────────────

    @Test
    void tenantIsolation_payloadRestaurantIdOverriddenByServer() {
        Bill incoming = bill(1, 1000L);
        incoming.setRestaurantId(666L); // attacker tries to set different tenant
        stubNoExisting();
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);

        verify(billRepo).saveAll(billSaveCaptor.capture());
        Bill saved = billSaveCaptor.getValue().iterator().next();
        assertThat(saved.getRestaurantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getRestaurantId()).isNotEqualTo(666L);
    }

    // ─── createdAt defaulting ─────────────────────────────────────────────────

    @Test
    void createdAtNull_defaultsToUpdatedAt() {
        Bill incoming = bill(1, 5000L);
        incoming.setCreatedAt(null);
        stubNoExisting();
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);

        verify(billRepo).saveAll(billSaveCaptor.capture());
        Bill saved = billSaveCaptor.getValue().iterator().next();
        assertThat(saved.getCreatedAt()).isEqualTo(5000L);
    }

    @Test
    void createdAtPresent_notOverridden() {
        Bill incoming = bill(1, 5000L);
        incoming.setCreatedAt(1000L); // original client creation time
        stubNoExisting();
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);

        verify(billRepo).saveAll(billSaveCaptor.capture());
        Bill saved = billSaveCaptor.getValue().iterator().next();
        assertThat(saved.getCreatedAt()).isEqualTo(1000L); // must be preserved
    }

    // ─── serverUpdatedAt is always server time ────────────────────────────────

    @Test
    void serverUpdatedAt_alwaysSetByServer_notClient() {
        Bill incoming = bill(1, 5000L);
        incoming.setServerUpdatedAt(9_999_999_999L); // client tries to forge future timestamp
        stubNoExisting();
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        long before = System.currentTimeMillis();
        service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);
        long after = System.currentTimeMillis();

        verify(billRepo).saveAll(billSaveCaptor.capture());
        Bill saved = billSaveCaptor.getValue().iterator().next();
        assertThat(saved.getServerUpdatedAt()).isBetween(before, after);
    }

    // ─── Batch deduplication ─────────────────────────────────────────────────

    @Test
    void batchWithDuplicateLocalIds_onlyLatestSaved() {
        Bill older = bill(1, 1000L);
        Bill newer = bill(1, 3000L);
        stubNoExisting();
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        PushSyncResponse resp = service.handlePushSync(TENANT_ID, List.of(older, newer), billRepo);

        verify(billRepo).saveAll(billSaveCaptor.capture());
        List<Bill> saved = (List<Bill>) billSaveCaptor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUpdatedAt()).isEqualTo(3000L);
        // Both local IDs acknowledged
        assertThat(resp.getSuccessfulLocalIds()).containsExactlyInAnyOrder(1, 1);
    }

    // ─── Singleton-style (RestaurantProfile) cross-device fallback ───────────

    @Test
    void singletonProfile_reinstall_matchesExistingTenantRecord() {
        RestaurantProfile existing = existingProfile(99L, 1, "DEVICE_OLD", 5000L);
        RestaurantProfile incoming = profileWithDevice(1, "DEVICE_NEW", 6000L);

        when(profileRepo.findByRestaurantIdAndDeviceIdAndLocalIdIn(eq(TENANT_ID), eq("DEVICE_NEW"), anyList()))
            .thenReturn(List.of()); // no match for new device
        when(profileRepo.findByRestaurantIdAndLocalIdIn(eq(TENANT_ID), anyList()))
            .thenReturn(List.of(existing)); // but found via tenant+localId
        doAnswer(i -> i.getArgument(0)).when(profileRepo).saveAll(any());

        PushSyncResponse resp = service.handlePushSync(TENANT_ID, List.of(incoming), profileRepo);

        assertThat(resp.getSuccessfulLocalIds()).containsExactly(1);
        verify(profileRepo).saveAll(profileSaveCaptor.capture());
        RestaurantProfile saved = profileSaveCaptor.getValue().iterator().next();
        // Must reuse the old server ID so the profile is updated, not duplicated
        assertThat(saved.getId()).isEqualTo(99L);
    }

    // ─── Non-singleton (Bill) does NOT get cross-device fallback ─────────────

    @Test
    void billWithLocalId1_doesNotTriggerCrossDeviceFallback() {
        Bill incoming = bill(1, 5000L); // Bill with localId=1, which is common

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalIdIn(eq(TENANT_ID), eq(DEVICE_A), anyList()))
            .thenReturn(List.of());
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);

        // Cross-device fallback must NOT be called for Bill
        verify(billRepo, never()).findByRestaurantIdAndLocalIdIn(anyLong(), anyList());
    }

    // ─── localId recovery from serverId ──────────────────────────────────────

    @Test
    void missingLocalId_recoveredFromServerId() {
        Bill incoming = new Bill();
        incoming.setId(77L);       // client sent serverId but no localId
        incoming.setLocalId(null);
        incoming.setDeviceId(DEVICE_A);
        incoming.setUpdatedAt(1000L);

        stubNoExisting();
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        PushSyncResponse resp = service.handlePushSync(TENANT_ID, List.of(incoming), billRepo);

        // After recovery, localId=77 and id cleared for fresh insert
        assertThat(resp.getSuccessfulLocalIds()).containsExactly(77);
        verify(billRepo).saveAll(billSaveCaptor.capture());
        Bill saved = billSaveCaptor.getValue().iterator().next();
        assertThat(saved.getLocalId()).isEqualTo(77);
        assertThat(saved.getId()).isNull(); // server ID cleared — fresh insert
    }

    // ─── Batch saves all at once (single saveAll call) ────────────────────────

    @Test
    void multipleDifferentRecords_savedInSingleBatch() {
        Bill b1 = bill(1, 1000L);
        Bill b2 = bill(2, 2000L);
        Bill b3 = bill(3, 3000L);
        stubNoExisting();
        doAnswer(i -> i.getArgument(0)).when(billRepo).saveAll(any());

        service.handlePushSync(TENANT_ID, List.of(b1, b2, b3), billRepo);

        // saveAll called exactly once — all 3 in one batch
        verify(billRepo, times(1)).saveAll(any());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Bill bill(int localId, long updatedAt) {
        Bill b = new Bill();
        b.setLocalId(localId);
        b.setUpdatedAt(updatedAt);
        b.setDeviceId(DEVICE_A);
        b.setRestaurantId(TENANT_ID);
        return b;
    }

    private Bill existingBill(Long serverId, int localId, long updatedAt) {
        Bill b = bill(localId, updatedAt);
        b.setId(serverId);
        return b;
    }

    private RestaurantProfile existingProfile(Long serverId, int localId, String deviceId, long updatedAt) {
        RestaurantProfile p = new RestaurantProfile();
        p.setId(serverId);
        p.setLocalId(localId);
        p.setDeviceId(deviceId);
        p.setRestaurantId(TENANT_ID);
        p.setUpdatedAt(updatedAt);
        return p;
    }

    private RestaurantProfile profileWithDevice(int localId, String deviceId, long updatedAt) {
        RestaurantProfile p = new RestaurantProfile();
        p.setLocalId(localId);
        p.setDeviceId(deviceId);
        p.setRestaurantId(TENANT_ID);
        p.setUpdatedAt(updatedAt);
        return p;
    }

    private void stubNoExisting() {
        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(new ArrayList<>());
    }

    private void stubExisting(List<Bill> records) {
        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(records);
    }
}
