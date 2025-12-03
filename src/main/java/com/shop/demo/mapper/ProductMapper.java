package com.shop.demo.mapper;

import com.shop.demo.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductMapper {
    List<Product> selectAll();
    Product selectById(Long id);
    // 新增库存更新方法
    int updateStock(@Param("productId") Long productId, @Param("newStock") Integer newStock);
}