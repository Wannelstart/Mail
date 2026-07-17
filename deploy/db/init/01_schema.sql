CREATE DATABASE IF NOT EXISTS mail_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mail_system;

CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mails (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     sender_id BIGINT NULL,
                                     receiver_id BIGINT NULL,
                                     subject VARCHAR(255) NULL,
    content MEDIUMTEXT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    sender_deleted TINYINT NOT NULL DEFAULT 0,
    receiver_deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    has_attachments TINYINT NOT NULL DEFAULT 0,
    is_remote TINYINT(1) DEFAULT 0,
    sender_server VARCHAR(255) DEFAULT NULL,
    remote_sender_email VARCHAR(255) DEFAULT NULL,
    KEY idx_sender (sender_id),
    KEY idx_receiver (receiver_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attachments (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           mail_id BIGINT NOT NULL,
                                           file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    content_type VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_mail (mail_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contacts (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        owner_id BIGINT NOT NULL,
                                        contact_id BIGINT NULL,
                                        email VARCHAR(100) NULL,
                                        nickname VARCHAR(100) NULL,
    group_name VARCHAR(50) DEFAULT '默认分组',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_owner_contact (owner_id, contact_id),
    KEY idx_owner (owner_id),
    KEY idx_contact_id (contact_id),
    CONSTRAINT contacts_ibfk_1 FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT contacts_ibfk_2 FOREIGN KEY (contact_id) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mail_flags (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          mail_id BIGINT NOT NULL,
                                          user_id BIGINT NOT NULL,
                                          is_read TINYINT NOT NULL DEFAULT 0,
                                          is_starred TINYINT NOT NULL DEFAULT 0,
                                          is_important TINYINT NOT NULL DEFAULT 0,
                                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                          UNIQUE KEY uk_mail_user (mail_id, user_id),
    KEY idx_user_mail (user_id, mail_id),
    CONSTRAINT mail_flags_ibfk_1 FOREIGN KEY (mail_id) REFERENCES mails(id) ON DELETE CASCADE,
    CONSTRAINT mail_flags_ibfk_2 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;