package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.SupplierMapper;
import com.kangcode.Service.SupplierService;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {
    @Autowired
    private SupplierMapper supplierMapper;


    @Override
    public void add(Supplier supplier) {
        supplierMapper.insert(supplier);
    }

    @Override
    public void deleteById(Integer id) {
        supplierMapper.deleteById(id);
    }

    @Override
    public void update(Supplier supplier) {
        supplierMapper.updateById(supplier);
    }

    @Override
    public Supplier getById(Integer id) {


        return supplierMapper.getById(id);
    }

    @Override
    public List<Supplier> list() {
       return supplierMapper.list();
    }
}
