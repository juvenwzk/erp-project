package com.kangcode.Controller;

import com.kangcode.Service.GoodsTypeService;
import com.kangcode.pojo.GoodsType;
import com.kangcode.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/goods-type")
public class GoodsTypeController {
    @Autowired
    private GoodsTypeService goodsTypeService;
    @PostMapping
    public Result add(@RequestBody GoodsType goodsType){
         log.info("添加商品类型:{}",goodsType);
         goodsTypeService.add(goodsType);
        return Result.success();
    }

    @GetMapping("/list")
    public Result  list(){
        log.info("查询所有商品类型");
        List<GoodsType> list=goodsTypeService.list();
        return Result.success(list);
    }
    @PutMapping
    public Result update(@RequestBody GoodsType goodsType){
        log.info("更新商品类型:  {}", goodsType);
        goodsTypeService.update(goodsType);
        return Result.success();
    }

    @DeleteMapping("{id}")
    public Result delete(@PathVariable Integer id){
        log.info("删除商品类型: {}", id);
        goodsTypeService.deleteById(id);
        return Result.success();
    }

}
