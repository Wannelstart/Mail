package com.mail.service;

import com.mail.dto.request.ContactSaveRequest;
import com.mail.dto.response.ContactVO;
import com.mail.entity.Contact;
import com.mail.entity.User;
import com.mail.mapper.ContactMapper;
import com.mail.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMapper contactMapper;
    private final UserMapper userMapper;

    public List<ContactVO> list(Long ownerId, String groupName) {
        return contactMapper.findByOwner(ownerId, groupName);
    }

    public List<ContactVO> search(Long ownerId, String keyword) {
     return contactMapper.search(ownerId, keyword);
    }

    public ContactVO add(Long ownerId, ContactSaveRequest req) {
        User target = userMapper.findByEmail(req.getEmail());

        Contact contact = new Contact();
        contact.setOwnerId(ownerId);
        contact.setNickname(req.getNickname());
        contact.setGroupName(req.getGroupName());
        contact.setEmail(req.getEmail());

        if (target != null) {
            // 站内用户
            if (target.getId().equals(ownerId)) throw new IllegalArgumentException("不能添加自己");
            contact.setContactId(target.getId());
        } else {
            // 外部联系人（如 QQ、网易邮箱）
            contact.setContactId(null);
        }

        contactMapper.insert(contact);
        return contactMapper.findById(contact.getId(), ownerId);
    }

    public ContactVO update(Long id, Long ownerId, ContactSaveRequest req) {
        ContactVO existing = contactMapper.findById(id, ownerId);
        if (existing == null) throw new IllegalArgumentException("联系人不存在");
        Contact contact = new Contact();
        contact.setId(id);
        contact.setOwnerId(ownerId);
        contact.setNickname(req.getNickname());
        contact.setGroupName(req.getGroupName());
        contactMapper.update(contact);
        return contactMapper.findById(id, ownerId);
    }

    public void delete(Long id, Long ownerId) {
        contactMapper.delete(id, ownerId);
    }
}
