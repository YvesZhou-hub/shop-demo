package com.shop.demo.mapper;

import com.shop.demo.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductMapper {
    List<Product> selectAll();
    Product selectById(Long id);
    // 新增带悲观锁的查询方法（用于并发控制）
    Product selectByIdForUpdate(@Param("id") Long id);
    // 调整库存更新方法参数（增加购买数量用于条件判断）
    int updateStock(
            @Param("productId") Long productId,
            @Param("newStock") Integer newStock,
            @Param("buyNum") Integer buyNum
    );

    int insertProduct(Product product);
}