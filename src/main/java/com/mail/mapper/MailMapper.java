package com.mail.mapper;

import com.mail.dto.response.MailListItem;
import com.mail.entity.Mail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MailMapper {

    // 收件箱
    List<MailListItem> findInbox(@Param("userId") Long userId,
                      @Param("offset") int offset,
                        @Param("size") int size);
    long countInbox(@Param("userId") Long userId);

    // 发件箱
    List<MailListItem> findSent(@Param("userId") Long userId,
                      @Param("offset") int offset,
          @Param("size") int size);
    long countSent(@Param("userId") Long userId);

    // 草稿箱
    List<MailListItem> findDrafts(@Param("userId") Long userId,
                           @Param("offset") int offset,
                       @Param("size") int size);
    long countDrafts(@Param("userId") Long userId);

    // 详情
    Mail findById(@Param("id") Long id);

    // 写操作
    void insert(Mail mail);
    void updateDraft(Mail mail);
    void updateToSent(@Param("id") Long id, @Param("sentAt") java.time.LocalDateTime sentAt);
    void softDeleteBySender(@Param("id") Long id, @Param("userId") Long userId);
    void softDeleteByReceiver(@Param("id") Long id, @Param("userId") Long userId);
    void updateHasAttachments(@Param("id") Long id);
    void clearHasAttachments(@Param("id") Long id);
    void recallToDraft(@Param("id") Long id, @Param("userId") Long userId);

    // 状态更新
    void updateStatus(@Param("id") Long id, @Param("status") int status);
    void updateSentAt(@Param("id") Long id, @Param("sentAt") java.time.LocalDateTime sentAt);
    List<Mail> findPendingExternalMails();

    // 搜索
    List<MailListItem> search(@Param("userId") Long userId,
             @Param("keyword") String keyword,
                         @Param("box") String box,
              @Param("searchIn") String searchIn,
           @Param("offset") int offset,
                @Param("size") int size);
    long countSearch(@Param("userId") Long userId,
              @Param("keyword") String keyword,
            @Param("box") String box,
                     @Param("searchIn") String searchIn);
}
