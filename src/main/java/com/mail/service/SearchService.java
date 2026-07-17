package com.mail.service;

import com.mail.dto.response.MailListItem;
import com.mail.dto.response.PageResult;
import com.mail.mapper.MailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final MailMapper mailMapper;

    public PageResult<MailListItem> search(Long userId, String keyword,
                          String box, String searchIn,
                     int page, int size) {
    if (keyword == null || keyword.isBlank()) {
            return PageResult.of(0, page, size, java.util.Collections.emptyList());
        }
        int offset = (page - 1) * size;
      var list = mailMapper.search(userId, keyword, box, searchIn, offset, size);
        long total = mailMapper.countSearch(userId, keyword, box, searchIn);
        return PageResult.of(total, page, size, list);
    }
}
