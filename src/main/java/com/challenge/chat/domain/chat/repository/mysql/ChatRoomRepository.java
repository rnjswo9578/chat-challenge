package com.challenge.chat.domain.chat.repository.mysql;

import com.challenge.chat.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByRoomId(String roomId);

}
