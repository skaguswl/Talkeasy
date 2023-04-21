package com.talkeasy.server.domain.chat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("chat_room")
@Getter
@Setter
public class ChatRoom {

    //id는 소문자로 설정해야함
    @Id
    private String id;
    private Long[] users;
    private String title; // 채팅방 이름
    private String date; // 생성 시간?

    public ChatRoom(Long[] users, String title, String date) {
        this.users = users;
        this.title = title;
        this.date = date;
    }
}
//