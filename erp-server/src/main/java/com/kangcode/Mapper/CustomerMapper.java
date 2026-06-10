package com.kangcode.Mapper;

import com.kangcode.pojo.Customer;
import com.kangcode.pojo.CustomerQueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CustomerMapper {
    void insert(Customer customer);

    void deleteById(Integer id);

    void updateById(Customer customer);

    Customer getById(Integer id);

    List<Customer> list(CustomerQueryParam customerQueryParam);

    List<Customer> listAll();
}
