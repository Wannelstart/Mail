package com.mail.entity;

import lombok.Getter;

@Getter
public enum MailStatus {

    DRAFT(0, "草稿"),
    SENT(1, "已发送"),
    PENDING(2, "发送中"),
    FAILED(3, "发送失败");

    private final int code;
    private final String description;

    MailStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
