package com.mail.service;

import com.mail.entity.Attachment;
import com.mail.mapper.AttachmentMapper;
import com.mail.mapper.MailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentCleanupScheduler {

    private final AttachmentMapper attachmentMapper;
    private final MailMapper mailMapper;

    /**
     * 每天凌晨 3 点清理过期附件（超过 30 天）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredAttachments() {
        log.info("开始清理过期附件...");

        List<Attachment> expired = attachmentMapper.findExpired(30);

        if (expired.isEmpty()) {
            log.info("没有过期附件需要清理");
            return;
        }

        log.info("发现 {} 个过期附件，开始清理", expired.size());

        // 删除物理文件
        int fileDeleted = 0;
        for (Attachment att : expired) {
            try {
                Path path = Path.of(att.getFilePath());
                if (Files.exists(path)) {
                    Files.delete(path);
                    fileDeleted++;
                }
            } catch (IOException e) {
                log.warn("删除文件失败: {}, 错误: {}", att.getFilePath(), e.getMessage());
            }
        }

        // 删除数据库记录
        List<Long> ids = expired.stream().map(Attachment::getId).collect(Collectors.toList());
        attachmentMapper.deleteByIds(ids);

        // 更新相关邮件的 has_attachments 标记
        expired.stream()
                .map(Attachment::getMailId)
                .distinct()
                .forEach(mailId -> {
                    int count = attachmentMapper.countByMailId(mailId);
                    if (count == 0) {
                        mailMapper.clearHasAttachments(mailId);
                    }
                });

        log.info("清理完成: 删除 {} 个文件, {} 条记录", fileDeleted, ids.size());
    }
}
