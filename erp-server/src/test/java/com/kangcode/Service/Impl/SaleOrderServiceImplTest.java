package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.CustomerMapper;
import com.kangcode.Mapper.GoodsMapper;
import com.kangcode.Mapper.SaleOrderMapper;
import com.kangcode.Service.SaleOrderService;
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
class SaleOrderServiceImplTest {

    @Mock
    private SaleOrderMapper saleOrderMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private SaleOrderServiceImpl saleOrderService;

    private Good good;
    private Customer customer;
    private SaleOrder order;

    @BeforeEach
    void setUp() {
        good = new Good();
        good.setId(1);
        good.setGoodsName("测试商品");
        good.setSellPrice(200.0);
        good.setStockNum(50);

        customer = new Customer();
        customer.setId(1);
        customer.setCustName("测试客户");

        order = new SaleOrder();
        order.setGoodsId(1);
        order.setCustId(1);
        order.setSaleNum(10);
        order.setUserId(1);
    }

    @Test
    void addOrder_shouldCalculateSaleMoneyAndSetStatus() {
        when(goodsMapper.getById(1)).thenReturn(good);
        when(customerMapper.getById(1)).thenReturn(customer);

        saleOrderService.addOrder(order);

        assertEquals(2000.0, order.getSaleMoney());
        assertEquals(SaleOrderService.STATUS_NOT_OUT, order.getStatus());
        assertNotNull(order.getSaleTime());
        assertNotNull(order.getCreateTime());
        assertNotNull(order.getUpdateTime());
        verify(saleOrderMapper).addOrder(order);
    }

    @Test
    void addOrder_shouldThrowWhenGoodsIdNull() {
        order.setGoodsId(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.addOrder(order));
        assertEquals("商品ID不能为空", e.getMessage());
        verifyNoInteractions(goodsMapper, customerMapper, saleOrderMapper);
    }

    @Test
    void addOrder_shouldThrowWhenCustIdNull() {
        order.setCustId(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.addOrder(order));
        assertEquals("客户ID不能为空", e.getMessage());
        verifyNoInteractions(goodsMapper, customerMapper, saleOrderMapper);
    }

    @Test
    void addOrder_shouldThrowWhenSaleNumNull() {
        order.setSaleNum(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.addOrder(order));
        assertEquals("销售数量不能为空", e.getMessage());
    }

    @Test
    void addOrder_shouldThrowWhenSaleNumZero() {
        order.setSaleNum(0);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.addOrder(order));
        assertEquals("销售数量必须大于0", e.getMessage());
    }

    @Test
    void addOrder_shouldThrowWhenGoodsNotFound() {
        when(goodsMapper.getById(1)).thenReturn(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.addOrder(order));
        assertEquals("商品不存在", e.getMessage());
        verify(saleOrderMapper, never()).addOrder(any());
    }

    @Test
    void addOrder_shouldThrowWhenStockInsufficient() {
        good.setStockNum(5);
        when(goodsMapper.getById(1)).thenReturn(good);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.addOrder(order));
        assertEquals("商品库存不足,无法创建订单", e.getMessage());
        verify(saleOrderMapper, never()).addOrder(any());
    }

    @Test
    void addOrder_shouldThrowWhenCustomerNotFound() {
        when(goodsMapper.getById(1)).thenReturn(good);
        when(customerMapper.getById(1)).thenReturn(null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.addOrder(order));
        assertEquals("客户不存在", e.getMessage());
        verify(saleOrderMapper, never()).addOrder(any());
    }

    @Test
    void updateOrder_shouldRecalculateWhenGoodsOrNumChanged() {
        SaleOrder oldOrder = new SaleOrder();
        oldOrder.setId(1);
        oldOrder.setGoodsId(1);
        oldOrder.setSaleNum(5);
        oldOrder.setStatus(SaleOrderService.STATUS_NOT_OUT);

        SaleOrder update = new SaleOrder();
        update.setId(1);
        update.setGoodsId(1);
        update.setSaleNum(20);
        update.setCustId(1);

        when(saleOrderMapper.getById(1)).thenReturn(oldOrder);
        when(goodsMapper.getById(1)).thenReturn(good);
        good.setStockNum(100);

        saleOrderService.updateOrder(update);

        assertEquals(4000.0, update.getSaleMoney());
        assertNotNull(update.getUpdateTime());
        verify(saleOrderMapper).updateById(update);
    }

    @Test
    void updateOrder_shouldThrowWhenOrderNotFound() {
        when(saleOrderMapper.getById(99)).thenReturn(null);
        SaleOrder update = new SaleOrder();
        update.setId(99);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.updateOrder(update));
        assertEquals("订单不存在", e.getMessage());
    }

    @Test
    void updateOrder_shouldThrowWhenAlreadyOutStock() {
        SaleOrder oldOrder = new SaleOrder();
        oldOrder.setId(1);
        oldOrder.setStatus(SaleOrderService.STATUS_OUTED);
        when(saleOrderMapper.getById(1)).thenReturn(oldOrder);

        SaleOrder update = new SaleOrder();
        update.setId(1);
        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.updateOrder(update));
        assertEquals("订单已出库，不能修改", e.getMessage());
    }

    @Test
    void getById_shouldReturnOrder() {
        SaleOrder expected = new SaleOrder();
        expected.setId(1);
        when(saleOrderMapper.getById(1)).thenReturn(expected);
        assertSame(expected, saleOrderService.getById(1));
        verify(saleOrderMapper).getById(1);
    }

    @Test
    void deleteById_shouldSucceedWhenNotOutStock() {
        when(saleOrderMapper.getById(1)).thenReturn(order);
        order.setStatus(SaleOrderService.STATUS_NOT_OUT);

        saleOrderService.deleteById(1);

        verify(saleOrderMapper).deleteById(1);
    }

    @Test
    void deleteById_shouldThrowWhenAlreadyOutStock() {
        when(saleOrderMapper.getById(1)).thenReturn(order);
        order.setStatus(SaleOrderService.STATUS_OUTED);

        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.deleteById(1));
        assertEquals("只有未出库可删除", e.getMessage());
        verify(saleOrderMapper, never()).deleteById(any());
    }

    @Test
    void outStock_shouldDecreaseStockAndUpdateStatus() {
        when(saleOrderMapper.getById(1)).thenReturn(order);
        order.setStatus(SaleOrderService.STATUS_NOT_OUT);
        order.setSaleNum(10);
        order.setGoodsId(1);
        when(goodsMapper.getById(1)).thenReturn(good);

        saleOrderService.outStock(1);

        assertEquals(40, good.getStockNum());
        assertEquals(SaleOrderService.STATUS_OUTED, order.getStatus());
        verify(goodsMapper).update(good);
        verify(saleOrderMapper).updateById(order);
    }

    @Test
    void outStock_shouldThrowWhenStockInsufficient() {
        when(saleOrderMapper.getById(1)).thenReturn(order);
        order.setStatus(SaleOrderService.STATUS_NOT_OUT);
        order.setSaleNum(100);
        order.setGoodsId(1);
        when(goodsMapper.getById(1)).thenReturn(good);

        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.outStock(1));
        assertEquals("库存不足", e.getMessage());
        verify(goodsMapper, never()).update(any());
    }

    @Test
    void outStock_shouldThrowWhenAlreadyOutStock() {
        when(saleOrderMapper.getById(1)).thenReturn(order);
        order.setStatus(SaleOrderService.STATUS_OUTED);

        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.outStock(1));
        assertEquals("只有未出库可出库", e.getMessage());
    }

    @Test
    void cancel_shouldSetCanceledStatus() {
        when(saleOrderMapper.getById(1)).thenReturn(order);
        order.setStatus(SaleOrderService.STATUS_NOT_OUT);

        saleOrderService.cancel(1);

        assertEquals(SaleOrderService.STATUS_CANCELED, order.getStatus());
        assertNotNull(order.getUpdateTime());
        verify(saleOrderMapper).updateById(order);
    }

    @Test
    void cancel_shouldThrowWhenAlreadyOutStock() {
        when(saleOrderMapper.getById(1)).thenReturn(order);
        order.setStatus(SaleOrderService.STATUS_OUTED);

        RuntimeException e = assertThrows(RuntimeException.class, () -> saleOrderService.cancel(1));
        assertEquals("只有未出库可取消", e.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void page_shouldReturnPageResult() {
        SaleOrderQueryParam param = new SaleOrderQueryParam();
        param.setPage(1);
        param.setPageSize(10);
        SaleOrder o1 = new SaleOrder();
        o1.setId(1);
        SaleOrder o2 = new SaleOrder();
        o2.setId(2);
        Page<SaleOrder> mockPage = mock(Page.class);
        when(mockPage.getTotal()).thenReturn(2L);
        when(saleOrderMapper.list(param)).thenReturn(mockPage);

        try (MockedStatic<PageHelper> pageHelper = mockStatic(PageHelper.class)) {
            PageResult result = saleOrderService.page(param);
            pageHelper.verify(() -> PageHelper.startPage(1, 10));
            assertEquals(2L, result.getTotal());
        }
    }
}
