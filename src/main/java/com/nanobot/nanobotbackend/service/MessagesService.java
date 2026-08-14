package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.entity.MessageEntity;
import java.util.List;
import java.util.Optional;

public interface MessagesService {
  MessageEntity createMessage(MessageDto messageDto);

  List<MessageEntity> getMessages();

  Optional<MessageEntity> getMessageById(String id);

  Optional<MessageEntity> updateMessage(String id, MessageDto messageDto);

  Optional<MessageEntity> deleteMessage(String id);
}
