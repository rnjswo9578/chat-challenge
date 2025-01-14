package com.challenge.chat.domain.chat.repository;

import com.challenge.chat.domain.chat.entity.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface ChatRepository extends MongoRepository<Chat, String> {
    // Spring Data MongoDB -> https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/

    List<Chat> findByRoomId(String roomId);
}
