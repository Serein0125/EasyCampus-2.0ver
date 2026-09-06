package com.campus.backend.service;

import com.campus.backend.dto.SearchQueryDTO;
import com.campus.backend.dto.SearchResultVO;

public interface SearchService {

    SearchResultVO search(SearchQueryDTO query);
}
