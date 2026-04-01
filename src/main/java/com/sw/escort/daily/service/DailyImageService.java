package com.sw.escort.daily.service;

import com.sw.escort.daily.dto.req.DailyDtoReq;
import com.sw.escort.daily.dto.res.DailyDtoRes;

import java.time.LocalDate;
import java.util.List;

public interface DailyImageService {
    List<DailyDtoRes.DailyImageUploadRes> generateAiImages(Long userId, Long dailyId, LocalDate date, List<String> imageUrls);
}
