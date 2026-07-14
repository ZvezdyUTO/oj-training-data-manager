package com.ojtraining.manager.api.member;

import com.ojtraining.manager.trainingdata.common.app.account.OjHandleAccountException;
import com.ojtraining.manager.trainingdata.common.app.account.OjHandleAccountService;
import com.ojtraining.manager.trainingdata.common.app.purge.OjStudentDataPurgeService;
import com.ojtraining.manager.trainingdata.common.collector.lock.OjDataConsistencyFence;
import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjStudentDataPurgeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    private final OjHandleAccountService accountService = mock(OjHandleAccountService.class);
    private final OjStudentDataPurgeService purgeService = mock(OjStudentDataPurgeService.class);
    private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private MemberManagementService service;

    @BeforeEach
    void setUp() {
        service = new MemberManagementService(
                accountService,
                purgeService,
                jdbcTemplate,
                OjDataConsistencyFence.passthrough(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void batchCreateUsesOnlyTrainingMemberFieldsAndDefaultsCollectionOn() {
        OjHandleAccount created = account("alice", Map.of("CODEFORCES", "alice-cf"), true);
        when(accountService.create("alice", Map.of("CODEFORCES", "alice-cf"), true)).thenReturn(created);

        List<OjHandleAccount> result = service.batchCreate(new BatchMemberCreateRequest(List.of(
                new MemberCreateRequest(" alice ", null, Map.of("codeforces", "alice-cf"))
        )));

        assertEquals(List.of(created), result);
    }

    @Test
    void handleChangePurgesOldDataBeforeSafeRenameAndCursorResetReplacement() {
        OjHandleAccount existing = account(
                "alice",
                Map.of("CODEFORCES", "old-cf", "ATCODER", "same-at"),
                true
        );
        OjHandleAccount updated = account(
                "alice-new",
                Map.of("CODEFORCES", "new-cf", "ATCODER", "same-at"),
                false
        );
        when(jdbcTemplate.queryForList(any(String.class), any(MapSqlParameterSource.class), eq(String.class)))
                .thenReturn(List.of("alice"));
        when(accountService.getByUsername("alice")).thenReturn(existing);
        when(accountService.getByUsername("alice-new")).thenThrow(notFound());
        when(accountService.getByHandle("CODEFORCES", "new-cf")).thenThrow(notFound());
        when(accountService.getByHandle("ATCODER", "same-at")).thenReturn(existing);
        when(purgeService.purgeStudentData("alice", "CODEFORCES"))
                .thenReturn(purgeResult("alice", "CODEFORCES", "old-cf"));
        when(jdbcTemplate.update(any(String.class), any(MapSqlParameterSource.class))).thenReturn(1);
        when(accountService.replaceHandlesAfterPurge(
                "alice-new",
                Map.of("CODEFORCES", "new-cf", "ATCODER", "same-at"),
                false
        )).thenReturn(updated);

        OjHandleAccount result = service.update("alice", new MemberUpdateRequest(
                "alice-new",
                false,
                Map.of("CODEFORCES", "new-cf", "ATCODER", "same-at")
        ));

        assertEquals(updated, result);
        var ordered = inOrder(purgeService, jdbcTemplate, accountService);
        ordered.verify(purgeService).purgeStudentData("alice", "CODEFORCES");
        ordered.verify(jdbcTemplate).update(any(String.class), any(MapSqlParameterSource.class));
        ordered.verify(accountService).replaceHandlesAfterPurge(
                "alice-new",
                Map.of("CODEFORCES", "new-cf", "ATCODER", "same-at"),
                false
        );
        verify(purgeService, never()).purgeStudentData("alice", "ATCODER");
    }

    @Test
    void conflictingHandleIsRejectedBeforeAnyPurge() {
        OjHandleAccount existing = account("alice", Map.of("CODEFORCES", "old-cf"), true);
        OjHandleAccount other = account("bob", Map.of("CODEFORCES", "taken"), true);
        when(jdbcTemplate.queryForList(any(String.class), any(MapSqlParameterSource.class), eq(String.class)))
                .thenReturn(List.of("alice"));
        when(accountService.getByUsername("alice")).thenReturn(existing);
        when(accountService.getByHandle("CODEFORCES", "taken")).thenReturn(other);

        OjHandleAccountException exception = assertThrows(OjHandleAccountException.class, () ->
                service.update("alice", new MemberUpdateRequest(
                        null,
                        true,
                        Map.of("CODEFORCES", "taken")
                ))
        );

        assertEquals(OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS, exception.errorCode());
        verify(purgeService, never()).purgeStudentData(any(), any());
    }

    @Test
    void deletePurgesEveryBoundOjThenDeletesMember() {
        OjHandleAccount existing = account(
                "alice",
                Map.of("CODEFORCES", "alice-cf", "ATCODER", "alice-at"),
                true
        );
        when(jdbcTemplate.queryForList(any(String.class), any(MapSqlParameterSource.class), eq(String.class)))
                .thenReturn(List.of("alice"));
        when(accountService.getByUsername("alice")).thenReturn(existing);
        when(purgeService.purgeStudentData(eq("alice"), any(String.class)))
                .thenAnswer(invocation -> purgeResult("alice", invocation.getArgument(1), "handle"));
        when(jdbcTemplate.update(any(String.class), any(MapSqlParameterSource.class))).thenReturn(1);

        MemberDeleteResult result = service.delete("alice");

        assertEquals(1, result.deletedMemberRows());
        assertEquals(2, result.purgeResults().size());
        verify(purgeService).purgeStudentData("alice", "CODEFORCES");
        verify(purgeService).purgeStudentData("alice", "ATCODER");
    }

    private static OjHandleAccount account(String username, Map<String, String> handles, boolean needCollect) {
        return new OjHandleAccount(username, handles, needCollect, NOW, NOW);
    }

    private static OjHandleAccountException notFound() {
        return new OjHandleAccountException(
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND,
                "not found"
        );
    }

    private static OjStudentDataPurgeResult purgeResult(String username, String ojName, String handle) {
        return OjStudentDataPurgeResult.aggregate(username, ojName, handle, Map.of(), List.of());
    }
}
