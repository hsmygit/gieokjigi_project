package com.sw.escort.daily.service;

import com.sw.escort.apiPayload.code.exception.GeneralException;
import com.sw.escort.apiPayload.code.status.ErrorStatus;
import com.sw.escort.common.client.PythonAiClient;
import com.sw.escort.daily.dto.res.DailyDtoRes;
import com.sw.escort.daily.entity.Daily;
import com.sw.escort.daily.entity.DailyImage;
import com.sw.escort.daily.repository.DailyImageRepository;
import com.sw.escort.daily.repository.DailyRepository;
import com.sw.escort.global.util.AmazonS3Util;
import com.sw.escort.user.entity.User;
import com.sw.escort.user.entity.UserInfo;
import com.sw.escort.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyImageServiceImpl implements DailyImageService {

    private final UserRepository userRepository;
    private final DailyRepository dailyRepository;
    private final DailyImageRepository dailyImageRepository;
    private final AmazonS3Util amazonS3Util;
    private final PythonAiClient pythonAiClient;

    /**
     * 안드로이드에서 dailyId, 날짜, 이미지 URL 리스트를 받아 AI 이미지 생성
     */
    @Override
    public List<DailyDtoRes.DailyImageUploadRes> generateAiImages(Long userId, Long dailyId, LocalDate date, List<String> imageUrls) {

        // 1) 유저 및 데일리 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Daily daily = dailyRepository.findById(dailyId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DAILY_NOT_FOUND));

        UserInfo userInfo = user.getUserInfo();

        // 2) 유저 정보 Map 변환
        Map<String, Object> userInfoMap = buildUserInfoMap(userInfo);

        // 3) 이미지 URL 유효성 검사
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new GeneralException(ErrorStatus.IMAGE_NOT_FOUND);
        }

        // 4) 이미지 다운로드 (첫 장은 배경, 나머지는 인물)
        MultipartFile backgroundFile = null;
        List<MultipartFile> personFiles = new ArrayList<>();

        try {
            for (int i = 0; i < imageUrls.size(); i++) {
                URL url = new URL(imageUrls.get(i));
                try (InputStream in = url.openStream()) {
                    byte[] bytes = in.readAllBytes();
                    String filename = "img_" + i + ".jpg";

                    MockMultipartFile file = new MockMultipartFile(
                            filename, filename, MediaType.IMAGE_JPEG_VALUE, bytes
                    );

                    if (i == 0) {
                        backgroundFile = file;
                    } else if (personFiles.size() < 2) {
                        personFiles.add(file);
                    }
                }
            }
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.FILE_DOWNLOAD_FAIL);
        }

        // 5) 파이썬 호출
        String dateStr = date != null ? date.toString() : LocalDate.now().toString();
        byte[] zipBytes = pythonAiClient.generateStoryAndImages(
                String.valueOf(user.getId()),
                dateStr,
                null, // topic
                userInfoMap,
                personFiles,
                backgroundFile
        );

        if (zipBytes == null || zipBytes.length == 0) {
            throw new GeneralException(ErrorStatus.FILE_DOWNLOAD_FAIL);
        }

        // 6) ZIP 해제 후 S3 업로드
        return unzipAndUpload(zipBytes, daily);
    }

    /**
     * 유저 정보(UserInfo)를 파이썬 전송용 Map으로 변환
     */
    private Map<String, Object> buildUserInfoMap(UserInfo userInfo) {
        Map<String, Object> map = new HashMap<>();
        if (userInfo == null) return map;

        map.put("age", userInfo.getAge());
        map.put("gender", userInfo.getGender() != null ? userInfo.getGender().name() : null);
        map.put("cognitiveStatus", userInfo.getCognitiveStatus() != null ? userInfo.getCognitiveStatus().name() : null);
        map.put("hometown", userInfo.getHometown());
        map.put("lifeHistory", userInfo.getLifeHistory());
        map.put("familyInfo", userInfo.getFamilyInfo());
        map.put("education", userInfo.getEducation());
        map.put("occupation", userInfo.getOccupation());
        map.put("forbiddenKeywords", userInfo.getForbiddenKeywords());
        map.put("lifetimeline", userInfo.getLifetimeline());
        return map;
    }

    /**
     * 파이썬 ZIP 응답을 해제하여 S3 업로드 후 URL 리스트 반환
     */
    private List<DailyDtoRes.DailyImageUploadRes> unzipAndUpload(byte[] zipBytes, Daily daily) {
        List<DailyDtoRes.DailyImageUploadRes> resultList = new ArrayList<>();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] imageBytes = zis.readAllBytes();

                MultipartFile file = new MockMultipartFile(
                        entry.getName(),
                        entry.getName(),
                        MediaType.IMAGE_PNG_VALUE,
                        imageBytes
                );

                DailyImage saved = amazonS3Util.uploadDailyImageAndSaveMeta(file, daily);

                resultList.add(DailyDtoRes.DailyImageUploadRes.builder()
                        .dailyImageId(saved.getId())
                        .url(saved.getUrl())
                        .build());
            }

        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.FILE_UPLOAD_FAIL);
        }

        return resultList;
    }
}
