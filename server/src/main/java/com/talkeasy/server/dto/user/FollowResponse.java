package com.talkeasy.server.dto.user;

import com.talkeasy.server.domain.chat.ChatRoom;
import com.talkeasy.server.domain.chat.LastChat;
import com.talkeasy.server.domain.member.Follow;
import com.talkeasy.server.domain.member.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FollowResponse {

    private String followId;
    private String roomId;
    private String userId;
    private String userName;
    private String imageUrl;
    private Integer gender; //0:남/1:여
    private Integer age;
    private String memo;
    private String birthDate;
    private Boolean mainStatus; // 주보호자 설정
    private Boolean locationStatus; // 위치정보 제공 동의 여부 설정
    private String nickName;
    private LastChatResponse lastChat;

    public FollowResponse(Member member, Follow follow, ChatRoom chatRoom) {
        this.followId = follow.getId();
        this.userId = member.getId();
        this.userName = member.getName();
        this.imageUrl = member.getImageUrl();
        this.memo = follow.getMemo();
        this.mainStatus = follow.getMainStatus();
        this.locationStatus = follow.getLocationStatus();
        this.age = member.getAge();
        this.gender = member.getGender();
        this.birthDate = member.getBirthDate();
        if(chatRoom!=null){
            this.roomId = chatRoom.getId();
        }
        this.nickName = follow.getNickName();
    }
}
