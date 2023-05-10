package com.talkeasy.server.service.chat;

import com.google.gson.Gson;
import com.talkeasy.server.common.CommonResponse;
import com.talkeasy.server.common.PagedResponse;
import com.talkeasy.server.domain.chat.ChatRoom;
import com.talkeasy.server.domain.chat.ChatRoomDetail;
import com.talkeasy.server.domain.chat.LastChat;
import com.talkeasy.server.domain.chat.UserData;
import com.talkeasy.server.domain.member.Member;
import com.talkeasy.server.dto.chat.ChatRoomDto;
import com.talkeasy.server.dto.chat.ChatRoomListDto;
import com.talkeasy.server.dto.chat.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    private Gson gson;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private AmqpAdmin amqpAdmin;

    @Mock
    private RabbitAdmin rabbitAdmin;


    @BeforeEach
    void setUp() {
        gson = new Gson();
    }

    @BeforeEach
    void createChatUsers() {
        Map<String, UserData> chatUsers = new HashMap<>();
        chatUsers.put("1", new UserData(true, null));
        chatUsers.put("2", new UserData(true, null));
    }


    @Test
    @DisplayName("채팅방 생성- 정상(leaveTime 내역이 없을 경우)")
    void createRoom() throws IOException {

        Map<String, UserData> chatUsers = new HashMap<>();
        chatUsers.put("1", new UserData(true, null));
        chatUsers.put("2", new UserData(true, null));

        ChatRoom chatRoom = ChatRoom.builder().id("3").users(new String[]{"1", "2"}).chatUsers(chatUsers).date("2023.01.01").build();
        Mockito.when(mongoTemplate.findOne(eq(Query.query(Criteria.where("users").all(new String[]{"1", "2"}))), eq(ChatRoom.class))).thenReturn(null);

        Mockito.when(mongoTemplate.insert(any(ChatRoom.class), eq("chat_room"))).thenReturn(chatRoom);

        Mockito.when(mongoTemplate.findOne(eq(Query.query(Criteria.where("id").is("3"))), eq(ChatRoom.class))).thenReturn(chatRoom);

        CommonResponse commonResponse = chatService.createRoom("1", "2");

        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(ChatRoom.class));
        verify(mongoTemplate, times(1)).insert(any(ChatRoom.class));

        verify(rabbitAdmin, times(4)).declareQueue(any(Queue.class));
        verify(rabbitAdmin, times(4)).declareBinding(any(Binding.class));
        verify(mongoTemplate, times(2)).insert(any(ChatRoomDetail.class));

        assertThat(commonResponse.getStatus()).isEqualTo(201);
    }


    @Test
    @DisplayName("채팅방 생성- 이미 채팅방이 있는 경우, 새로 생성하지 않고 원래의 채팅방 아이디를 반환 / 채팅방에서 퇴장했던 유저가 다시 입장하는 경우")
    void createRoomCaseAlreadyExist() throws IOException {

        Map<String, UserData> chatUsers = new HashMap<>();
        chatUsers.put("1", new UserData(true, null));
        chatUsers.put("2", new UserData(true, null));

        ChatRoom chatRoom = ChatRoom.builder().id("3").users(new String[]{"1", "2"}).chatUsers(chatUsers).date("2023.01.01").build();
        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(ChatRoom.class))).thenReturn(chatRoom);
        CommonResponse commonResponse = chatService.createRoom("1", "2");

        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(ChatRoom.class));
        verify(mongoTemplate, times(1)).save(any(ChatRoom.class));
        verify(mongoTemplate, never()).insert(any(ChatRoom.class));

        assertThat(commonResponse.getStatus()).isEqualTo(200);

    }

//    @Test
//    void doCreateRoomChat() throws IOException {
//
//        ChatRoom chatRoom = new ChatRoom(new String[]{"1", "2"}, "hihi", LocalDateTime.now().toString());
//        chatRoom.setId("3");
//        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(ChatRoom.class))).thenReturn(chatRoom);
//
//
//        chatService.doCreateRoomChat(chatRoom, "1");
//
//        verify(mongoTemplate, times(2)).save(any(ChatRoom.class));
//
//
//    }

    @Test
    @DisplayName("채팅 큐 생성 세부 메서드 호출")
    void createQueue() {
        ChatRoom chatRoom = new ChatRoom(new String[]{"1", "2"}, "hihi", LocalDateTime.now().toString());
        ChatRoomDto chatRoomDto = new ChatRoomDto(chatRoom);
        chatService.createQueue(chatRoomDto);

        verify(rabbitAdmin, times(4)).declareQueue(any(Queue.class));
        verify(rabbitAdmin, times(4)).declareBinding(any(Binding.class));
    }

//    @Test
//    @DisplayName("채팅 큐 생성 세부 메서드")
//    void createQueueDetail() {
//        ChatRoom chatRoom = new ChatRoom(new String[]{"1", "2"}, "hihi", LocalDateTime.now().toString());
//        ChatRoomDto chatRoomDto = new ChatRoomDto(chatRoom);
//        chatService.createQueueDetail(chatRoomDto, "chat", "1");
//        verify(rabbitAdmin, times(1)).declareQueue(any(Queue.class));
//        verify(rabbitAdmin, times(1)).declareBinding(any(Binding.class));
//    }

    @Test
    @DisplayName("메시지를 전송하기전 Message로 변환 / 아직 읽지 않았기 때문에 readCnt의 디폴트값은 1")
    void convertChat() {

        ChatRoomDetail chat = ChatRoomDetail.builder().roomId("3").fromUserId("1").toUserId("2").created_dt("2023.05.01").build();
        Message msg = MessageBuilder.withBody(gson.toJson(chat).getBytes()).build();

        ChatRoomDetail chatRoomDetail = chatService.convertChat(gson, msg);
        assertThat(chatRoomDetail.getReadCnt()).isEqualTo(1);

    }

    @Test
    @DisplayName("채팅 보낼때 메시지 저장")
    void saveChat() {
        ChatRoomDetail chat = ChatRoomDetail.builder().roomId("3").fromUserId("1").toUserId("2").created_dt("2023.05.01").build();
        Mockito.when(mongoTemplate.insert(any(ChatRoomDetail.class))).thenReturn(chat);

        String roomId = chatService.saveChat(chat);

        verify(mongoTemplate, times(1)).insert(any(ChatRoomDetail.class));
        verify(mongoTemplate, times(2)).remove(any(Query.class), eq(LastChat.class));
        verify(mongoTemplate, times(2)).insert(any(LastChat.class), eq("last_chat"));
        assertThat(roomId).isEqualTo("3");
    }

//    @Test
//    @DisplayName("메시지 보내기")
//    void doChat() throws IOException {
//        ChatRoomDetail chat = ChatRoomDetail.builder().roomId("3").fromUserId("1").toUserId("2").created_dt("2023.05.01").build();
//
//        Map<String, UserData> chatUsers = new HashMap<>();
//        chatUsers.put("1", new UserData(true, null));
//        chatUsers.put("2", new UserData(true, null));
//
//        ChatRoom chatRoom = ChatRoom.builder().id("3").users(new String[]{"1", "2"}).chatUsers(chatUsers).date("2023.01.01").build();
//        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(ChatRoom.class))).thenReturn(chatRoom);
//
//        chatService.doChat(gson, chat);
//
//        verify(rabbitTemplate, times(1)).send(eq("chat.exchange"), any(String.class), any(Message.class));
////        verify(rabbitTemplate, times(1)).convertAndSend(eq("user.exchange"), any(String.class), any(Message.class));
////        verify(rabbitTemplate, times(1)).convertAndSend(eq("user.exchange"), any(String.class), any(Message.class));
//        verify(rabbitTemplate, times(1)).convertAndSend(eq("user.exchange"), any(String.class), any(Class.class));
//        verify(rabbitTemplate, times(1)).convertAndSend(eq("user.exchange"), any(String.class), any(Class.class));
//    }

    @Test
    void sendChatMessage() {

        ChatRoomDetail chatRoomDetail = ChatRoomDetail.builder().roomId("3").fromUserId("1").toUserId("2").created_dt("2023.05.01").build();
        chatService.sendChatMessage(gson, chatRoomDetail, "1");

        verify(rabbitTemplate, times(1)).send(eq("chat.exchange"), eq("room.3.1"), any(Message.class));
    }

    @Test
    @DisplayName("채팅방 유저 정보 업데이트 userId가 admin이 아니고 nowIn이 true였던 경우 - 변경안됨")
    void updateUserInChatRoomCaseNotAdmin() {
        ChatRoom chatRoom = new ChatRoom(new String[]{"1", "2"}, "hihi", LocalDateTime.now().toString());
        chatService.updateUserInChatRoom(chatRoom, "1");

        verify(mongoTemplate, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("채팅방 유저 정보 업데이트 - 보내는이가 admin이고 nowIn이 true였던 경우 - ")
    void updateUserInChatRoomCaseAdmin() {
        ChatRoom chatRoom = new ChatRoom(new String[]{"admin", "2"}, "hihi", LocalDateTime.now().toString());
        chatService.updateUserInChatRoom(chatRoom, "admin");

        verify(mongoTemplate, times(0)).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("채팅방 유저 정보 업데이트 - userId가 admin이 아니고 nowIn이 false였던 경우 - 변경됨")
    void updateUserInChatRoomCaseNotAdminAndNowInIsFalse() {
        ChatRoom chatRoom = new ChatRoom(new String[]{"1", "2"}, "hihi", LocalDateTime.now().toString());
        chatRoom.getChatUsers().get("1").setNowIn(false);
        chatService.updateUserInChatRoom(chatRoom, "1");

        verify(mongoTemplate, times(1)).save(any(ChatRoom.class));
    }


    @Test
    @DisplayName("채팅 내역 불러오기 - 정상(leaveTime 내역이 없을 경우)")
    void getChatHistory() {

        Map<String, UserData> chatUsers = new HashMap<>();
        chatUsers.put("1", new UserData(true, null));
        chatUsers.put("2", new UserData(true, null));

        ChatRoom chatRoom = ChatRoom.builder().id("3").users(new String[]{"1", "2"}).chatUsers(chatUsers).date("2023.01.01").build();
        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(ChatRoom.class))).thenReturn(chatRoom);

        List<ChatRoomDetail> chatRoomDetails = new ArrayList<>();
        chatRoomDetails.add(ChatRoomDetail.builder().roomId("3").created_dt("2023.05.01").build());
        Mockito.when(mongoTemplate.find(any(Query.class), eq(ChatRoomDetail.class))).thenReturn(chatRoomDetails);

        PagedResponse<ChatRoomDetail> response = chatService.getChatHistory("3", 1, 10, "1");

        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(ChatRoom.class));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(ChatRoomDetail.class));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getTotalPages()).isEqualTo(1);

    }

    @Test
    @DisplayName("참가중인 채팅방 목록 불러오기(하나의 채팅방에 참가한 경우)")
    void getChatRoomList() {

        ChatRoomDetail chatRoomDetail = ChatRoomDetail.builder().fromUserId("1").toUserId("2").roomId("3").build();
        List<LastChat> lastChatList = new ArrayList<>();

        LastChat lastChat1 = new LastChat(chatRoomDetail);
        lastChat1.setUserId(lastChat1.getFromUserId());
        lastChatList.add(lastChat1);

        Mockito.when(mongoTemplate.find(any(Query.class), eq(LastChat.class))).thenReturn(lastChatList);

        Member member = Member.builder().id("1").build();
        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(Member.class))).thenReturn(member);

        List<ChatRoomListDto> chatRoomListDtoList = new ArrayList<>();

        for (LastChat lastChat : lastChatList) {
            ChatRoomListDto chatRoomListDto = new ChatRoomListDto(lastChat);
            chatRoomListDtoList.add(chatRoomListDto);
        }

        PagedResponse<ChatRoomListDto> pagedResponse = chatService.getChatRoomList("1");

        verify(rabbitAdmin, times(1)).getQueueInfo(eq("chat.queue.3.1"));

        assertThat(pagedResponse.getData()).isEqualTo(chatRoomListDtoList);
        assertThat(pagedResponse.getStatus()).isEqualTo(200);
        assertThat(pagedResponse.getTotalPages()).isEqualTo(1);

    }

    @Test
    @DisplayName("유저 조회하기")
    void getMemberById() {

        Member member = Member.builder().id("1").build();
        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(Member.class))).thenReturn(member);

        chatService.getMemberById("1");

        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(Member.class));

    }

    @Test
    @DisplayName("큐 정보 조회하기")
    void getQueueInfo() {

        chatService.getQueueInfo("3", "1");
        verify(rabbitAdmin, times(1)).getQueueInfo(eq("chat.queue.3.1"));

    }

    @Test
    @DisplayName("마지막 채팅 지우기 - 정상")
    void saveLastChat() {

        ChatRoomDetail chatRoomDetail = ChatRoomDetail.builder().fromUserId("1").toUserId("2").roomId("3").build();
        chatService.saveLastChat(chatRoomDetail);

        verify(mongoTemplate, times(2)).remove(any(Query.class), eq(LastChat.class));
        verify(mongoTemplate, times(2)).insert(any(LastChat.class), eq("last_chat"));

    }

    @Test
    @DisplayName("마지막 채팅 지우기(세부 메서드) - 정상")
    void saveLastChatDetail() {
        ChatRoomDetail chatRoomDetail = ChatRoomDetail.builder().fromUserId("1").toUserId("2").roomId("3").build();
        String userId = "1";
        chatService.saveLastChatDetail(chatRoomDetail, userId);

        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(LastChat.class));
        verify(mongoTemplate, times(1)).insert(any(LastChat.class), eq("last_chat"));

    }

    @Test
    @DisplayName("마지막 채팅 조회하기")
    void getLastChatList() {

        String userId = "1";
        ChatRoomDetail chatRoomDetail = ChatRoomDetail.builder().fromUserId("1").toUserId("2").roomId("3").build();

        LastChat lastChat = new LastChat(chatRoomDetail);
        Mockito.when(mongoTemplate.find(any(Query.class), eq(LastChat.class))).thenReturn(Collections.singletonList(lastChat));

        chatService.getLastChatList(userId);
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(LastChat.class));
    }

    @Test
    @DisplayName("채팅방 삭제하기 - 채팅방 삭제되지 않고 nowIn이 false가 되는 경우")
    void deleteRoom() throws IOException {

        Map<String, UserData> chatUsers = new HashMap<>();
        chatUsers.put("1", new UserData(true, null));
        chatUsers.put("2", new UserData(true, null));
        ChatRoom chatRoom = ChatRoom.builder().id("3").chatUsers(chatUsers).users(new String[]{"1", "2"}).build();
        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(ChatRoom.class))).thenReturn(chatRoom);

        String result = chatService.deleteRoom("3", "1");

        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(ChatRoom.class));
        verify(mongoTemplate, times(1)).save(any(ChatRoom.class));
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(LastChat.class));

        assertEquals("3", result);

    }

    @Test
    @DisplayName("채팅방 삭제하기 - 채팅방에 이미 상대방도 없을 경우")
    void deleteRoomCaseNoExistToUser() throws IOException {

        Map<String, UserData> chatUsers = new HashMap<>();
        chatUsers.put("1", new UserData(true, null));
        chatUsers.put("2", new UserData(false, "2023.05.01"));
        ChatRoom chatRoom = ChatRoom.builder().id("3").chatUsers(chatUsers).users(new String[]{"1", "2"}).build();
        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(ChatRoom.class))).thenReturn(chatRoom);

        String result = chatService.deleteRoom("3", "1");

        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(ChatRoom.class));

        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ChatRoom.class));
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ChatRoomDetail.class));
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(LastChat.class));

        String queueName = String.format("chat.queue.%s.%s", "3", "1");
        QueueInformation queueInformation = rabbitAdmin.getQueueInfo(queueName);

        assertThat(result).isEqualTo("3");
        assertThat(mongoTemplate.findById("3", ChatRoom.class)).isNull();
        assertThat(mongoTemplate.findById("3", ChatRoomDetail.class)).isNull();
        assertThat(mongoTemplate.findById("3", LastChat.class)).isNull();
    }

    @Test
    void getUserInfoByRoom() {

        ChatRoom chatRoom = ChatRoom.builder().id("3").users(new String[]{"1", "2"}).date("2023.01.01").build();
        Mockito.when(mongoTemplate.findOne(any(Query.class), eq(ChatRoom.class))).thenReturn(chatRoom);

        List<Member> members = new ArrayList<>();
        members.add(Member.builder().id("1").build());
        members.add(Member.builder().id("2").build());
        Mockito.when(mongoTemplate.find(any(Query.class), eq(Member.class))).thenReturn(members);

        PagedResponse<UserInfo> pagedResponse = chatService.getUserInfoByRoom("3");

        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(ChatRoom.class));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Member.class));

        assertThat(pagedResponse.getStatus()).isEqualTo(200);
//        assertThat(pagedResponse.getData()).isEqualTo(2);
        assertThat(pagedResponse.getTotalPages()).isEqualTo(1);

    }
}