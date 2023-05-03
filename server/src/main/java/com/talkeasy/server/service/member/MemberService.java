package com.talkeasy.server.service.member;

import com.talkeasy.server.config.s3.S3Uploader;
import com.talkeasy.server.domain.Member;
import com.talkeasy.server.domain.aac.CustomAAC;
import com.talkeasy.server.domain.app.UserAppToken;
import com.talkeasy.server.domain.chat.ChatRoom;
import com.talkeasy.server.dto.user.MemberInfoUpdateRequest;
import com.talkeasy.server.repository.member.MembersRepository;
import com.talkeasy.server.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {

    private final MembersRepository memberRepository;
    private final MongoTemplate mongoTemplate;
    private final S3Uploader s3Uploader;
    private final AmqpAdmin amqpAdmin;
    private final ChatService chatService;

    public Member getUserInfo(String email) {
        return memberRepository.findByEmail(email);
    }

    public Member findUserByEmail(String email) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("email").is(email)), Member.class);
    }

    public Member updateUserInfo(MultipartFile multipartFile, MemberInfoUpdateRequest request, String memberId) {
        Member member = mongoTemplate.findOne(
                Query.query(Criteria.where("id").is(memberId)), Member.class);
        try {
            log.info("============file: " + multipartFile);
            String saveFileName = s3Uploader.uploadFiles(multipartFile, "talkeasy");
            member.setUserInfo(request, saveFileName);
        } catch (Exception e) {
        }
        return memberRepository.save(member);

    }

    public String saveUser(Member member) {
        Member member1 = memberRepository.save(member);
        return member1.getId();
    }

    /* 회원 탈퇴 : 관련 큐 삭제, 커스텀 AAC 삭제, 유저 앱 토큰 삭제, 채팅방 내부 사용자 정보 "null"으로 변경 */
    public String deleteUserInfo(String userId) throws IOException {

        // 유저 큐 삭제
        deleteQueue("user.queue", null, userId);

        // 채팅 큐 삭제
        List<ChatRoom> chatRoomList = mongoTemplate.find(Query.query(Criteria.where("users").in(userId)), ChatRoom.class);
        for (ChatRoom chatRoom : chatRoomList){
            chatService.deleteRoom(chatRoom.getId(), userId);
//            deleteQueue("chat.queue", chatRoom.getId(), userId);
//            deleteQueue("read.queue", chatRoom.getId(), userId);
        }

        // Member 테이블에서 삭제
        Member member =  mongoTemplate.findOne(Query.query(Criteria.where("id").is(userId)), Member.class);
        member.setDelete();
        mongoTemplate.save(member);

        // 커스텀 AAC 삭제
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(userId)), CustomAAC.class);

        // 유저 앱 토큰 삭제
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(userId)), UserAppToken.class);

        return userId;
    }

    private void deleteQueue(String queueName, String roomId, String userId){
        if(roomId == null) {
            amqpAdmin.deleteQueue(queueName + "." + userId);
            return;
        }
        amqpAdmin.deleteQueue(queueName + "." + roomId + "." + userId);
    }
}