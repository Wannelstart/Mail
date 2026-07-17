package com.mail.mapper;

import com.mail.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findById(@Param("id") Long id);
    User findByUsername(@Param("username") String username);
    User findByEmail(@Param("email") String email);
    void insert(User user);
    void updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
    void updateUsername(@Param("id") Long id, @Param("username") String username);
    java.util.List<User> findAllWithQqBinding();
    void updateQqBinding(@Param("id") Long id, @Param("qqEmail") String qqEmail, @Param("qqAuthCode") String qqAuthCode);
}
