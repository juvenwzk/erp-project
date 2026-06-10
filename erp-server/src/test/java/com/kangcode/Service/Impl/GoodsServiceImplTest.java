package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.GoodsMapper;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.GoodsQueryParam;
import com.kangcode.pojo.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoodsServiceImplTest {

    @Mock
    private GoodsMapper goodsMapper;

    @InjectMocks
    private GoodsServiceImpl goodsService;

    @Test
    void add_shouldSetCreateAndUpdateTime() {
        Good good = new Good();
        good.setGoodsName("测试商品");

        goodsService.add(good);

        assertNotNull(good.getCreateTime());
        assertNotNull(good.getUpdateTime());
        verify(goodsMapper).insert(good);
    }

    @Test
    void getById_shouldReturnGood() {
        Good expected = new Good();
        expected.setId(1);
        expected.setGoodsName("商品1");
        when(goodsMapper.getById(1)).thenReturn(expected);

        Good actual = goodsService.getById(1);

        assertSame(expected, actual);
        verify(goodsMapper).getById(1);
    }

    @Test
    void getById_shouldReturnNullWhenNotFound() {
        when(goodsMapper.getById(99)).thenReturn(null);
        assertNull(goodsService.getById(99));
        verify(goodsMapper).getById(99);
    }

    @Test
    void delete_shouldCallMapper() {
        List<Integer> ids = List.of(1, 2, 3);
        goodsService.delete(ids);
        verify(goodsMapper).delete(ids);
    }

    @Test
    void update_shouldCallMapper() {
        Good good = new Good();
        good.setId(1);
        good.setGoodsName("更新");

        goodsService.update(good);

        verify(goodsMapper).update(good);
    }

    @Test
    @SuppressWarnings("unchecked")
    void page_shouldReturnPageResult() {
        GoodsQueryParam param = new GoodsQueryParam();
        param.setPage(1);
        param.setPageSize(10);
        Good g1 = new Good();
        g1.setId(1);
        Good g2 = new Good();
        g2.setId(2);
        Page<Good> mockPage = mock(Page.class);
        when(mockPage.getTotal()).thenReturn(2L);
        when(mockPage.getResult()).thenReturn(List.of(g1, g2));

        try (MockedStatic<PageHelper> pageHelper = mockStatic(PageHelper.class)) {
            when(goodsMapper.list(param)).thenReturn(mockPage);

            PageResult result = goodsService.page(param);

            pageHelper.verify(() -> PageHelper.startPage(1, 10));
            assertEquals(2L, result.getTotal());
            assertEquals(2, result.getRows().size());
        }
    }
}
