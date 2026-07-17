package com.mail.mapper;

import com.mail.entity.MailFlag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MailFlagMapper {
    void upsert(MailFlag flag);
    MailFlag findByMailAndUser(@Param("mailId") Long mailId, @Param("userId") Long userId);
    void initFlag(@Param("mailId") Long mailId, @Param("userId") Long userId);
}
