package com.ojtraining.manager.api.member;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

public record MemberUpdateRequest(
        @JsonAlias("newUsername") String username,
        Boolean needCollect,
        Map<String, String> handles
) {
}
