PRAGMA journal_mode=WAL;
PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS sys_user (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT    NOT NULL UNIQUE,
    password    TEXT    NOT NULL,
    nickname    TEXT,
    avatar_url  TEXT,
    role        TEXT    NOT NULL DEFAULT 'USER',
    status      INTEGER NOT NULL DEFAULT 1,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS category (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL,
    parent_id  INTEGER,
    sort       INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS book (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    title          TEXT    NOT NULL,
    author         TEXT,
    isbn           TEXT,
    description    TEXT,
    cover_url      TEXT,
    category_id    INTEGER,
    file_path      TEXT    NOT NULL,
    file_format    TEXT    NOT NULL,
    file_size      INTEGER NOT NULL DEFAULT 0,
    file_hash      TEXT    NOT NULL,
    version_no     INTEGER NOT NULL DEFAULT 1,
    uploader_id    INTEGER,
    upload_ip      TEXT,
    status         INTEGER NOT NULL DEFAULT 0,
    download_count INTEGER NOT NULL DEFAULT 0,
    created_at     TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at     TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (category_id) REFERENCES category(id)
);
CREATE INDEX IF NOT EXISTS idx_book_category ON book(category_id);
CREATE INDEX IF NOT EXISTS idx_book_title ON book(title);
CREATE INDEX IF NOT EXISTS idx_book_hash ON book(file_hash);
CREATE INDEX IF NOT EXISTS idx_book_status ON book(status);

CREATE TABLE IF NOT EXISTS comment (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id    INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    content    TEXT    NOT NULL,
    parent_id  INTEGER,
    like_count INTEGER NOT NULL DEFAULT 0,
    status     INTEGER NOT NULL DEFAULT 1,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (book_id) REFERENCES book(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
CREATE INDEX IF NOT EXISTS idx_comment_book ON comment(book_id);
CREATE INDEX IF NOT EXISTS idx_comment_parent ON comment(parent_id);

CREATE TABLE IF NOT EXISTS comment_like (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    comment_id INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES comment(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS favorite (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id    INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (book_id, user_id),
    FOREIGN KEY (book_id) REFERENCES book(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS reading_progress (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id    INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    position   TEXT,
    updated_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (book_id, user_id),
    FOREIGN KEY (book_id) REFERENCES book(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS trace_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id     INTEGER NOT NULL,
    action      TEXT    NOT NULL,
    operator_id INTEGER,
    detail      TEXT,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (book_id) REFERENCES book(id)
);
CREATE INDEX IF NOT EXISTS idx_trace_book ON trace_log(book_id);

CREATE TABLE IF NOT EXISTS version_history (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id    INTEGER NOT NULL,
    version_no INTEGER NOT NULL,
    file_hash  TEXT    NOT NULL,
    change_log TEXT,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (book_id) REFERENCES book(id)
);
CREATE INDEX IF NOT EXISTS idx_version_book ON version_history(book_id);

CREATE TABLE IF NOT EXISTS system_log (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    trace_id   TEXT,
    level      TEXT,
    module     TEXT,
    message    TEXT,
    request_uri TEXT,
    method     TEXT,
    ip         TEXT,
    cost_ms    INTEGER,
    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_syslog_level ON system_log(level);
CREATE INDEX IF NOT EXISTS idx_syslog_created ON system_log(created_at);

CREATE TABLE IF NOT EXISTS frontend_monitor (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    page_url   TEXT,
    fcp_ms     INTEGER,
    lcp_ms     INTEGER,
    js_errors  INTEGER DEFAULT 0,
    api_total  INTEGER DEFAULT 0,
    api_fail   INTEGER DEFAULT 0,
    user_agent TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS system_config (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key   TEXT NOT NULL UNIQUE,
    config_value TEXT,
    description  TEXT,
    updated_at   TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS user_action_log (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    action     TEXT,
    detail     TEXT,
    ip         TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_action_user ON user_action_log(user_id);

CREATE TABLE IF NOT EXISTS post (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    title      TEXT    NOT NULL,
    content    TEXT,
    images     TEXT,
    status     INTEGER NOT NULL DEFAULT 1,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
CREATE INDEX IF NOT EXISTS idx_post_user ON post(user_id);
CREATE INDEX IF NOT EXISTS idx_post_created ON post(created_at);

CREATE TABLE IF NOT EXISTS post_like (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id    INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES post(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
CREATE INDEX IF NOT EXISTS idx_post_like_post ON post_like(post_id);

CREATE TABLE IF NOT EXISTS post_comment (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id    INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    content    TEXT    NOT NULL,
    parent_id  INTEGER,
    like_count INTEGER NOT NULL DEFAULT 0,
    status     INTEGER NOT NULL DEFAULT 1,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (post_id) REFERENCES post(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
CREATE INDEX IF NOT EXISTS idx_post_comment_post ON post_comment(post_id);

CREATE TABLE IF NOT EXISTS post_comment_like (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    comment_id INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    created_at TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES post_comment(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
CREATE INDEX IF NOT EXISTS idx_post_comment_like ON post_comment_like(comment_id);

CREATE TABLE IF NOT EXISTS follow (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    follower_id INTEGER NOT NULL,
    followee_id INTEGER NOT NULL,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (follower_id, followee_id),
    FOREIGN KEY (follower_id) REFERENCES sys_user(id),
    FOREIGN KEY (followee_id) REFERENCES sys_user(id)
);
CREATE INDEX IF NOT EXISTS idx_follow_follower ON follow(follower_id);
CREATE INDEX IF NOT EXISTS idx_follow_followee ON follow(followee_id);

CREATE TABLE IF NOT EXISTS user_request (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    type         TEXT    NOT NULL DEFAULT 'PASSWORD_RESET',
    username     TEXT    NOT NULL,
    email        TEXT    NOT NULL,
    detail       TEXT,
    status       INTEGER NOT NULL DEFAULT 0,
    requested_by INTEGER,
    processed_by INTEGER,
    processed_at TEXT,
    images       TEXT,
    created_at   TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (requested_by) REFERENCES sys_user(id)
);
CREATE INDEX IF NOT EXISTS idx_user_request_status ON user_request(status);
CREATE INDEX IF NOT EXISTS idx_user_request_username ON user_request(username);

CREATE TABLE IF NOT EXISTS visit_log (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    username   TEXT,
    nickname   TEXT,
    ip         TEXT,
    page_url   TEXT,
    user_agent TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_visit_created ON visit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_visit_user ON visit_log(user_id);
