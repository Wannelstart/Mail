CREATE DATABASE IF NOT EXISTS mail_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mail_system;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt          VARCHAR(64)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mails (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id           BIGINT,
    receiver_id         BIGINT,
    subject             VARCHAR(255) DEFAULT '',
    content         LONGTEXT,
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0=草稿 1=已发',
    sender_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    receiver_deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at             DATETIME,
    has_attachments     TINYINT(1)   NOT NULL DEFAULT 0,
    is_remote           TINYINT(1)   NOT NULL DEFAULT 0,
    sender_server       VARCHAR(255),
    remote_sender_email VARCHAR(100),
    INDEX idx_receiver  (receiver_id, status, receiver_deleted),
    INDEX idx_sender    (sender_id, status, sender_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attachments (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_id      BIGINT       NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_path    VARCHAR(512) NOT NULL,
    file_size    BIGINT       NOT NULL DEFAULT 0,
    content_type VARCHAR(100),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mail_id (mail_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mail_flags (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_id      BIGINT     NOT NULL,
    user_id      BIGINT     NOT NULL,
    is_read  TINYINT(1) NOT NULL DEFAULT 0,
    is_starred   TINYINT(1) NOT NULL DEFAULT 0,
    is_important TINYINT(1) NOT NULL DEFAULT 0,
    created_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mail_user (mail_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contacts (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id   BIGINT       NOT NULL,
    contact_id BIGINT       NOT NULL,
    nickname   VARCHAR(50),
    group_name VARCHAR(50)  DEFAULT '默认分组',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_owner_contact (owner_id, contact_id),
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
