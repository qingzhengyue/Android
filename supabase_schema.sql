-- Teacher
CREATE TABLE teacher (
    teacher_id SERIAL PRIMARY KEY,
    work_id TEXT NOT NULL,
    name TEXT NOT NULL,
    password TEXT NOT NULL,
    create_time BIGINT DEFAULT (extract(epoch from now()) * 1000)
);

-- Class
CREATE TABLE "class" (
    class_id SERIAL PRIMARY KEY,
    class_name TEXT NOT NULL,
    grade TEXT NOT NULL,
    teacher_id INT NOT NULL,
    create_time BIGINT DEFAULT (extract(epoch from now()) * 1000),
    is_active BOOLEAN DEFAULT TRUE
);

-- Student
CREATE TABLE student (
    student_id SERIAL PRIMARY KEY,
    student_number TEXT NOT NULL,
    name TEXT NOT NULL,
    password TEXT NOT NULL,
    class_id INT NOT NULL,
    register_time BIGINT DEFAULT (extract(epoch from now()) * 1000)
);

-- Learning Task
CREATE TABLE learning_task (
    task_id SERIAL PRIMARY KEY,
    task_name TEXT NOT NULL,
    task_detail TEXT NOT NULL,
    grade TEXT NOT NULL,
    deadline TEXT NOT NULL,
    deadline_time BIGINT NOT NULL,
    teacher_id INT NOT NULL,
    class_id INT NOT NULL,
    status TEXT NOT NULL
);

-- Scratch Draft
CREATE TABLE scratch_draft (
    draft_id SERIAL PRIMARY KEY,
    draft_name TEXT NOT NULL,
    block_code TEXT NOT NULL,
    student_id INT NOT NULL,
    task_id INT,
    create_time BIGINT DEFAULT (extract(epoch from now()) * 1000),
    last_modified_time BIGINT DEFAULT (extract(epoch from now()) * 1000)
);

-- Ai Teaching Config
CREATE TABLE ai_teaching_config (
    config_id SERIAL PRIMARY KEY,
    class_id INT NOT NULL,
    teacher_id INT NOT NULL,
    ai_hint_level TEXT NOT NULL,
    code_generation_limit INT NOT NULL,
    creative_guide_daily_limit INT NOT NULL
);

-- Ai Assist Record
CREATE TABLE ai_assist_record (
    call_id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    class_id INT NOT NULL,
    assist_type TEXT NOT NULL,
    assist_type_int INT DEFAULT 1,
    call_time BIGINT DEFAULT (extract(epoch from now()) * 1000),
    request_content TEXT NOT NULL,
    ai_result TEXT NOT NULL,
    draft_id INT
);

-- Scratch Work
CREATE TABLE scratch_work (
    work_id SERIAL PRIMARY KEY,
    work_name TEXT NOT NULL,
    work_code TEXT NOT NULL,
    student_id INT NOT NULL,
    class_id INT NOT NULL,
    task_id INT NOT NULL,
    submit_count INT NOT NULL,
    submit_time BIGINT DEFAULT (extract(epoch from now()) * 1000),
    review_status TEXT NOT NULL,
    teacher_score INT,
    teacher_comment TEXT,
    teacher_review_time BIGINT,
    is_public BOOLEAN DEFAULT FALSE,
    fork_from_id INT,
    likes_count INT DEFAULT 0,
    sync_status INT DEFAULT 1,
    plagiarism_flag BOOLEAN DEFAULT FALSE,
    similarity_score INT DEFAULT 0
);

-- Work Comment
CREATE TABLE work_comment (
    comment_id SERIAL PRIMARY KEY,
    work_id INT NOT NULL,
    author_student_id INT NOT NULL,
    author_name TEXT NOT NULL,
    content TEXT NOT NULL,
    create_time BIGINT DEFAULT (extract(epoch from now()) * 1000),
    is_approved BOOLEAN DEFAULT TRUE
);

-- Work AI Report
CREATE TABLE work_ai_report (
    report_id SERIAL PRIMARY KEY,
    work_id INT NOT NULL,
    student_id INT NOT NULL,
    grammar_score INT NOT NULL,
    logic_score INT NOT NULL,
    task_match_score INT NOT NULL,
    creative_score INT NOT NULL,
    average_score INT NOT NULL,
    optimization_suggestions TEXT NOT NULL,
    report_time BIGINT DEFAULT (extract(epoch from now()) * 1000)
);

-- Work Likes
CREATE TABLE work_likes (
    work_id INT NOT NULL,
    student_id TEXT NOT NULL,
    create_time BIGINT DEFAULT (extract(epoch from now()) * 1000),
    PRIMARY KEY (work_id, student_id)
);
