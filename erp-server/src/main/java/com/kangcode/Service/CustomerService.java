package com.kangcode.Service;

import com.kangcode.pojo.Customer;
import com.kangcode.pojo.CustomerQueryParam;
import com.kangcode.pojo.PageResult;

import java.util.List;

public interface CustomerService {
    void add(Customer customer);

    void deleteById(Integer id);

    void updateById(Customer customer);

    Customer getById(Integer id);

    PageResult page(CustomerQueryParam customerQueryParam);

    List<Customer> listAll();
}
