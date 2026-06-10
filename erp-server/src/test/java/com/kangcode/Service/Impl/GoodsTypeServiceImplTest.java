package com.kangcode.Service.Impl;

import com.kangcode.Mapper.GoodsTypeMapper;
import com.kangcode.pojo.GoodsType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoodsTypeServiceImplTest {

    @Mock
    private GoodsTypeMapper goodsTypeMapper;

    @InjectMocks
    private GoodsTypeServiceImpl goodsTypeService;

    @Test
    void add_shouldCallMapper() {
        GoodsType type = new GoodsType(null, "电子产品", "备注");
        goodsTypeService.add(type);
        verify(goodsTypeMapper).insertById(type);
    }

    @Test
    void list_shouldReturnAllTypes() {
        GoodsType t1 = new GoodsType(1, "电子产品", null);
        GoodsType t2 = new GoodsType(2, "食品", null);
        when(goodsTypeMapper.list()).thenReturn(List.of(t1, t2));

        List<GoodsType> result = goodsTypeService.list();

        assertEquals(2, result.size());
        verify(goodsTypeMapper).list();
    }

    @Test
    void list_shouldReturnEmptyWhenNone() {
        when(goodsTypeMapper.list()).thenReturn(List.of());
        assertTrue(goodsTypeService.list().isEmpty());
        verify(goodsTypeMapper).list();
    }

    @Test
    void update_shouldCallMapper() {
        GoodsType type = new GoodsType(1, "更新类别", "新备注");
        goodsTypeService.update(type);
        verify(goodsTypeMapper).updateById(type);
    }

    @Test
    void deleteById_shouldCallMapper() {
        goodsTypeService.deleteById(1);
        verify(goodsTypeMapper).deleteById(1);
    }
}
