package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.CustomerMapper;
import com.kangcode.Service.CustomerService;
import com.kangcode.pojo.Customer;
import com.kangcode.pojo.CustomerQueryParam;
import com.kangcode.pojo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class CustomerServiceImpl implements CustomerService {
    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public void add(Customer customer) {
        if (customer.getCustName() == null || customer.getCustName().isBlank()) {
            throw new RuntimeException("客户名称不能为空");
        }
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());

        customerMapper.insert(customer);

    }

    @Override
    public void deleteById(Integer id) {
        customerMapper.deleteById(id);
    }

    @Override
    public void updateById(Customer customer) {
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);

    }

    @Override
    public List<Customer> listAll() {
        return customerMapper.listAll();
    }
    //根据用户名查询
    @Override
    public List<Customer> searchByName(String keyword) {

        CustomerQueryParam param=new CustomerQueryParam();
        param.setCustName(keyword);
        param.setPage(1);
        param.setPageSize(10);
        PageResult result=page(param);
        return (List<Customer>) result.getRows();
    }

    @Override
    public Customer getById(Integer id) {
        Customer customer = customerMapper.getById(id);
        return customer;
    }

    @Override
    public PageResult page(CustomerQueryParam customerQueryParam) {
        //1.设置分页参数
        PageHelper.startPage(customerQueryParam.getPage(), customerQueryParam.getPageSize());
        //2.执行查询
        List< Customer> list=customerMapper.list(customerQueryParam);
        //3.解析查询结合，并封装
        Page< Customer> p=(Page< Customer>) list;
        return new PageResult(p.getTotal(),p.getResult());
    }
}
