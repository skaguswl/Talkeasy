package com.talkeasy.server.service.firebase;

import com.talkeasy.server.domain.app.UserAppToken;
import com.talkeasy.server.domain.chat.ChatRoomDetail;
import com.talkeasy.server.domain.member.Member;
import com.talkeasy.server.dto.chat.MessageDto;
import com.talkeasy.server.dto.firebase.FcmMessage;
import com.talkeasy.server.dto.firebase.RequestFcmDto;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.apache.http.HttpHeaders;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FirebaseCloudMessageService {
    private final String API_URL = "https://fcm.googleapis.com/v1/projects/\n" +
            "talkeasy/messages:send";
    private final ObjectMapper objectMapper;

    private final MongoTemplate mongoTemplate;
    public void sendMessageTo(String targetToken, String title, String body) throws IOException {
        // userId에 따른 appToken 가져오기

        String message = makeMessage(targetToken, title, body);
//        String message = makeMessage(targetToken, title, body);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(message,
                MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json; UTF-8")
                .build();

        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());
    }

    private String makeMessage(String targetToken, String title, String body) throws JsonProcessingException {

        String bodyJson = objectMapper.writeValueAsString(body);

        FcmMessage fcmMessage = FcmMessage.builder()
                .message(FcmMessage.Message.builder()
                        .token(targetToken)
                        .notification(FcmMessage.Notification.builder()
                                .title(title)
                                .body(bodyJson)
                                .image(null)
                                .build()
                        ).build()).validateOnly(false).build();

        return objectMapper.writeValueAsString(fcmMessage);
    }

    /* json 형식의 스트링 메시지 전송*/

    public void sendMessageTo(String targetToken, String title, MessageDto body) throws IOException {

        // userId에 따른 appToken 가져오기

        String message = makeMessage(targetToken, title, body);
//        String message = makeMessage(targetToken, title, body);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(message,
                MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json; UTF-8")
                .build();

        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());
    }

    private String makeMessage(String targetToken, String title, MessageDto body) throws JsonProcessingException {

        String bodyJson = objectMapper.writeValueAsString(body);

        FcmMessage fcmMessage = FcmMessage.builder()
                .message(FcmMessage.Message.builder()
                        .token(targetToken)
                        .notification(FcmMessage.Notification.builder()
                                .title(title)
                                .body(bodyJson)
                                .image(null)
                                .build()
                        ).build()).validateOnly(false).build();

        return objectMapper.writeValueAsString(fcmMessage);
    }


    //firebase 서비스 인증 정보
    private String getAccessToken() throws IOException {
        String firebaseConfigPath = "firebase/serviceAccountKey.json";

        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource(firebaseConfigPath).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    public String saveAppToken(String userId, String appToken) {

        UserAppToken userAppToken = Optional.ofNullable(mongoTemplate.findOne(Query.query(Criteria.where("userId").is(userId)), UserAppToken.class)).orElse(null);

        if (userAppToken != null){
            userAppToken.setAppToken(appToken);
            mongoTemplate.save(userAppToken);
            return userAppToken.getAppToken();
        }

        UserAppToken newUserAppToken = mongoTemplate.save(new UserAppToken(userId, appToken));

        return newUserAppToken.getAppToken();
    }

    public void sendFcm(RequestFcmDto requestFcmDto) throws IOException {

        UserAppToken userAppToken = Optional.ofNullable(mongoTemplate.findOne(Query.query(Criteria.where("userId").is(requestFcmDto.getUserId())), UserAppToken.class)).orElse(null);

        if (userAppToken == null)
            throw new NullPointerException("해당하는 사용자의 fcm 토큰이 존재하지 않습니다.");

        sendMessageTo(userAppToken.getAppToken(), requestFcmDto.getTitle(), requestFcmDto.getBody());
    }
}
