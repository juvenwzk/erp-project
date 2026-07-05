package com.kangcode.Service;

import com.kangcode.pojo.Supplier;

import java.util.List;

public interface SupplierService {

    void add(Supplier supplier);

    void deleteById(Integer id);

    void update(Supplier supplier);

    Supplier getById(Integer id);

    List<Supplier> list();

    List<Supplier> searchByName(String keyword);
}
