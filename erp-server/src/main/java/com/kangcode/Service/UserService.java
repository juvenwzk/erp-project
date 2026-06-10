package com.kangcode.Service;

import com.kangcode.pojo.LoginInfo;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.User;
import com.kangcode.pojo.UserQueryParam;

import java.util.List;

public interface UserService {
    void add(User user);


    void update(User user);

    User getById(Integer id);

    PageResult page(UserQueryParam userQueryParam);

    void delete(List<Integer> ids);

    LoginInfo login(User user);
}
