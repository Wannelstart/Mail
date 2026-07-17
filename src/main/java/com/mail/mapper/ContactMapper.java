package com.mail.mapper;

import com.mail.dto.response.ContactVO;
import com.mail.entity.Contact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContactMapper {
    List<ContactVO> findByOwner(@Param("ownerId") Long ownerId,
               @Param("groupName") String groupName);
    ContactVO findById(@Param("id") Long id, @Param("ownerId") Long ownerId);
    void insert(Contact contact);
    void update(Contact contact);
    void delete(@Param("id") Long id, @Param("ownerId") Long ownerId);
    List<ContactVO> search(@Param("ownerId") Long ownerId,
                   @Param("keyword") String keyword);
}
