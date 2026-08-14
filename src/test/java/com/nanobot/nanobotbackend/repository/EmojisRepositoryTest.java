package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.EmojiEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class EmojisRepositoryTest {

  @Autowired
  private EmojisRepository emojisRepository;

  @BeforeEach
  void setUp() {
    emojisRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoEmojis() {
    List<EmojiEntity> result = emojisRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedEmoji() {
    EmojiEntity emoji = new EmojiEntity();
    emoji.setCategory("fish");
    emoji.setName("nano");
    emoji.setEmoji(":nano:");

    EmojiEntity saved = emojisRepository.insert(emoji);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<EmojiEntity> all = emojisRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("fish", all.get(0).getCategory());
    assertEquals("nano", all.get(0).getName());
  }

  @Test
  void findByCategoryReturnsMatchingEmojis() {
    EmojiEntity e1 = new EmojiEntity();
    e1.setCategory("fish");
    e1.setName("nano");
    emojisRepository.insert(e1);

    EmojiEntity e2 = new EmojiEntity();
    e2.setCategory("fish");
    e2.setName("mega");
    emojisRepository.insert(e2);

    EmojiEntity e3 = new EmojiEntity();
    e3.setCategory("currency");
    e3.setName("xno");
    emojisRepository.insert(e3);

    List<EmojiEntity> byFish = emojisRepository.findByCategory("fish");
    assertEquals(2, byFish.size());
    assertTrue(byFish.stream().allMatch(e -> "fish".equals(e.getCategory())));
  }

  @Test
  void findByNameReturnsMatchingEmojis() {
    EmojiEntity emoji = new EmojiEntity();
    emoji.setCategory("fish");
    emoji.setName("nano");
    emojisRepository.insert(emoji);

    List<EmojiEntity> result = emojisRepository.findByName("nano");
    assertEquals(1, result.size());
    assertEquals("nano", result.get(0).getName());
  }

  @Test
  void findByCategoryAndNameReturnsMatchingEmoji() {
    EmojiEntity emoji = new EmojiEntity();
    emoji.setCategory("fish");
    emoji.setName("nano");
    emoji.setEmoji(":nano:");
    emojisRepository.insert(emoji);

    List<EmojiEntity> result =
      emojisRepository.findByCategoryAndName("fish", "nano");
    assertEquals(1, result.size());
    assertEquals("fish", result.get(0).getCategory());
    assertEquals("nano", result.get(0).getName());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    EmojiEntity emoji = new EmojiEntity();
    emoji.setCategory("fish");
    emoji.setName("nano");
    EmojiEntity saved = emojisRepository.insert(emoji);

    var found = emojisRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("nano", found.get().getName());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = emojisRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    EmojiEntity emoji = new EmojiEntity();
    emoji.setCategory("fish");
    emoji.setName("nano");
    EmojiEntity saved = emojisRepository.insert(emoji);

    saved.setEmoji(":new_nano:");
    EmojiEntity updated = emojisRepository.save(saved);

    assertEquals(":new_nano:", updated.getEmoji());
  }

  @Test
  void deleteByIdRemovesEntity() {
    EmojiEntity emoji = new EmojiEntity();
    emoji.setCategory("fish");
    emoji.setName("nano");
    EmojiEntity saved = emojisRepository.insert(emoji);

    emojisRepository.deleteById(saved.getId());

    var found = emojisRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
