package com.kangcode.Mapper;

import com.kangcode.pojo.Supplier;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SupplierMapper {


    void insert(Supplier supplier);

    void deleteById(Integer id);

    void updateById(Supplier supplier);

    Supplier getById(Integer id);

    List<Supplier> list();
}
