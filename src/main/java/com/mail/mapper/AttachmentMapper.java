package com.mail.mapper;

import com.mail.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttachmentMapper {
    List<Attachment> findByMailId(@Param("mailId") Long mailId);
    Attachment findById(@Param("id") Long id);
    void insertBatch(@Param("list") List<Attachment> list);
    void copyFromMail(@Param("sourceMailId") Long sourceMailId,
                   @Param("targetMailId") Long targetMailId);
    void copySelected(@Param("sourceMailId") Long sourceMailId,
                      @Param("targetMailId") Long targetMailId,
                      @Param("ids") List<Long> ids);
    void deleteByMailIdAndIds(@Param("mailId") Long mailId,
                @Param("ids") List<Long> ids);
    int countByMailId(@Param("mailId") Long mailId);
    List<Attachment> findExpired(@Param("days") int days);
    void deleteByIds(@Param("ids") List<Long> ids);
}
