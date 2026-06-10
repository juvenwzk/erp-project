package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.CustomerMapper;
import com.kangcode.pojo.Customer;
import com.kangcode.pojo.CustomerQueryParam;
import com.kangcode.pojo.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void add_shouldSetCreateAndUpdateTime() {
        Customer customer = new Customer();
        customer.setCustName("测试客户");

        customerService.add(customer);

        assertNotNull(customer.getCreateTime());
        assertNotNull(customer.getUpdateTime());
        assertEquals(LocalDate.now(), customer.getCreateTime());
        assertEquals(LocalDate.now(), customer.getUpdateTime());
        verify(customerMapper).insert(customer);
    }

    @Test
    void deleteById_shouldCallMapper() {
        customerService.deleteById(1);
        verify(customerMapper).deleteById(1);
    }

    @Test
    void updateById_shouldSetUpdateTime() {
        Customer customer = new Customer();
        customer.setId(1);
        customer.setCustName("更新");

        customerService.updateById(customer);

        assertNotNull(customer.getUpdateTime());
        assertEquals(LocalDate.now(), customer.getUpdateTime());
        verify(customerMapper).updateById(customer);
    }

    @Test
    void getById_shouldReturnCustomer() {
        Customer expected = new Customer();
        expected.setId(1);
        expected.setCustName("客户1");
        when(customerMapper.getById(1)).thenReturn(expected);

        Customer actual = customerService.getById(1);

        assertSame(expected, actual);
        verify(customerMapper).getById(1);
    }

    @Test
    void getById_shouldReturnNullWhenNotFound() {
        when(customerMapper.getById(99)).thenReturn(null);

        Customer actual = customerService.getById(99);

        assertNull(actual);
        verify(customerMapper).getById(99);
    }

    @Test
    @SuppressWarnings("unchecked")
    void page_shouldReturnPageResult() {
        CustomerQueryParam param = new CustomerQueryParam(1, 10, null, null);
        Customer c1 = new Customer();
        c1.setId(1);
        Customer c2 = new Customer();
        c2.setId(2);
        Page<Customer> mockPage = mock(Page.class);
        when(mockPage.getTotal()).thenReturn(2L);
        when(mockPage.getResult()).thenReturn(List.of(c1, c2));

        try (MockedStatic<PageHelper> pageHelper = mockStatic(PageHelper.class)) {
            when(customerMapper.list(param)).thenReturn(mockPage);

            PageResult result = customerService.page(param);

            pageHelper.verify(() -> PageHelper.startPage(1, 10));
            assertEquals(2L, result.getTotal());
            assertEquals(2, result.getRows().size());
        }
    }
}
