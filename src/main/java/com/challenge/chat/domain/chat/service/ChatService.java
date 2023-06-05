package com.challenge.chat.domain.chat.service;

import com.challenge.chat.domain.chat.dto.ChatDto;
import com.challenge.chat.domain.chat.dto.ChatRoomDto;
import com.challenge.chat.domain.chat.dto.EnterUserDto;
import com.challenge.chat.domain.chat.entity.Chat;
import com.challenge.chat.domain.chat.entity.ChatRoom;
import com.challenge.chat.domain.chat.entity.MemberChatRoom;
import com.challenge.chat.domain.chat.entity.MessageType;
import com.challenge.chat.domain.chat.repository.ChatRepository;
import com.challenge.chat.domain.chat.repository.ChatRoomRepository;
import com.challenge.chat.domain.chat.repository.MemberChatRoomRepository;
import com.challenge.chat.domain.member.entity.Member;
import com.challenge.chat.domain.member.service.MemberService;
import com.challenge.chat.exception.RestApiException;
import com.challenge.chat.exception.dto.ChatErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatService {

	private final MemberChatRoomRepository memberChatRoomRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatRepository chatRepository;
	private final MemberService memberService;

	// 채팅방 조회
	@Transactional(readOnly = true)
	public List<ChatRoomDto> showRoomList() {
		log.info("Service 채팅방 조회");

		// TODO : 내가 구독한 채팅방만 표출되게 변경하기
		return chatRoomRepository.findAll()
			.stream()
			.map(ChatRoomDto::from)
			.collect(Collectors.toList());
	}

	// 채팅방 생성
	public String createChatRoom(ChatRoomDto chatRoomDto) {
		log.info("Service 채팅방 생성");

		chatRoomRepository.save(ChatRoomDto.toEntity(chatRoomDto));
		return "Successfully created chat room";
	}

	// 채팅방 입장
	public ChatDto enterChatRoom(ChatDto chatDto, SimpMessageHeaderAccessor headerAccessor) {
		log.info("Service 채팅방 입장");

		ChatRoom chatRoom = getRoomByRoomId(chatDto.getRoomId());
		Member member = memberService.findMemberByEmail(chatDto.getUserId());
		// 중간 테이블 생성
		Optional<MemberChatRoom> memberChatRoom = memberChatRoomRepository.findByMemberIdAndRoomId(member.getEmail(), chatRoom.getRoomId());
		if (memberChatRoom.isEmpty()) {
			memberChatRoomRepository.save(new MemberChatRoom(chatRoom, member));
		}
		//반환 결과를 socket session 에 사용자의 id로 저장
		Objects.requireNonNull(headerAccessor.getSessionAttributes()).put("userId", chatDto.getUserId());
		headerAccessor.getSessionAttributes().put("roomId", chatDto.getRoomId());
		headerAccessor.getSessionAttributes().put("nickName", chatDto.getSender());
		chatDto.setMessage(chatDto.getSender() + "님 입장!! ο(=•ω＜=)ρ⌒☆");

		return chatDto;
	}

	// 채팅방 나가기
	@Transactional(readOnly = true)
	public ChatDto leaveChatRoom(SimpMessageHeaderAccessor headerAccessor) {
		log.info("Service 채팅방 나가기");

		String roomId = (String)headerAccessor.getSessionAttributes().get("roomId");
		String nickName = (String)headerAccessor.getSessionAttributes().get("nickName");
		String userId = (String)headerAccessor.getSessionAttributes().get("userId");

		return ChatDto.builder()
			.type(MessageType.LEAVE)
			.roomId(roomId)
			.sender(nickName)
			.userId(userId)
			.message(nickName + "님 퇴장!! ヽ(*。>Д<)o゜")
			.build();
	}

	// 채팅 메세지 조회
	@Transactional(readOnly = true)
	public EnterUserDto viewChat(String roomId, String email) {
		log.info("Service 채팅방 메세지 조회");

		ChatRoom chatRoom = getRoomByRoomId(roomId);
		Member member = memberService.findMemberByEmail(email);

		List<ChatDto> chatDtoList = chatRepository
				.findByRoomId(chatRoom.getRoomId())
				.stream()
				.sorted(Comparator.comparing(Chat::getCreatedAt))
				.map(ChatDto::from) // Using the from() method as a method reference
				.toList();

		return new EnterUserDto(member.getNickname(), member.getEmail(), chatRoom.getRoomId(), chatDtoList);
	}

	// 채팅 보내기
	public void sendChatRoom(ChatDto chatDto) {
		log.info("Service 채팅 SEND");

		isRoomExist(chatDto.getRoomId());

		Chat chat = new Chat(chatDto, MessageType.TALK);
		chatRepository.save(chat);
	}

	public ChatRoom getRoomByRoomId(String roomId) {
		return chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new RestApiException(ChatErrorCode.CHATROOM_NOT_FOUND));
	}

	public void isRoomExist(String roomId) {
		chatRoomRepository.findByRoomId(roomId).orElseThrow(
				() -> new RestApiException(ChatErrorCode.CHATROOM_NOT_FOUND));
	}
}