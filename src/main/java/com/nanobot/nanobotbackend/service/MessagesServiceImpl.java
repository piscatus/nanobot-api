package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.entity.MessageEntity;
import com.nanobot.nanobotbackend.repository.MessagesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
public class MessagesServiceImpl implements MessagesService {

  private MessagesRepository messagesRepository;

  private final FileLogger fileLogger;

  public MessagesServiceImpl(MessagesRepository messagesRepository) {
    this.fileLogger = new FileLogger("MessagesService");
    this.messagesRepository = messagesRepository;
  }

  @Override
  public MessageEntity createMessage(MessageDto messageDto) {
    MessageEntity messageEntity = new MessageEntity(messageDto);
    ObjectId id = new ObjectId();
    messageEntity.setId(id.toHexString());
    fileLogger.info("Creating message with ID: " + messageEntity.getId());
    try {
      MessageEntity createdMessage = messagesRepository.insert(messageEntity);
      fileLogger.info("Message created with ID: " + createdMessage);
      return createdMessage;
    } catch (Exception e) {
      fileLogger.error("Error creating message: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<MessageEntity> getMessages() {
    try {
      // fileLogger.info("Fetching all messages by oldest timestamp.");
      return messagesRepository.findAllSortedByOldestTimestamp();
    } catch (Exception e) {
      fileLogger.error("Error fetching messages: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<MessageEntity> getMessageById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching message with ID: " + id);
        return messagesRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching message: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<MessageEntity> updateMessage(
    String id,
    MessageDto messageDto
  ) {
    if (id != null) {
      try {
        Optional<MessageEntity> messageOptional = messagesRepository.findById(
          id
        );
        if (messageOptional.isPresent()) {
          MessageEntity message = messageOptional.get();
          message.setUserId(messageDto.getUserId());
          message.setGuildId(messageDto.getGuildId());
          message.setChannelId(messageDto.getChannelId());
          message.setMessageId(messageDto.getMessageId());
          message.setTitle(messageDto.getTitle());
          message.setColor(messageDto.getColor());
          message.setContent(messageDto.getContent());
          message.setTimestamp(messageDto.getTimestamp());
          message.setList(messageDto.getList());
          message.setUrl(messageDto.getUrl());

          MessageEntity updatedMessage = messagesRepository.save(message);
          fileLogger.info("Message updated with ID: " + id);
          return Optional.of(updatedMessage);
        } else {
          fileLogger.warn("Message not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating message: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<MessageEntity> deleteMessage(String id) {
    if (id != null) {
      try {
        Optional<MessageEntity> messageOptional = messagesRepository.findById(
          id
        );
        if (messageOptional.isPresent()) {
          MessageEntity message = messageOptional.get();
          messagesRepository.deleteById(id);
          fileLogger.info("Message deleted with ID: " + id);
          return Optional.of(message);
        } else {
          fileLogger.warn("Message not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting message: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }
}
