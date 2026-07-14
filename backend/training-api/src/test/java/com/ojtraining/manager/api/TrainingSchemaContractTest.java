package com.ojtraining.manager.api;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingSchemaContractTest {
    @Test
    void finalSchemaOwnsThirteenTablesAndUsesIdentityAppropriateCollations() throws Exception {
        String schema = new ClassPathResource(
                "db/migration/V001__create_training_data_schema.sql"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(schema.split("CREATE TABLE ", -1)).hasSize(14);
        assertThat(schema).doesNotContain("REFERENCES `user`");
        assertThat(schema).contains(
                "username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL",
                "handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL",
                "author_handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL",
                "user_id varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL",
                "CREATE TABLE oj_data_consistency_fence",
                "VALUES ('ATCODER', 0), ('CODEFORCES', 0)"
        );
        assertThat(count(schema, "username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL"))
                .isEqualTo(2);
        assertThat(count(schema, "handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"))
                .isEqualTo(8);
    }

    private static int count(String text, String fragment) {
        return (text.length() - text.replace(fragment, "").length()) / fragment.length();
    }
}
