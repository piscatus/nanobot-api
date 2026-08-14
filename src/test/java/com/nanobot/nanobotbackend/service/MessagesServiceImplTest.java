package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.entity.MessageEntity;
import com.nanobot.nanobotbackend.repository.MessagesRepository;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessagesServiceImplTest {

  @Mock
  private MessagesRepository messagesRepository;

  private MessagesServiceImpl messagesService;

  @BeforeEach
  void setUp() {
    messagesService = new MessagesServiceImpl(messagesRepository);
  }

  @Test
  void createMessageShouldReturnCreatedEntity() {
    MessageDto dto = new MessageDto("user1", "guild1", "ch1", "msg1", "Title", "#000", "Content", new Date(), "http://example.com", null, null);
    MessageEntity entity = new MessageEntity(dto);
    entity.setId("id-1");
    when(messagesRepository.insert(any(MessageEntity.class))).thenReturn(entity);

    MessageEntity result = messagesService.createMessage(dto);

    assertNotNull(result);
    assertEquals("id-1", result.getId());
    verify(messagesRepository).insert(any(MessageEntity.class));
  }

  @Test
  void getMessagesShouldReturnList() {
    MessageEntity entity = new MessageEntity();
    entity.setId("id-1");
    when(messagesRepository.findAllSortedByOldestTimestamp()).thenReturn(List.of(entity));

    List<MessageEntity> result = messagesService.getMessages();

    assertFalse(result.isEmpty());
    assertEquals("id-1", result.get(0).getId());
    verify(messagesRepository).findAllSortedByOldestTimestamp();
  }

  @Test
  void getMessageByIdShouldReturnPresentWhenFound() {
    MessageEntity entity = new MessageEntity();
    entity.setId("id-1");
    when(messagesRepository.findById("id-1")).thenReturn(Optional.of(entity));

    Optional<MessageEntity> result = messagesService.getMessageById("id-1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(messagesRepository).findById("id-1");
  }

  @Test
  void getMessageByIdShouldReturnEmptyWhenNotFound() {
    when(messagesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<MessageEntity> result = messagesService.getMessageById("missing");

    assertTrue(result.isEmpty());
    verify(messagesRepository).findById("missing");
  }

  @Test
  void getMessageByIdShouldReturnEmptyWhenIdIsNull() {
    Optional<MessageEntity> result = messagesService.getMessageById(null);

    assertTrue(result.isEmpty());
    verify(messagesRepository, never()).findById(any());
  }

  @Test
  void updateMessageShouldReturnUpdatedWhenFound() {
    MessageEntity existing = new MessageEntity();
    existing.setId("id-1");
    MessageDto dto = new MessageDto("user2", "guild2", "ch2", "msg2", "New Title", "#fff", "New Content", new Date(), null, null, null);
    when(messagesRepository.findById("id-1")).thenReturn(Optional.of(existing));
    when(messagesRepository.save(any(MessageEntity.class))).thenReturn(existing);

    Optional<MessageEntity> result = messagesService.updateMessage("id-1", dto);

    assertTrue(result.isPresent());
    verify(messagesRepository).findById("id-1");
    verify(messagesRepository).save(any(MessageEntity.class));
  }

  @Test
  void updateMessageShouldReturnEmptyWhenNotFound() {
    MessageDto dto = new MessageDto("user2", null, null, null, null, null, null, null, null, null, null);
    when(messagesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<MessageEntity> result = messagesService.updateMessage("missing", dto);

    assertTrue(result.isEmpty());
    verify(messagesRepository).findById("missing");
    verify(messagesRepository, never()).save(any());
  }

  @Test
  void deleteMessageShouldReturnDeletedWhenFound() {
    MessageEntity entity = new MessageEntity();
    entity.setId("id-1");
    when(messagesRepository.findById("id-1")).thenReturn(Optional.of(entity));
    doNothing().when(messagesRepository).deleteById("id-1");

    Optional<MessageEntity> result = messagesService.deleteMessage("id-1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(messagesRepository).findById("id-1");
    verify(messagesRepository).deleteById("id-1");
  }

  @Test
  void deleteMessageShouldReturnEmptyWhenNotFound() {
    when(messagesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<MessageEntity> result = messagesService.deleteMessage("missing");

    assertTrue(result.isEmpty());
    verify(messagesRepository).findById("missing");
    verify(messagesRepository, never()).deleteById(any());
  }
}
