package com.campus.backend.service;

import com.campus.backend.dto.ProductCreateDTO;
import com.campus.backend.dto.ProductQueryDTO;
import com.campus.backend.dto.ProductUpdateDTO;
import com.campus.backend.dto.ProductVO;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 查询商品列表 (分页)
     */
    List<ProductVO> getProductList(ProductQueryDTO query);

    /**
     * 查询商品总数
     */
    int getProductCount(ProductQueryDTO query);

    /**
     * 商品详情 (同时增加浏览量)
     */
    ProductVO getProductDetail(Long productId);

    /**
     * 发布商品
     */
    ProductVO createProduct(ProductCreateDTO createDTO, Long sellerId);

    /**
     * 编辑商品
     */
    ProductVO updateProduct(Long productId, ProductUpdateDTO updateDTO, Long sellerId);

    /**
     * 删除商品 (软删除-下架)
     */
    void deleteProduct(Long productId, Long sellerId);

    /**
     * 上架/下架商品
     */
    void toggleProductStatus(Long productId, Long sellerId, Integer status);

    /**
     * 获取我的商品列表
     */
    List<ProductVO> getMyProducts(Long sellerId);
}
