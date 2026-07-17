package com.mail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时邮件接收调度器
 * 每 60 秒连接一次 IMAP 服务器，拉取新邮件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailReceiverScheduler {

    private final IncomingMailService incomingMailService;

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void receiveMails() {
        try {
            incomingMailService.fetchAndProcessMails();
        } catch (Exception e) {
            log.error("定时邮件接收任务异常", e);
        }
    }
}
