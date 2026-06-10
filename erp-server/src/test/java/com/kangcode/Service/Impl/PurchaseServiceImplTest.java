package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.GoodsMapper;
import com.kangcode.Mapper.PurchaseMapper;
import com.kangcode.Mapper.SupplierMapper;
import com.kangcode.Service.PurchaseService;
import com.kangcode.pojo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplTest {

    @Mock
    private PurchaseMapper purchaseMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private Good good;
    private Supplier supplier;
    private PurchaseOrder order;

    @BeforeEach
    void setUp() {
        good = new Good();
        good.setId(1);
        good.setGoodsName("测试商品");
        good.setBuyPrice(100.0);
        good.setStockNum(50);

        supplier = new Supplier();
        supplier.setId(1);
        supplier.setSupplierName("测试供应商");

        order = new PurchaseOrder();
        order.setGoodsId(1);
        order.setSupplierId(1);
        order.setBuyNum(10);
        order.setUserId(1);
    }

    @Test
    void addOrder_shouldCalculateTotalAndSetStatus() {
        when(goodsMapper.getById(1)).thenReturn(good);
        when(supplierMapper.getById(1)).thenReturn(supplier);

        purchaseService.addOrder(order);

        assertEquals(1000.0, order.getTotalMoney());
        assertEquals(PurchaseService.STATUS_NOT_IN, order.getStatus());
        assertNotNull(order.getBuyTime());
        assertNotNull(order.getCreateTime());
        assertNotNull(order.getUpdateTime());
        verify(purchaseMapper).addOrder(order);
    }

    @Test
    void addOrder_shouldThrowWhenGoodsIdNull() {
        order.setGoodsId(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.addOrder(order));
        assertEquals("商品ID不能为空", e.getMessage());
        verifyNoInteractions(goodsMapper, supplierMapper, purchaseMapper);
    }

    @Test
    void addOrder_shouldThrowWhenSupplierIdNull() {
        order.setSupplierId(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.addOrder(order));
        assertEquals("供应商ID不能为空", e.getMessage());
        verifyNoInteractions(goodsMapper, supplierMapper, purchaseMapper);
    }

    @Test
    void addOrder_shouldThrowWhenBuyNumNull() {
        order.setBuyNum(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.addOrder(order));
        assertEquals("采购数量不能为空", e.getMessage());
        verifyNoInteractions(goodsMapper, supplierMapper, purchaseMapper);
    }

    @Test
    void addOrder_shouldThrowWhenBuyNumZero() {
        order.setBuyNum(0);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.addOrder(order));
        assertEquals("采购数量必须大于0", e.getMessage());
        verifyNoInteractions(goodsMapper, supplierMapper, purchaseMapper);
    }

    @Test
    void addOrder_shouldThrowWhenGoodsNotFound() {
        when(goodsMapper.getById(1)).thenReturn(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.addOrder(order));
        assertEquals("商品不存在", e.getMessage());
        verify(purchaseMapper, never()).addOrder(any());
    }

    @Test
    void addOrder_shouldThrowWhenSupplierNotFound() {
        when(goodsMapper.getById(1)).thenReturn(good);
        when(supplierMapper.getById(1)).thenReturn(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.addOrder(order));
        assertEquals("供应商不存在", e.getMessage());
        verify(purchaseMapper, never()).addOrder(any());
    }

    @Test
    void updateOrder_shouldRecalculateWhenGoodsOrNumChanged() {
        PurchaseOrder oldOrder = new PurchaseOrder();
        oldOrder.setId(1);
        oldOrder.setGoodsId(1);
        oldOrder.setBuyNum(5);
        oldOrder.setStatus(PurchaseService.STATUS_NOT_IN);

        PurchaseOrder update = new PurchaseOrder();
        update.setId(1);
        update.setGoodsId(1);
        update.setBuyNum(20);
        update.setSupplierId(1);

        when(purchaseMapper.getById(1)).thenReturn(oldOrder);
        when(goodsMapper.getById(1)).thenReturn(good);

        purchaseService.updateOrder(update);

        assertEquals(2000.0, update.getTotalMoney());
        assertNotNull(update.getUpdateTime());
        verify(purchaseMapper).updateById(update);
    }

    @Test
    void updateOrder_shouldThrowWhenOrderNotFound() {
        when(purchaseMapper.getById(99)).thenReturn(null);
        PurchaseOrder update = new PurchaseOrder();
        update.setId(99);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.updateOrder(update));
        assertEquals("订单不存在", e.getMessage());
    }

    @Test
    void updateOrder_shouldThrowWhenAlreadyInStock() {
        PurchaseOrder oldOrder = new PurchaseOrder();
        oldOrder.setId(1);
        oldOrder.setStatus(PurchaseService.STATUS_INED);
        when(purchaseMapper.getById(1)).thenReturn(oldOrder);

        PurchaseOrder update = new PurchaseOrder();
        update.setId(1);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.updateOrder(update));
        assertEquals("订单已入库，不能修改", e.getMessage());
    }

    @Test
    void getById_shouldReturnOrder() {
        PurchaseOrder expected = new PurchaseOrder();
        expected.setId(1);
        when(purchaseMapper.getById(1)).thenReturn(expected);

        PurchaseOrder actual = purchaseService.getById(1);

        assertSame(expected, actual);
        verify(purchaseMapper).getById(1);
    }

    @Test
    void deleteById_shouldSucceedWhenNotInStock() {
        when(purchaseMapper.getById(1)).thenReturn(order);
        order.setStatus(PurchaseService.STATUS_NOT_IN);

        purchaseService.deleteById(1);

        verify(purchaseMapper).deleteById(1);
    }

    @Test
    void deleteById_shouldThrowWhenAlreadyInStock() {
        when(purchaseMapper.getById(1)).thenReturn(order);
        order.setStatus(PurchaseService.STATUS_INED);

        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.deleteById(1));
        assertEquals("只有未入库可删除", e.getMessage());
        verify(purchaseMapper, never()).deleteById(any());
    }

    @Test
    void deleteById_shouldThrowWhenOrderNotFound() {
        when(purchaseMapper.getById(99)).thenReturn(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.deleteById(99));
        assertEquals("订单不存在", e.getMessage());
    }

    @Test
    void inStock_shouldIncreaseStockAndUpdateStatus() {
        when(purchaseMapper.getById(1)).thenReturn(order);
        order.setStatus(PurchaseService.STATUS_NOT_IN);
        order.setBuyNum(10);
        order.setGoodsId(1);
        when(goodsMapper.getById(1)).thenReturn(good);

        purchaseService.inStock(1);

        assertEquals(60, good.getStockNum());
        assertEquals(PurchaseService.STATUS_INED, order.getStatus());
        verify(goodsMapper).update(good);
        verify(purchaseMapper).updateById(order);
    }

    @Test
    void inStock_shouldThrowWhenAlreadyInStock() {
        when(purchaseMapper.getById(1)).thenReturn(order);
        order.setStatus(PurchaseService.STATUS_INED);

        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.inStock(1));
        assertEquals("只有未入库可入库", e.getMessage());
    }

    @Test
    void cancel_shouldSetCanceledStatus() {
        when(purchaseMapper.getById(1)).thenReturn(order);
        order.setStatus(PurchaseService.STATUS_NOT_IN);

        purchaseService.cancel(1);

        assertEquals(PurchaseService.STATUS_CANCELED, order.getStatus());
        assertNotNull(order.getUpdateTime());
        verify(purchaseMapper).updateById(order);
    }

    @Test
    void cancel_shouldThrowWhenAlreadyInStock() {
        when(purchaseMapper.getById(1)).thenReturn(order);
        order.setStatus(PurchaseService.STATUS_INED);

        RuntimeException e = assertThrows(RuntimeException.class, () -> purchaseService.cancel(1));
        assertEquals("只有未入库可取消", e.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void page_shouldReturnPageResult() {
        PurchaseQueryParam param = new PurchaseQueryParam();
        param.setPage(1);
        param.setPageSize(10);
        PurchaseOrder o1 = new PurchaseOrder();
        o1.setId(1);
        PurchaseOrder o2 = new PurchaseOrder();
        o2.setId(2);
        Page<PurchaseOrder> mockPage = mock(Page.class);
        when(mockPage.getTotal()).thenReturn(2L);
        when(purchaseMapper.list(param)).thenReturn(mockPage);

        try (MockedStatic<PageHelper> pageHelper = mockStatic(PageHelper.class)) {
            PageResult result = purchaseService.page(param);
            pageHelper.verify(() -> PageHelper.startPage(1, 10));
            assertEquals(2L, result.getTotal());
        }
    }
}
