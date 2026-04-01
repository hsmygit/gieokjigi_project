package com.sw.escort.common.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sw.escort.chat.dto.req.ChatStartReq;
import com.sw.escort.chat.dto.res.ChatResponse;
import com.sw.escort.daily.dto.res.DailyDtoRes;
import com.sw.escort.media.entity.UserInfoPhoto;
import com.sw.escort.user.entity.User;
import com.sw.escort.user.entity.UserInfo;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PythonAiClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://ai:5000")//localhost:8000
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB 버퍼 허용
            .clientConnector(
                    new ReactorClientHttpConnector(
                            HttpClient.create()
                                    .responseTimeout(Duration.ofMinutes(10))     // 응답 대기 최대 5분
                                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)  // 연결 타임아웃 30초
                                    .doOnConnected(conn ->
                                            conn.addHandlerLast(new ReadTimeoutHandler(300))
                                                    .addHandlerLast(new WriteTimeoutHandler(300))
                                    )
                    )
            )
            .build();

    public ChatResponse.ChatDetail sendChatToPython(User user, UserInfo info, ChatStartReq req) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", String.valueOf(user.getId()));
        payload.put("topic", req.getTopic());

        String prompt = req.getUserPrompt();
        if (prompt != null && !prompt.trim().isEmpty()) {
            payload.put("user_prompt", prompt);
        }

        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("age", String.valueOf(info.getAge()));
        userInfoMap.put("gender", info.getGender());
        userInfoMap.put("cognitiveStatus", info.getCognitiveStatus().name());
        userInfoMap.put("hometown", info.getHometown());
        userInfoMap.put("lifeHistory", info.getLifeHistory());
        userInfoMap.put("familyInfo", info.getFamilyInfo());
        userInfoMap.put("education", info.getEducation());
        userInfoMap.put("occupation", info.getOccupation());
        userInfoMap.put("forbiddenKeywords", info.getForbiddenKeywords());
        userInfoMap.put("lifetimeline", info.getLifetimeline());

        payload.put("user_info", userInfoMap);

        return webClient.post()
                .uri("/ai/chat")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block()
                .getResponse();
    }

    public byte[] generateVideo(String prompt, MultipartFile inputImage) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("prompt", prompt);
        builder.part("input_image", inputImage.getResource());

        return webClient.post()
                .uri("/ai/generate-video")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    public DailyDtoRes.ConversationRes fetchAiConversation(Long userId, LocalDate localDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/conversation-by-date")
                        .queryParam("user_id", userId.toString())
                        .queryParam("date", localDate.toString())
                        .build())
                .retrieve()
                .bodyToMono(DailyDtoRes.ConversationRes.class)
                .block();
    }

    public List<MultipartFile> requestImageGeneration(User user, List<UserInfoPhoto> photos) {
        List<Map<String, Object>> imageList = photos.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("description", p.getDescription());
            map.put("relation_to_patient", p.getRelationToPatient());
            map.put("url", p.getUrl());
            return map;
        }).toList();

        Map<String, Object> request = new HashMap<>();
        request.put("user_id", String.valueOf(user.getId()));
        request.put("images", imageList);

        return webClient.post()
                .uri("/ai/generate-recall-video-frames")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(MultipartFile.class)
                .collectList()
                .block();
    }

    public List<String> fetchTopicFileNames(Long userId, UserInfo info) {
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", String.valueOf(userId));

        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("age", String.valueOf(info.getAge()));
        userInfoMap.put("gender", info.getGender());
        userInfoMap.put("cognitiveStatus", info.getCognitiveStatus().name());
        userInfoMap.put("hometown", info.getHometown());
        userInfoMap.put("lifeHistory", info.getLifeHistory());
        userInfoMap.put("familyInfo", info.getFamilyInfo());
        userInfoMap.put("education", info.getEducation());
        userInfoMap.put("occupation", info.getOccupation());
        userInfoMap.put("forbiddenKeywords", info.getForbiddenKeywords());
        userInfoMap.put("lifetimeline", info.getLifetimeline());

        request.put("user_info", userInfoMap);

        ObjectMapper objectMapper = new ObjectMapper();

        return webClient.post()
                .uri("/ai/select-topic")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(map -> objectMapper.convertValue(map.get("response"), new TypeReference<List<String>>() {}))
                .block();
    }

    // 스토리 + 이미지 ZIP 스트리밍 다운로드 (버퍼 초과 방지)
    public byte[] generateStoryAndImages(
            String userId,
            String date,
            String topic,
            Map<String, Object> userInfoMap,
            List<MultipartFile> personImages,
            MultipartFile backgroundImage
    ) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("user_id", userId);
        builder.part("date", date);
        if (topic != null) builder.part("topic", topic);

        if (userInfoMap != null && !userInfoMap.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                builder.part("user_info_json", mapper.writeValueAsString(userInfoMap));
            } catch (Exception e) {
                throw new RuntimeException("user_info_json 직렬화 실패", e);
            }
        }

        if (personImages != null) {
            for (MultipartFile file : personImages) {
                builder.part("person_images", file.getResource())
                        .filename(file.getOriginalFilename());
            }
        }

        if (backgroundImage != null) {
            builder.part("background_image", backgroundImage.getResource())
                    .filename(backgroundImage.getOriginalFilename());
        }

        //  스트리밍 방식으로 ZIP 파일 다운로드
        return webClient.post()
                .uri("/ai/generate-story-and-images")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToFlux(DataBuffer.class)
                                .reduce(new ByteArrayOutputStream(), (out, buffer) -> {
                                    try {
                                        Channels.newChannel(out).write(buffer.asByteBuffer());
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return out;
                                })
                                .map(ByteArrayOutputStream::toByteArray);
                    } else {
                        return Mono.error(new RuntimeException("ZIP 다운로드 실패: " + response.statusCode()));
                    }
                })
                .block();
    }
}
