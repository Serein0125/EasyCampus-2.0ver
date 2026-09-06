package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.AdCreateDTO;
import com.campus.backend.dto.AdPackageVO;
import com.campus.backend.dto.PostVO;
import com.campus.backend.service.AdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v2/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;

    @PostMapping
    public Result<PostVO> createAd(@Valid @RequestBody AdCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        PostVO ad = adService.createAd(dto, userId);
        return Result.success(ad);
    }

    @GetMapping("/packages")
    public Result<List<AdPackageVO>> getAdPackages() {
        return Result.success(adService.getAdPackages());
    }

    @PostMapping("/{postId}/pay")
    public Result<Map<String, Object>> simulatePayment(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String packageId = body.get("packageId");
        boolean success = adService.simulatePayment(userId, postId, packageId);
        return Result.success(Map.of("success", success, "postId", postId, "packageId", packageId != null ? packageId : ""));
    }
}
