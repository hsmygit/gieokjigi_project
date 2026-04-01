package com.sw.escort.daily.controller;

import com.sw.escort.apiPayload.ApiResponse;
import com.sw.escort.common.security.JwtTokenProvider;
import com.sw.escort.daily.dto.req.DailyDtoReq;
import com.sw.escort.daily.dto.res.DailyDtoRes;
import com.sw.escort.daily.service.DailyImageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/record")
public class DailyImageController {

    private final DailyImageService dailyImageService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "AI 이미지 생성 (사용자 입력 이미지 + 회상 날짜 기반)")
    @PostMapping("/generate-ai-images")
    public ApiResponse<List<DailyDtoRes.DailyImageUploadRes>> generateDailyImages(
            @RequestBody DailyDtoReq.DailyImageGenerationReq req) {

        Long userId = jwtTokenProvider.getUserIdFromToken();

        return ApiResponse.onSuccess(
                dailyImageService.generateAiImages(userId, req.getDailyId(), req.getDate(), req.getImageUrls())
        );
    }
}
