package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.GoodsMapper;
import com.kangcode.Mapper.PurchaseMapper;
import com.kangcode.Mapper.SupplierMapper;
import com.kangcode.Service.PurchaseService;
import com.kangcode.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class PurchaseServiceImpl implements PurchaseService {

    @Autowired
    private PurchaseMapper purchaseMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private SupplierMapper supplierMapper;

    @Override
    public List<PurchaseOrder> listAll(Integer status) {
        return purchaseMapper.listByStatus(status);
    }

    @Override
    public void addOrder(PurchaseOrder purchaseOrder) {
        // 1. 基础校验
        if (purchaseOrder.getGoodsId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (purchaseOrder.getSupplierId() == null) {
            throw new RuntimeException("供应商ID不能为空");
        }
        if (purchaseOrder.getBuyNum() == null) {
            throw new RuntimeException("采购数量不能为空");
        }
        if (purchaseOrder.getBuyNum() <= 0) {
            throw new RuntimeException("采购数量必须大于0");
        }

        // 2. 查询商品
        Good good = goodsMapper.getById(purchaseOrder.getGoodsId());
        if (good == null) {
            throw new RuntimeException("商品不存在");
        }

        // 3. 查询供应商
        Supplier supplier = supplierMapper.getById(purchaseOrder.getSupplierId());
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }

        // 4. 确定进货单价：优先用订单自带价格，无则用商品默认价
        Double buyPrice = purchaseOrder.getBuyPrice() != null ? purchaseOrder.getBuyPrice() : good.getBuyPrice();
        if (buyPrice == null) {
            throw new RuntimeException("进货单价不能为空（商品未设置默认价，请在订单中填写单价）");
        }
        purchaseOrder.setBuyPrice(buyPrice);

        // 5. 自动计算采购总金额：进货单价 * 数量
        Double totalMoney = buyPrice * purchaseOrder.getBuyNum();
        purchaseOrder.setTotalMoney(totalMoney);

        // 5. 设置状态和时间（前端月份选择器可能只传 yyyy-MM，已由反序列化器解析）
        purchaseOrder.setStatus(STATUS_NOT_IN);
        if (purchaseOrder.getBuyTime() == null) {
            purchaseOrder.setBuyTime(LocalDateTime.now());
        }
        purchaseOrder.setCreateTime(LocalDateTime.now());
        purchaseOrder.setUpdateTime(LocalDateTime.now());

        // 6. 创建订单
        purchaseMapper.addOrder(purchaseOrder);
    }

    @Override
    public void updateOrder(PurchaseOrder purchaseOrder) {
        PurchaseOrder oldOrder = purchaseMapper.getById(purchaseOrder.getId());
        if (oldOrder == null) {
            throw new RuntimeException("订单不存在");
        }
        if (oldOrder.getStatus() != STATUS_NOT_IN) {
            throw new RuntimeException("订单已入库，不能修改");
        }

        // 修改了商品/数量/价格，重新计算金额
        if (!oldOrder.getGoodsId().equals(purchaseOrder.getGoodsId())
                || !oldOrder.getBuyNum().equals(purchaseOrder.getBuyNum())
                || !java.util.Objects.equals(oldOrder.getBuyPrice(), purchaseOrder.getBuyPrice())) {
            Good good = goodsMapper.getById(purchaseOrder.getGoodsId());
            if (good == null) throw new RuntimeException("商品不存在");

            // 确定进货单价：优先用订单自带价格，无则用商品默认价
            Double buyPrice = purchaseOrder.getBuyPrice() != null ? purchaseOrder.getBuyPrice() : good.getBuyPrice();
            if (buyPrice == null) throw new RuntimeException("进货单价不能为空");
            purchaseOrder.setBuyPrice(buyPrice);

            // 重新计算金额
            Double totalMoney = buyPrice * purchaseOrder.getBuyNum();
            purchaseOrder.setTotalMoney(totalMoney);
        }

        purchaseOrder.setUpdateTime(LocalDateTime.now());
        purchaseMapper.updateById(purchaseOrder);
    }

    @Override
    public PurchaseOrder getById(Integer id) {
        return purchaseMapper.getById(id);
    }

    @Override
    public void deleteById(Integer id) {
        PurchaseOrder order = purchaseMapper.getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != STATUS_NOT_IN) {
            throw new RuntimeException("只有未入库可删除");
        }
        purchaseMapper.deleteById(id);
    }

    @Override
    public void inStock(Integer id) {
        PurchaseOrder order = purchaseMapper.getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != STATUS_NOT_IN) throw new RuntimeException("只有未入库可入库");

        Good good = goodsMapper.getById(order.getGoodsId());
        if (good == null) throw new RuntimeException("商品不存在");

        // 入库：增加库存
        int currentStock = good.getStockNum() == null ? 0 : good.getStockNum();
        good.setStockNum(currentStock + order.getBuyNum());
        goodsMapper.update(good);

        order.setStatus(STATUS_INED);
        order.setBuyTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        purchaseMapper.updateById(order);
    }

    @Override
    public void cancel(Integer id) {
        PurchaseOrder order = purchaseMapper.getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != STATUS_NOT_IN) throw new RuntimeException("只有未入库可取消");

        order.setStatus(STATUS_CANCELED);
        order.setUpdateTime(LocalDateTime.now());
        purchaseMapper.updateById(order);
    }

    @Override
    public PageResult page(PurchaseQueryParam param) {
        PageHelper.startPage(param.getPage(), param.getPageSize());
        List<PurchaseOrder> list = purchaseMapper.list(param);
        Page<PurchaseOrder> p = (Page<PurchaseOrder>) list;
        return new PageResult<>(p.getTotal(), list);
    }
}
