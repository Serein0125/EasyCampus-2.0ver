package com.campus.backend.service.impl;

import com.campus.backend.dto.SearchQueryDTO;
import com.campus.backend.dto.SearchResultVO;
import com.campus.backend.entity.User;
import com.campus.backend.mapper.PostMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final PostMapper postMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public SearchResultVO search(SearchQueryDTO query) {
        String keyword = query.getKeyword();
        if (keyword == null || keyword.isBlank()) {
            return emptyResult(query);
        }

        keyword = keyword.trim();
        int offset = query.getOffset();
        int size = query.getSize();
        String type = query.getType();

        SearchResultVO result = new SearchResultVO();
        result.setKeyword(keyword);
        result.setPage(query.getPage());
        result.setSize(size);

        if (type == null || type.isEmpty() || "all".equalsIgnoreCase(type)) {
            int halfSize = Math.max(size / 2, 5);
            result.setPosts(searchPosts(keyword, offset, halfSize));
            result.setPostTotal(postMapper.searchPostCount(keyword));
            result.setProducts(searchProducts(keyword, offset, halfSize));
            result.setProductTotal(productMapper.searchProductCount(keyword));
            result.setUsers(searchUsers(keyword, 0, 5));
            result.setUserTotal(userMapper.searchUserCount(keyword));
        } else {
            switch (type.toLowerCase()) {
                case "post" -> {
                    result.setPosts(searchPosts(keyword, offset, size));
                    result.setPostTotal(postMapper.searchPostCount(keyword));
                    result.setProducts(new ArrayList<>());
                    result.setProductTotal(0);
                    result.setUsers(new ArrayList<>());
                    result.setUserTotal(0);
                }
                case "product" -> {
                    result.setPosts(new ArrayList<>());
                    result.setPostTotal(0);
                    result.setProducts(searchProducts(keyword, offset, size));
                    result.setProductTotal(productMapper.searchProductCount(keyword));
                    result.setUsers(new ArrayList<>());
                    result.setUserTotal(0);
                }
                case "user" -> {
                    result.setPosts(new ArrayList<>());
                    result.setPostTotal(0);
                    result.setProducts(new ArrayList<>());
                    result.setProductTotal(0);
                    result.setUsers(searchUsers(keyword, offset, size));
                    result.setUserTotal(userMapper.searchUserCount(keyword));
                }
                default -> {
                    int halfSize = Math.max(size / 2, 5);
                    result.setPosts(searchPosts(keyword, offset, halfSize));
                    result.setPostTotal(postMapper.searchPostCount(keyword));
                    result.setProducts(searchProducts(keyword, offset, halfSize));
                    result.setProductTotal(productMapper.searchProductCount(keyword));
                    result.setUsers(searchUsers(keyword, 0, 5));
                    result.setUserTotal(userMapper.searchUserCount(keyword));
                }
            }
        }

        // 排序
        sortResults(result, query.getSortBy());

        return result;
    }

    private void sortResults(SearchResultVO result, String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return;
        switch (sortBy) {
            case "price_asc" -> result.getProducts().sort(
                (a, b) -> comparePrice(a.getPrice(), b.getPrice()));
            case "price_desc" -> result.getProducts().sort(
                (a, b) -> comparePrice(b.getPrice(), a.getPrice()));
            case "time_desc" -> {
                result.getPosts().sort((a, b) -> compareTime(b.getCreatedAt(), a.getCreatedAt()));
                result.getProducts().sort((a, b) -> compareTime(b.getCreatedAt(), a.getCreatedAt()));
            }
            case "hot" -> result.getPosts().sort((a, b) -> {
                int scoreA = (a.getLikeCount() != null ? a.getLikeCount() : 0) * 2
                           + (a.getCommentCount() != null ? a.getCommentCount() : 0) * 3
                           + (a.getViewCount() != null ? a.getViewCount() : 0);
                int scoreB = (b.getLikeCount() != null ? b.getLikeCount() : 0) * 2
                           + (b.getCommentCount() != null ? b.getCommentCount() : 0) * 3
                           + (b.getViewCount() != null ? b.getViewCount() : 0);
                return Integer.compare(scoreB, scoreA);
            });
        }
    }

    private int comparePrice(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }

    private int compareTime(java.time.LocalDateTime a, java.time.LocalDateTime b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }

    private List<SearchResultVO.PostItem> searchPosts(String keyword, int offset, int limit) {
        List<Map<String, Object>> rows = postMapper.searchPosts(keyword, offset, limit);
        List<SearchResultVO.PostItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            SearchResultVO.PostItem item = new SearchResultVO.PostItem();
            item.setId(toLong(row.get("id")));
            item.setUserId(toLong(row.get("user_id")));
            item.setTitle(toStr(row.get("title")));
            item.setContent(truncate(toStr(row.get("content")), 200));
            item.setPostType(toStr(row.get("post_type")));
            item.setPostTypeText(getPostTypeText(toStr(row.get("post_type"))));
            item.setViewCount(toInt(row.get("view_count")));
            item.setLikeCount(toInt(row.get("like_count")));
            item.setCommentCount(toInt(row.get("comment_count")));
            item.setTags(toStr(row.get("tags")));
            item.setCreatedAt(toLocalDateTime(row.get("created_at")));

            String userName = toStr(row.get("user_name"));
            String userUsername = toStr(row.get("user_username"));
            item.setUserName(userName != null && !userName.isEmpty() ? userName : userUsername);
            item.setUserAvatar(toStr(row.get("user_avatar")));

            item.setMatchedField(determinePostMatchField(keyword, toStr(row.get("title")), toStr(row.get("content")), userName, userUsername));
            items.add(item);
        }
        return items;
    }

    private List<SearchResultVO.ProductItem> searchProducts(String keyword, int offset, int limit) {
        List<Map<String, Object>> rows = productMapper.searchProducts(keyword, offset, limit);
        List<SearchResultVO.ProductItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            SearchResultVO.ProductItem item = new SearchResultVO.ProductItem();
            item.setId(toLong(row.get("id")));
            item.setName(toStr(row.get("name")));
            item.setDescription(truncate(toStr(row.get("description")), 200));
            item.setPrice(toBigDecimal(row.get("price")));
            item.setOriginalPrice(toBigDecimal(row.get("original_price")));
            item.setCoverImage(toStr(row.get("cover_image")));
            item.setImageUrls(parseImageUrls(toStr(row.get("image_urls"))));
            item.setSellerId(toLong(row.get("seller_id")));
            item.setConditionLevel(toInteger(row.get("condition_level")));
            item.setConditionText(getConditionText(toInteger(row.get("condition_level"))));
            item.setStatus(toInteger(row.get("status")));
            item.setStatusText(getStatusText(toInteger(row.get("status"))));
            item.setViewCount(toInt(row.get("view_count")));
            item.setLikeCount(toInt(row.get("like_count")));
            item.setLocation(toStr(row.get("location")));
            item.setCreatedAt(toLocalDateTime(row.get("created_at")));

            String sellerName = toStr(row.get("seller_name"));
            String sellerUsername = toStr(row.get("seller_username"));
            item.setSellerName(sellerName != null && !sellerName.isEmpty() ? sellerName : sellerUsername);
            item.setSellerAvatar(toStr(row.get("seller_avatar")));

            item.setMatchedField(determineProductMatchField(keyword, toStr(row.get("name")), toStr(row.get("description")), sellerName, sellerUsername));
            items.add(item);
        }
        return items;
    }

    private List<SearchResultVO.UserItem> searchUsers(String keyword, int offset, int limit) {
        List<User> users = userMapper.searchUsers(keyword, offset, limit);
        List<SearchResultVO.UserItem> items = new ArrayList<>();
        for (User user : users) {
            SearchResultVO.UserItem item = new SearchResultVO.UserItem();
            item.setId(user.getId());
            item.setUsername(user.getUsername());
            item.setNickname(user.getNickname());
            item.setAvatar(user.getAvatar());
            item.setSchool(user.getSchool());
            item.setMajor(user.getMajor());
            item.setBio(truncate(user.getBio(), 100));

            String matchedField;
            if (user.getNickname() != null && user.getNickname().toLowerCase().contains(keyword.toLowerCase())) {
                matchedField = "nickname";
            } else {
                matchedField = "username";
            }
            item.setMatchedField(matchedField);
            items.add(item);
        }
        return items;
    }

    private String determinePostMatchField(String keyword, String title, String content, String userName, String userUsername) {
        String lower = keyword.toLowerCase();
        if (title != null && title.toLowerCase().contains(lower)) return "title";
        if (content != null && content.toLowerCase().contains(lower)) return "content";
        if (userName != null && userName.toLowerCase().contains(lower)) return "userName";
        if (userUsername != null && userUsername.toLowerCase().contains(lower)) return "userName";
        return "title";
    }

    private String determineProductMatchField(String keyword, String name, String description, String sellerName, String sellerUsername) {
        String lower = keyword.toLowerCase();
        if (name != null && name.toLowerCase().contains(lower)) return "name";
        if (description != null && description.toLowerCase().contains(lower)) return "description";
        if (sellerName != null && sellerName.toLowerCase().contains(lower)) return "sellerName";
        if (sellerUsername != null && sellerUsername.toLowerCase().contains(lower)) return "sellerName";
        return "name";
    }

    private SearchResultVO emptyResult(SearchQueryDTO query) {
        SearchResultVO result = new SearchResultVO();
        result.setKeyword(query.getKeyword());
        result.setPage(query.getPage());
        result.setSize(query.getSize());
        result.setPosts(new ArrayList<>());
        result.setPostTotal(0);
        result.setProducts(new ArrayList<>());
        result.setProductTotal(0);
        result.setUsers(new ArrayList<>());
        result.setUserTotal(0);
        return result;
    }

    private List<String> parseImageUrls(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return null; }
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 0; }
    }

    private String toStr(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        try { return new BigDecimal(obj.toString()); } catch (Exception e) { return null; }
    }

    private LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDateTime) return (LocalDateTime) obj;
        if (obj instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    private String getPostTypeText(String postType) {
        if (postType == null) return "讨论";
        return switch (postType) {
            case "DISCUSSION" -> "讨论";
            case "SHOWCASE" -> "展示";
            case "HELP" -> "求助";
            case "ACTIVITY" -> "活动";
            default -> postType;
        };
    }

    private String getConditionText(Integer level) {
        if (level == null) return "未设置";
        return switch (level) {
            case 1 -> "全新";
            case 2 -> "几乎全新";
            case 3 -> "轻微使用痕迹";
            case 4 -> "明显使用痕迹";
            case 5 -> "一般";
            default -> "未设置";
        };
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "已下架";
            case 1 -> "在售";
            case 2 -> "已售出";
            case 3 -> "预约中";
            default -> "未知";
        };
    }
}
