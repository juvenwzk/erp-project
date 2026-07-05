package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.UserMapper;
import com.kangcode.Service.UserService;
import com.kangcode.pojo.LoginInfo;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.User;
import com.kangcode.pojo.UserQueryParam;
import com.kangcode.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;


    @Override
    public void add(User user) {
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        userMapper.insertById(user);
    }


    @Override
    public void update(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
    }

    @Override
    public User getById(Integer id) {
        User user = userMapper.getById(id);
        return user;
    }

    @Override
    public PageResult page(UserQueryParam userQueryParam) {
        //1.设置分页参数
        PageHelper.startPage(userQueryParam.getPage(), userQueryParam.getPageSize());
        //2.执行查询
        List<User> list = userMapper.list(userQueryParam);
        //3.解析查询结合，并封装
        Page< User> p=(Page< User>) list;
        return new PageResult(p.getTotal(),p.getResult());
    }

    @Override
    public void delete(List<Integer> ids) {
        userMapper.delete(ids);
    }
    //用户·登录
    @Override
    public LoginInfo login(User user) {
        //1调用Mapper接口，根据用户名查询用户

        User u=userMapper.selectByUsername(user.getUsername());

        //2.用户不存在
        if(u==null){
            log.info("登录失败：用户名不存在 -> {}", user.getUsername());
            throw new RuntimeException("用户不存在");
        }

        //3.密码加密后的对比
        String inputPassword = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());


        //4.密码错误（兼容历史明文密码，匹配后自动升级为 MD5）
        if (!inputPassword.equals(u.getPassword())) {
            if (!user.getPassword().equals(u.getPassword())) {
                log.info("登录失败：密码错误 -> {}", user.getUsername());
                throw new RuntimeException("密码错误");
            }
            User upgrade = new User();
            upgrade.setId(u.getId());
            upgrade.setPassword(inputPassword);
            userMapper.updateById(upgrade);
            u.setPassword(inputPassword);
            log.info("已将用户 {} 的明文密码升级为 MD5", u.getUsername());
        }
        //5.登录成功
        log.info("登录成功：{}", u.getUsername());
        String token = jwtUtils.generateToken(u);

        return new LoginInfo(
                u.getId(),
                u.getUsername(),
                null,
                u.getName(),
                u.getRole(),
                token);

    }


}
