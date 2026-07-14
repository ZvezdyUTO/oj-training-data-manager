-- Author: huangbingrui.awa

CREATE TABLE training_member (
    username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    need_collect boolean NOT NULL DEFAULT true,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    PRIMARY KEY (username)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE oj_handle_binding (
    username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    oj_name varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    last_collected_at datetime(6) NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    PRIMARY KEY (username, oj_name),
    UNIQUE KEY uk_oj_handle_binding_oj_name_handle (oj_name, handle),
    CONSTRAINT chk_oj_handle_binding_oj_name CHECK (oj_name IN ('CODEFORCES', 'ATCODER')),
    CONSTRAINT fk_oj_handle_binding_training_member
        FOREIGN KEY (username) REFERENCES training_member (username)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE oj_data_consistency_fence (
    oj_name varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    generation bigint unsigned NOT NULL DEFAULT 0,
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    PRIMARY KEY (oj_name),
    CONSTRAINT chk_oj_data_consistency_fence_oj_name
        CHECK (oj_name IN ('CODEFORCES', 'ATCODER'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO oj_data_consistency_fence (oj_name, generation)
VALUES ('ATCODER', 0), ('CODEFORCES', 0);

CREATE TABLE ods_codeforces__submission (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    codeforces_submission_id bigint NOT NULL,
    contest_id bigint NULL,
    creation_time_seconds bigint NULL,
    relative_time_seconds int NULL,
    problem_contest_id bigint NULL,
    problem_index varchar(32) NULL,
    problem_name varchar(255) NULL,
    problem_type varchar(64) NULL,
    problem_points decimal(12, 4) NULL,
    problem_rating int NULL,
    problem_tags_json longtext NULL,
    author_handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    author_participant_type varchar(64) NULL,
    author_json longtext NULL,
    programming_language varchar(128) NULL,
    verdict varchar(64) NULL,
    testset varchar(64) NULL,
    passed_test_count int NULL,
    time_consumed_millis int NULL,
    memory_consumed_bytes bigint NULL,
    batch_id varchar(128) NOT NULL,
    fetched_at datetime(6) NOT NULL,
    raw_payload longtext NOT NULL,
    payload_hash char(64) NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_ods_codeforces_submission_handle (codeforces_submission_id, author_handle),
    KEY idx_ods_codeforces_submission_author_time (author_handle, creation_time_seconds),
    KEY idx_ods_codeforces_submission_problem (contest_id, problem_index),
    KEY idx_ods_codeforces_submission_batch (batch_id)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE dwd_codeforces__submission (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    ods_submission_id bigint NOT NULL,
    submission_id varchar(128) NOT NULL,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    submitted_at_utc_plus8 datetime(6) NULL,
    submitted_date_utc_plus8 date NULL,
    problem_key varchar(128) NULL,
    problem_index varchar(32) NULL,
    problem_name varchar(255) NULL,
    difficulty varchar(64) NULL,
    language varchar(255) NULL,
    verdict varchar(128) NULL,
    is_accepted tinyint(1) NOT NULL,
    time_consumed_millis int NULL,
    source_url varchar(512) NULL,
    ods_batch_id varchar(128) NOT NULL,
    ods_fetched_at datetime(6) NOT NULL,
    ods_payload_hash char(64) NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_dwd_codeforces_submission_handle (submission_id, handle),
    KEY idx_dwd_codeforces_submission_ods (ods_submission_id),
    KEY idx_dwd_codeforces_handle_time (handle, submitted_at_utc_plus8),
    KEY idx_dwd_codeforces_problem_time (problem_key, submitted_at_utc_plus8, submission_id),
    KEY idx_dwd_codeforces_handle_problem (handle, problem_key),
    KEY idx_dwd_codeforces_batch (ods_batch_id)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE dwm_codeforces__handle_problem_first_accepted (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    problem_key varchar(128) NOT NULL,
    problem_index varchar(32) NULL,
    problem_name varchar(255) NULL,
    difficulty varchar(64) NULL,
    first_accepted_submission_id varchar(128) NOT NULL,
    first_accepted_at_utc_plus8 datetime(6) NOT NULL,
    first_accepted_date_utc_plus8 date NOT NULL,
    first_accepted_language varchar(255) NULL,
    first_accepted_source_url varchar(512) NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_dwm_codeforces_handle_problem (handle, problem_key),
    KEY idx_dwm_codeforces_problem (problem_key),
    KEY idx_dwm_codeforces_handle_date (handle, first_accepted_date_utc_plus8)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE dws_codeforces__handle_daily_rating_accepted_summary (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    accepted_date_utc_plus8 date NOT NULL,
    difficulty varchar(64) NOT NULL,
    accepted_problem_count int NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_dws_codeforces_handle_date_difficulty (
        handle, accepted_date_utc_plus8, difficulty
    ),
    KEY idx_dws_codeforces_date_difficulty (accepted_date_utc_plus8, difficulty)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE ods_atcoder__submission (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    atcoder_submission_id bigint NOT NULL,
    epoch_second bigint NOT NULL,
    problem_id varchar(128) NULL,
    contest_id varchar(128) NULL,
    user_id varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    language varchar(255) NULL,
    point decimal(12, 4) NULL,
    source_code_length int NULL,
    result varchar(64) NULL,
    execution_time_millis int NULL,
    batch_id varchar(128) NOT NULL,
    fetched_at datetime(6) NOT NULL,
    raw_payload longtext NOT NULL,
    payload_hash char(64) NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_ods_atcoder_submission_source (atcoder_submission_id),
    KEY idx_ods_atcoder_submission_user_time (user_id, epoch_second),
    KEY idx_ods_atcoder_submission_problem_time (problem_id, epoch_second),
    KEY idx_ods_atcoder_submission_contest_problem (contest_id, problem_id),
    KEY idx_ods_atcoder_submission_batch (batch_id)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE ods_atcoder__problem (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    problem_id varchar(128) NOT NULL,
    contest_id varchar(128) NULL,
    problem_index varchar(64) NULL,
    problem_name varchar(255) NULL,
    title varchar(512) NULL,
    batch_id varchar(128) NOT NULL,
    fetched_at datetime(6) NOT NULL,
    raw_payload longtext NOT NULL,
    payload_hash char(64) NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_ods_atcoder_problem_source (problem_id),
    KEY idx_ods_atcoder_problem_contest_index (contest_id, problem_index),
    KEY idx_ods_atcoder_problem_batch (batch_id)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE ods_atcoder__problem_model (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    problem_id varchar(128) NOT NULL,
    slope decimal(20, 12) NULL,
    intercept decimal(20, 12) NULL,
    variance decimal(20, 12) NULL,
    raw_difficulty int NULL,
    clipped_difficulty int NULL,
    discrimination decimal(20, 12) NULL,
    irt_loglikelihood decimal(20, 12) NULL,
    irt_users int NULL,
    is_experimental tinyint(1) NULL,
    batch_id varchar(128) NOT NULL,
    fetched_at datetime(6) NOT NULL,
    raw_payload longtext NOT NULL,
    payload_hash char(64) NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_ods_atcoder_problem_model_source (problem_id),
    KEY idx_ods_atcoder_problem_model_difficulty (clipped_difficulty),
    KEY idx_ods_atcoder_problem_model_batch (batch_id)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE dwd_atcoder__submission (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    ods_submission_id bigint NOT NULL,
    submission_id varchar(128) NOT NULL,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    submitted_at_utc_plus8 datetime(6) NULL,
    submitted_date_utc_plus8 date NULL,
    problem_key varchar(128) NULL,
    problem_index varchar(32) NULL,
    problem_name varchar(255) NULL,
    difficulty varchar(64) NULL,
    language varchar(255) NULL,
    verdict varchar(128) NULL,
    is_accepted tinyint(1) NOT NULL,
    time_consumed_millis int NULL,
    source_url varchar(512) NULL,
    ods_batch_id varchar(128) NOT NULL,
    ods_fetched_at datetime(6) NOT NULL,
    ods_payload_hash char(64) NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_dwd_atcoder_submission_id (submission_id),
    KEY idx_dwd_atcoder_submission_ods (ods_submission_id),
    KEY idx_dwd_atcoder_handle_time (handle, submitted_at_utc_plus8),
    KEY idx_dwd_atcoder_problem_time (problem_key, submitted_at_utc_plus8, submission_id),
    KEY idx_dwd_atcoder_handle_problem (handle, problem_key),
    KEY idx_dwd_atcoder_batch (ods_batch_id)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE dwm_atcoder__handle_problem_first_accepted (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    problem_key varchar(128) NOT NULL,
    problem_index varchar(32) NULL,
    problem_name varchar(255) NULL,
    difficulty varchar(64) NULL,
    first_accepted_submission_id varchar(128) NOT NULL,
    first_accepted_at_utc_plus8 datetime(6) NOT NULL,
    first_accepted_date_utc_plus8 date NOT NULL,
    first_accepted_language varchar(255) NULL,
    first_accepted_source_url varchar(512) NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_dwm_atcoder_handle_problem (handle, problem_key),
    KEY idx_dwm_atcoder_problem (problem_key),
    KEY idx_dwm_atcoder_handle_date (handle, first_accepted_date_utc_plus8)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE dws_atcoder__handle_daily_rating_accepted_summary (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    accepted_date_utc_plus8 date NOT NULL,
    difficulty varchar(64) NOT NULL,
    accepted_problem_count int NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    UNIQUE KEY uk_dws_atcoder_handle_date_difficulty (
        handle, accepted_date_utc_plus8, difficulty
    ),
    KEY idx_dws_atcoder_date_difficulty (accepted_date_utc_plus8, difficulty)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;
