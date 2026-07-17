USE mail_system;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE mails ADD COLUMN is_remote TINYINT(1) DEFAULT 0 COMMENT ''是否为远程邮件 0=本地 1=远程''',
                   'SELECT 1'
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mails'
      AND COLUMN_NAME = 'is_remote'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE mails ADD COLUMN sender_server VARCHAR(255) DEFAULT NULL COMMENT ''发送方服务器地址（远程邮件）''',
                   'SELECT 1'
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mails'
      AND COLUMN_NAME = 'sender_server'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE mails ADD COLUMN remote_sender_email VARCHAR(255) DEFAULT NULL COMMENT ''远程发送方/收件方完整邮箱地址''',
                   'SELECT 1'
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mails'
      AND COLUMN_NAME = 'remote_sender_email'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;