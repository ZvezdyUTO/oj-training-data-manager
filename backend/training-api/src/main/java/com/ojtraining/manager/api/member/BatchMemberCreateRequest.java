package com.ojtraining.manager.api.member;

import java.util.List;

public record BatchMemberCreateRequest(List<MemberCreateRequest> members) {
}
