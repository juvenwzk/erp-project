package com.kangcode.Mapper;

import com.kangcode.pojo.User;
import com.kangcode.pojo.UserQueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    void insertById(User user);

    void updateById(User user);

    User getById(Integer id);

    List<User> list(UserQueryParam userQueryParam);

    void delete(List<Integer> ids);

    User selectByUsername(String username);
}
