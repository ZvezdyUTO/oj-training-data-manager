package com.ojtraining.manager.api.member;

import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.ojtraining.manager.api.web.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberManagementService memberManagementService;

    public MemberController(MemberManagementService memberManagementService) {
        this.memberManagementService = memberManagementService;
    }

    @PostMapping("/batch")
    public ApiResponse<List<OjHandleAccount>> batchCreate(
            @RequestBody BatchMemberCreateRequest request
    ) {
        return ApiResponse.ok("成员已创建", memberManagementService.batchCreate(request));
    }

    @PutMapping("/{username}")
    public ApiResponse<OjHandleAccount> update(
            @PathVariable String username,
            @RequestBody MemberUpdateRequest request
    ) {
        return ApiResponse.ok("成员已更新", memberManagementService.update(username, request));
    }

    @DeleteMapping("/{username}")
    public ApiResponse<MemberDeleteResult> delete(@PathVariable String username) {
        return ApiResponse.ok("成员及训练数据已删除", memberManagementService.delete(username));
    }
}
