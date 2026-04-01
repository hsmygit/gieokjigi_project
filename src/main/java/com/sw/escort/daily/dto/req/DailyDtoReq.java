package com.sw.escort.daily.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class DailyDtoReq {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordDailyReq {
        private String conversation;
        private String feedback;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate dailyDayRecording;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordFeedbackReq {
        private String feedback;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate dailyDayRecording;
    }



    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyImageGenerationReq {
        private Long dailyId; // 어떤 데일리에 연결할지 명시
        private List<String> imageUrls; // 안드로이드에서 선택한 S3 이미지 URL
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate date; // 회상 대화 날짜
    }
}
