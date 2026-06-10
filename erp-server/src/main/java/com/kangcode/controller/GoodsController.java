package com.kangcode.Controller;

import com.kangcode.Service.GoodsService;
import com.kangcode.anno.Log;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.GoodsQueryParam;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/goods")
public class GoodsController {
    @Autowired
    private GoodsService goodsService;

    @Log
    @PostMapping
    public Result add(@RequestBody Good  good){
        log.info("添加商品:{}",good);
        // 新商品库存默认为0，只能通过采购入库增加
        good.setStockNum(0);
        goodsService.add(good);
        return Result.success();
    }

    @Log
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id){
        log.info("根据Id查询商品: {}",id);
        Good good=goodsService.getById(id);
        return Result.success(good);

    }
    @Log
    @GetMapping("/list")
    public Result list(GoodsQueryParam goodsQueryParam){
        log.info("查询所有商品：{}",goodsQueryParam);
        PageResult PageResult=goodsService.page(goodsQueryParam);
        return Result.success(PageResult);
    }
    @Log
    @DeleteMapping
    public Result delete(@RequestBody List<Integer> ids){
        log.info("删除商品:{}", ids);
        goodsService.delete(ids);
        return Result.success();
    }
    @Log
    @PutMapping
    public Result update(@RequestBody Good good){
        log.info("更新商品:{}",good);
        // 库存数量不能直接修改，只能通过采购入库/销售出库变动
        Good existing = goodsService.getById(good.getId());
        if (existing != null) {
            good.setStockNum(existing.getStockNum());
        }
        goodsService.update(good);
        return Result.success();
    }


@Log
    @GetMapping("/low-stock")
    public Result getLowStock(){
        log.info("查询库存低于10的");
        return Result.success(goodsService.getLowStock());
    }

}
