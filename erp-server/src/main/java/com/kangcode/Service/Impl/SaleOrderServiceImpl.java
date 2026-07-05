package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.CustomerMapper;
import com.kangcode.Mapper.GoodsMapper;
import com.kangcode.Mapper.SaleOrderMapper;
import com.kangcode.Service.SaleOrderService;
import com.kangcode.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class SaleOrderServiceImpl implements SaleOrderService {
    @Autowired
    private SaleOrderMapper saleOrderMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private CustomerMapper customerMapper;


    @Override
    public List<SaleOrder> listAll(Integer status) {
        return saleOrderMapper.listByStatus(status);
    }

    @Override
    public void addOrder(SaleOrder saleOrder) {
        //1.基础校验
        if(saleOrder.getGoodsId()==null){
            throw new RuntimeException("商品ID不能为空");
        }
        if(saleOrder.getCustId()==null){
            throw new RuntimeException("客户ID不能为空");
        }
        if(saleOrder.getSaleNum()==null){
            throw new RuntimeException("销售数量不能为空");
        }
        if(saleOrder.getSaleNum()<=0){
            throw new RuntimeException("销售数量必须大于0");
        }

        //2.查询商品，校验库存
        Good good=goodsMapper.getById(saleOrder.getGoodsId());
        if(good==null){
            throw new RuntimeException("商品不存在");
        }
        if (good.getStockNum() == null || good.getStockNum() < saleOrder.getSaleNum()) {
            throw new RuntimeException("商品库存不足,无法创建订单");
        }

        //3.查询客户
        Customer customer=customerMapper.getById(saleOrder.getCustId());
        if(customer==null){
            throw new RuntimeException("客户不存在");
        }

        //4. 确定销售单价：优先用订单自带价格，无则用商品默认价
        Double salePrice = saleOrder.getSalePrice() != null ? saleOrder.getSalePrice() : good.getSellPrice();
        if (salePrice == null) {
            throw new RuntimeException("销售单价不能为空（商品未设置默认价，请在订单中填写单价）");
        }
        saleOrder.setSalePrice(salePrice);

        //5.自动计算销售金额：单价*数量
        Double saleMoney = salePrice * saleOrder.getSaleNum();
        saleOrder.setSaleMoney(saleMoney);

        //5.设置状态和时间（前端月份选择器可能只传 yyyy-MM，已由反序列化器解析）
        saleOrder.setStatus(STATUS_NOT_OUT);//将状态设置为未出库
        if (saleOrder.getSaleTime() == null) {
            saleOrder.setSaleTime(LocalDateTime.now());
        }
        saleOrder.setCreateTime(LocalDateTime.now());
        saleOrder.setUpdateTime(LocalDateTime.now());

        //6.创建订单
        saleOrderMapper.addOrder(saleOrder);




    }
    //修改订单： 未出库才能改  同时应该重新计算金额，更新时间
    @Override
    public void updateOrder(SaleOrder saleOrder) {
        SaleOrder oldOrder=saleOrderMapper.getById(saleOrder.getId());
        if(oldOrder==null){
            throw new RuntimeException("订单不存在");
        }
        if(oldOrder.getStatus()!=STATUS_NOT_OUT){
            throw new RuntimeException("订单已出库，不能修改");
        }

        //修改了商品/数量/价格，同时要校验库存和计算金额
        if(!oldOrder.getGoodsId().equals(saleOrder.getGoodsId())
                ||!oldOrder.getSaleNum().equals(saleOrder.getSaleNum())
                ||!java.util.Objects.equals(oldOrder.getSalePrice(), saleOrder.getSalePrice())){
            Good good=goodsMapper.getById(saleOrder.getGoodsId());
            if (good == null) throw new RuntimeException("商品不存在");
            int stock = good.getStockNum() == null ? 0 : good.getStockNum();
            if (stock < saleOrder.getSaleNum()) throw new RuntimeException("商品库存不足");

            // 确定销售单价：优先用订单自带价格，无则用商品默认价
            Double salePrice = saleOrder.getSalePrice() != null ? saleOrder.getSalePrice() : good.getSellPrice();
            if (salePrice == null) throw new RuntimeException("销售单价不能为空");
            saleOrder.setSalePrice(salePrice);

            //计算金额
            Double saleMoney = salePrice * saleOrder.getSaleNum();
            saleOrder.setSaleMoney(saleMoney);
        }
        //修改订单
        saleOrder.setUpdateTime(LocalDateTime.now());
        saleOrderMapper.updateById(saleOrder);
    }

    @Override
    public SaleOrder getById(Integer id) {

        return saleOrderMapper.getById(id);
    }

    @Override
    public void deleteById(Integer id) {
        SaleOrder order = saleOrderMapper.getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != STATUS_NOT_OUT) {
            throw new RuntimeException("只有未出库可删除");
        }
        saleOrderMapper.deleteById(id);
    }

    @Override
    public void outStock(Integer id) {
        SaleOrder order = saleOrderMapper.getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != STATUS_NOT_OUT) throw new RuntimeException("只有未出库可出库");

        Good good = goodsMapper.getById(order.getGoodsId());
        if (good == null) {
            throw new RuntimeException("商品不存在");
        }
        int currentStock = good.getStockNum() == null ? 0 : good.getStockNum();
        if (currentStock < order.getSaleNum()) {
            throw new RuntimeException("库存不足");
        }

        // 扣库存
        good.setStockNum(currentStock - order.getSaleNum());
        goodsMapper.update(good);

        order.setStatus(STATUS_OUTED);
        order.setSaleTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        saleOrderMapper.updateById(order);
    }

    @Override
    public void cancel(Integer id) {
        SaleOrder order = saleOrderMapper.getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != STATUS_NOT_OUT) throw new RuntimeException("只有未出库可取消");

        order.setStatus(STATUS_CANCELED);
        order.setUpdateTime(LocalDateTime.now());
        saleOrderMapper.updateById(order);
    }

    @Override
    public PageResult page(SaleOrderQueryParam saleOrderQueryParam) {
        //1.设置分页参数
        PageHelper.startPage(saleOrderQueryParam.getPage(), saleOrderQueryParam.getPageSize());
        //2.执行查询
        List<SaleOrder> list = saleOrderMapper.list(saleOrderQueryParam);
        //3.解析查询结合，并封装
        Page<SaleOrder> p = (Page<SaleOrder>) list;
        PageResult<SaleOrder> pageResult = new PageResult<>(p.getTotal(), list);
        return pageResult;
    }

}
