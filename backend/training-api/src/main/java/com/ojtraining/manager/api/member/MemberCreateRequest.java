package com.ojtraining.manager.api.member;

import java.util.Map;

public record MemberCreateRequest(
        String username,
        Boolean needCollect,
        Map<String, String> handles
) {
}
