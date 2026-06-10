package com.kangcode.Service.Impl;

import com.kangcode.Mapper.SupplierMapper;
import com.kangcode.pojo.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Test
    void add_shouldCallMapper() {
        Supplier supplier = new Supplier();
        supplier.setSupplierName("供应商1");
        supplierService.add(supplier);
        verify(supplierMapper).insert(supplier);
    }

    @Test
    void deleteById_shouldCallMapper() {
        supplierService.deleteById(1);
        verify(supplierMapper).deleteById(1);
    }

    @Test
    void update_shouldCallMapper() {
        Supplier supplier = new Supplier();
        supplier.setId(1);
        supplier.setSupplierName("更新");
        supplierService.update(supplier);
        verify(supplierMapper).updateById(supplier);
    }

    @Test
    void getById_shouldReturnSupplier() {
        Supplier expected = new Supplier();
        expected.setId(1);
        expected.setSupplierName("供应商1");
        when(supplierMapper.getById(1)).thenReturn(expected);

        Supplier actual = supplierService.getById(1);
        assertSame(expected, actual);
        verify(supplierMapper).getById(1);
    }

    @Test
    void getById_shouldReturnNullWhenNotFound() {
        when(supplierMapper.getById(99)).thenReturn(null);
        assertNull(supplierService.getById(99));
        verify(supplierMapper).getById(99);
    }

    @Test
    void list_shouldReturnAll() {
        Supplier s1 = new Supplier();
        s1.setId(1);
        Supplier s2 = new Supplier();
        s2.setId(2);
        when(supplierMapper.list()).thenReturn(List.of(s1, s2));

        List<Supplier> result = supplierService.list();
        assertEquals(2, result.size());
        verify(supplierMapper).list();
    }

    @Test
    void list_shouldReturnEmptyWhenNone() {
        when(supplierMapper.list()).thenReturn(List.of());
        assertTrue(supplierService.list().isEmpty());
        verify(supplierMapper).list();
    }
}
