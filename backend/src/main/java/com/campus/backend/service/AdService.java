package com.campus.backend.service;

import com.campus.backend.dto.AdCreateDTO;
import com.campus.backend.dto.AdPackageVO;
import com.campus.backend.dto.PostVO;
import java.util.List;

public interface AdService {

    PostVO createAd(AdCreateDTO dto, Long userId);

    List<AdPackageVO> getAdPackages();

    AdPackageVO getAdPackage(String packageId);

    boolean simulatePayment(Long userId, Long postId, String packageId);
}
