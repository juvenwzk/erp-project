package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.UserMapper;
import com.kangcode.pojo.LoginInfo;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.User;
import com.kangcode.pojo.UserQueryParam;
import com.kangcode.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void add_shouldCallMapper() {
        User user = new User();
        user.setUsername("test");
        userService.add(user);
        verify(userMapper).insertById(user);
    }

    @Test
    void update_shouldCallMapper() {
        User user = new User();
        user.setId(1);
        user.setName("更新");
        userService.update(user);
        verify(userMapper).updateById(user);
    }

    @Test
    void getById_shouldReturnUser() {
        User expected = new User();
        expected.setId(1);
        expected.setUsername("admin");
        when(userMapper.getById(1)).thenReturn(expected);

        User actual = userService.getById(1);

        assertSame(expected, actual);
        verify(userMapper).getById(1);
    }

    @Test
    void delete_shouldCallMapper() {
        List<Integer> ids = List.of(1, 2);
        userService.delete(ids);
        verify(userMapper).delete(ids);
    }

    @Test
    void login_shouldReturnLoginInfoWithTokenWhenSuccess() {
        User input = new User();
        input.setUsername("admin");
        input.setPassword("123456");

        User dbUser = new User();
        dbUser.setId(1);
        dbUser.setUsername("admin");
        dbUser.setPassword("123456");
        dbUser.setName("管理员");
        dbUser.setRole(1);

        when(userMapper.selectByUsernameAndPassword(input)).thenReturn(dbUser);
        when(jwtUtils.generateToken(dbUser)).thenReturn("mock-jwt-token");

        LoginInfo info = userService.login(input);

        assertNotNull(info);
        assertEquals(1, info.getId());
        assertEquals("admin", info.getUsername());
        assertEquals("123456", info.getPassword());
        assertEquals("管理员", info.getName());
        assertEquals(1, info.getRole());
        assertEquals("mock-jwt-token", info.getToken());
        verify(userMapper).selectByUsernameAndPassword(input);
        verify(jwtUtils).generateToken(dbUser);
    }

    @Test
    void login_shouldReturnNullWhenUserNotFound() {
        User input = new User();
        input.setUsername("wrong");
        input.setPassword("wrong");
        when(userMapper.selectByUsernameAndPassword(input)).thenReturn(null);

        LoginInfo info = userService.login(input);

        assertNull(info);
        verify(userMapper).selectByUsernameAndPassword(input);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @SuppressWarnings("unchecked")
    void page_shouldReturnPageResult() {
        UserQueryParam param = new UserQueryParam();
        param.setPage(1);
        param.setPageSize(10);
        User u1 = new User();
        u1.setId(1);
        User u2 = new User();
        u2.setId(2);
        Page<User> mockPage = mock(Page.class);
        when(mockPage.getTotal()).thenReturn(2L);
        when(mockPage.getResult()).thenReturn(List.of(u1, u2));

        try (MockedStatic<PageHelper> pageHelper = mockStatic(PageHelper.class)) {
            when(userMapper.list(param)).thenReturn(mockPage);

            PageResult result = userService.page(param);

            pageHelper.verify(() -> PageHelper.startPage(1, 10));
            assertEquals(2L, result.getTotal());
            assertEquals(2, result.getRows().size());
        }
    }
}
