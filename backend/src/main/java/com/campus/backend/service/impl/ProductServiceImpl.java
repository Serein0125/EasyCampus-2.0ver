package com.campus.backend.service.impl;

import com.campus.backend.dto.*;
import com.campus.backend.entity.Product;
import com.campus.backend.common.ErrorCode;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.CategoryMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.CategoryService;
import com.campus.backend.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务实现 - 闲鱼风格
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    @Override
    public List<ProductVO> getProductList(ProductQueryDTO query) {
        List<Product> products = productMapper.selectProductList(query);
        return products.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public int getProductCount(ProductQueryDTO query) {
        return productMapper.selectProductCount(query);
    }

    @Override
    @Transactional
    public ProductVO getProductDetail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new NotFoundException("商品", productId);
        }
        if (product.getStatus() != null && product.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE, "该商品已下架");
        }
        productMapper.incrementViewCount(productId);
        return convertToVO(product);
    }

    @Override
    @Transactional
    public ProductVO createProduct(ProductCreateDTO dto, Long sellerId) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setSellerId(sellerId);
        product.setStatus(1); // 在售

        // 处理图片列表 -> JSON字符串
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            try {
                product.setImageUrls(objectMapper.writeValueAsString(dto.getImageUrls()));
            } catch (JsonProcessingException e) {
                log.warn("图片URL序列化失败", e);
            }
        }
        // 封面图默认取第一张
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty() && product.getCoverImage() == null) {
            product.setCoverImage(dto.getImageUrls().get(0));
        }

        // 处理商品故事
        if (dto.getStoryTitle() != null || dto.getStoryContent() != null) {
            product.setStoryTitle(dto.getStoryTitle());
            product.setStoryContent(dto.getStoryContent());
            product.setHasStory(dto.getStoryContent() != null && !dto.getStoryContent().isBlank());
        }
        if (dto.getStoryImages() != null && !dto.getStoryImages().isEmpty()) {
            try {
                product.setStoryImages(objectMapper.writeValueAsString(dto.getStoryImages()));
            } catch (JsonProcessingException e) {
                log.warn("故事配图URL序列化失败", e);
            }
        }
        // 销售模式
        if (dto.getSaleMode() != null) {
            product.setSaleMode(dto.getSaleMode());
        }

        productMapper.insert(product);
        log.info("商品发布成功: id={}, name={}, sellerId={}", product.getId(), product.getName(), sellerId);
        return convertToVO(product);
    }

    @Override
    @Transactional
    public ProductVO updateProduct(Long productId, ProductUpdateDTO dto, Long sellerId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new NotFoundException("商品", productId);
        }
        checkOwnership(product, sellerId);

        BeanUtils.copyProperties(dto, product);

        // 处理图片列表
        if (dto.getImageUrls() != null) {
            try {
                product.setImageUrls(objectMapper.writeValueAsString(dto.getImageUrls()));
            } catch (JsonProcessingException e) {
                log.warn("图片URL序列化失败", e);
            }
        }

        // 处理故事图片
        if (dto.getStoryImages() != null) {
            try {
                product.setStoryImages(objectMapper.writeValueAsString(dto.getStoryImages()));
            } catch (JsonProcessingException e) {
                log.warn("故事配图URL序列化失败", e);
            }
        }
        // 更新has_story标记
        if (dto.getStoryContent() != null) {
            product.setHasStory(!dto.getStoryContent().isBlank());
        }

        productMapper.update(product);
        return convertToVO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new NotFoundException("商品", productId);
        }
        checkOwnership(product, sellerId);
        productMapper.deleteById(productId);
    }

    @Override
    @Transactional
    public void toggleProductStatus(Long productId, Long sellerId, Integer status) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new NotFoundException("商品", productId);
        }
        checkOwnership(product, sellerId);
        productMapper.updateStatus(productId, status);
    }

    @Override
    public List<ProductVO> getMyProducts(Long sellerId) {
        List<Product> products = productMapper.selectBySellerId(sellerId);
        return products.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /** 校验所有权 */
    private void checkOwnership(Product product, Long sellerId) {
        if (!product.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.PRODUCT_OWNERSHIP_ERROR, "无权操作此商品");
        }
    }

    /** Entity -> VO */
    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        // 分类名称
        try {
            if (product.getCategoryId() != null) {
                var cat = categoryMapper.findById(product.getCategoryId());
                if (cat != null) vo.setCategoryName(cat.getName());
            }
        } catch (Exception ignored) {}

        // 卖家信息
        try {
            if (product.getSellerId() != null) {
                var seller = userMapper.selectById(product.getSellerId());
                if (seller != null) {
                    vo.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());
                    vo.setSellerAvatar(seller.getAvatar());
                    vo.setSellerSchool(seller.getSchool());
                }
            }
        } catch (Exception ignored) {}

        // 商品图片JSON -> List
        if (product.getImageUrls() != null && !product.getImageUrls().isBlank()) {
            try {
                vo.setImageUrls(objectMapper.readValue(product.getImageUrls(),
                        new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException e) {
                log.warn("商品图片URL反序列化失败", e);
            }
        }

        // 故事配图JSON -> List
        if (product.getStoryImages() != null && !product.getStoryImages().isBlank()) {
            try {
                vo.setStoryImages(objectMapper.readValue(product.getStoryImages(),
                        new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException e) {
                log.warn("故事配图URL反序列化失败", e);
            }
        }

        return vo;
    }
}
