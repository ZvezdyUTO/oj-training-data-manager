package com.ojtraining.manager.api.member;

import com.ojtraining.manager.trainingdata.common.app.account.OjHandleAccountException;
import com.ojtraining.manager.trainingdata.common.app.account.OjHandleAccountService;
import com.ojtraining.manager.trainingdata.common.app.purge.OjStudentDataPurgeService;
import com.ojtraining.manager.trainingdata.common.collector.lock.OjDataConsistencyFence;
import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjStudentDataPurgeResult;
import com.ojtraining.manager.api.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class MemberManagementService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[\\p{L}\\p{N}._-]{1,128}");

    private final OjHandleAccountService accountService;
    private final OjStudentDataPurgeService purgeService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OjDataConsistencyFence consistencyFence;
    private final Clock clock;

    @Autowired
    public MemberManagementService(
            OjHandleAccountService accountService,
            OjStudentDataPurgeService purgeService,
            NamedParameterJdbcTemplate jdbcTemplate,
            OjDataConsistencyFence consistencyFence
    ) {
        this(accountService, purgeService, jdbcTemplate, consistencyFence, Clock.systemUTC());
    }

    MemberManagementService(
            OjHandleAccountService accountService,
            OjStudentDataPurgeService purgeService,
            NamedParameterJdbcTemplate jdbcTemplate,
            OjDataConsistencyFence consistencyFence,
            Clock clock
    ) {
        this.accountService = accountService;
        this.purgeService = purgeService;
        this.jdbcTemplate = jdbcTemplate;
        this.consistencyFence = consistencyFence;
        this.clock = clock;
    }

    @Transactional
    public List<OjHandleAccount> batchCreate(BatchMemberCreateRequest request) {
        return consistencyFence.mutateBindings(ignored -> {
            if (request == null || request.members() == null || request.members().isEmpty()) {
                throw badRequest("成员列表不能为空");
            }
            List<OjHandleAccount> created = new ArrayList<>(request.members().size());
            for (MemberCreateRequest member : request.members()) {
                if (member == null) {
                    throw badRequest("成员不能为空");
                }
                String username = normalizeUsername(member.username());
                Map<String, String> handles = normalizeHandles(member.handles());
                boolean needCollect = member.needCollect() == null || member.needCollect();
                created.add(accountService.create(username, handles, needCollect));
            }
            return List.copyOf(created);
        });
    }

    @Transactional
    public OjHandleAccount update(String pathUsername, MemberUpdateRequest request) {
        return consistencyFence.mutateBindings(ignored -> {
            if (request == null) {
                throw badRequest("请求体不能为空");
            }
            String oldUsername = normalizeUsername(pathUsername);
            lockMember(oldUsername);
            OjHandleAccount existing = accountService.getByUsername(oldUsername);
            String newUsername = request.username() == null
                    ? oldUsername
                    : normalizeUsername(request.username());
            if (!oldUsername.equals(newUsername)) {
                requireUsernameAvailable(newUsername);
            }

            Map<String, String> requestedHandles = request.handles() == null
                    ? existing.handles()
                    : normalizeHandles(request.handles());
            boolean needCollect = request.needCollect() == null
                    ? existing.needCollect()
                    : request.needCollect();
            validateHandleOwnership(oldUsername, requestedHandles);

            List<String> changedOjNames = existing.handles().entrySet().stream()
                    .filter(entry -> !entry.getValue().equals(requestedHandles.get(entry.getKey())))
                    .map(Map.Entry::getKey)
                    .toList();
            for (String ojName : changedOjNames) {
                purgeService.purgeStudentData(oldUsername, ojName);
            }

            if (!oldUsername.equals(newUsername)) {
                renameMember(oldUsername, newUsername);
            }
            return accountService.replaceHandlesAfterPurge(newUsername, requestedHandles, needCollect);
        });
    }

    @Transactional
    public MemberDeleteResult delete(String username) {
        return consistencyFence.mutateBindings(ignored -> {
            String normalizedUsername = normalizeUsername(username);
            lockMember(normalizedUsername);
            OjHandleAccount existing = accountService.getByUsername(normalizedUsername);
            List<OjStudentDataPurgeResult> purgeResults = existing.handles().keySet().stream()
                    .map(ojName -> purgeService.purgeStudentData(normalizedUsername, ojName))
                    .toList();
            int deleted = jdbcTemplate.update(
                    "delete from training_member where username = :username",
                    new MapSqlParameterSource("username", normalizedUsername)
            );
            if (deleted != 1) {
                throw new IllegalStateException("expected to delete one training member, deleted=" + deleted);
            }
            return new MemberDeleteResult(normalizedUsername, purgeResults, deleted);
        });
    }

    private void lockMember(String username) {
        List<String> rows = jdbcTemplate.queryForList(
                "select username from training_member where username = :username for update",
                new MapSqlParameterSource("username", username),
                String.class
        );
        if (rows.isEmpty()) {
            throw new OjHandleAccountException(
                    OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND,
                    "training member not found"
            );
        }
    }

    private void requireUsernameAvailable(String username) {
        try {
            accountService.getByUsername(username);
        } catch (OjHandleAccountException exception) {
            if (exception.errorCode() == OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND) {
                return;
            }
            throw exception;
        }
        throw new OjHandleAccountException(
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS,
                "username already exists"
        );
    }

    private void validateHandleOwnership(String currentUsername, Map<String, String> handles) {
        for (Map.Entry<String, String> handle : handles.entrySet()) {
            try {
                OjHandleAccount owner = accountService.getByHandle(handle.getKey(), handle.getValue());
                if (!currentUsername.equals(owner.username())) {
                    throw new OjHandleAccountException(
                            OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS,
                            handle.getKey() + " handle already belongs to another username"
                    );
                }
            } catch (OjHandleAccountException exception) {
                if (exception.errorCode() != OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND) {
                    throw exception;
                }
            }
        }
    }

    private void renameMember(String oldUsername, String newUsername) {
        Instant updatedAt = clock.instant();
        int updated = jdbcTemplate.update("""
                        update training_member
                        set username = :newUsername,
                            updated_at = :updatedAt
                        where username = :oldUsername
                        """,
                new MapSqlParameterSource()
                        .addValue("oldUsername", oldUsername)
                        .addValue("newUsername", newUsername)
                        .addValue("updatedAt", Timestamp.from(updatedAt)));
        if (updated != 1) {
            throw new IllegalStateException("expected to rename one training member, updated=" + updated);
        }
    }

    private static String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw badRequest("username 需为 1 到 128 个中文、字母、数字、点、下划线或连字符");
        }
        return normalized;
    }

    private static Map<String, String> normalizeHandles(Map<String, String> handles) {
        try {
            return Map.copyOf(new LinkedHashMap<>(OjHandleAccount.normalizeHandles(handles)));
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEMBER_REQUEST", message);
    }
}
