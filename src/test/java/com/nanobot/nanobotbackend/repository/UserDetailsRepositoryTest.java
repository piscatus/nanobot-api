package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class UserDetailsRepositoryTest {

  @Autowired
  private UserDetailsRepository userDetailsRepository;

  @BeforeEach
  void setUp() {
    userDetailsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoUserDetails() {
    List<UserDetailsEntity> result = userDetailsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedUserDetails() {
    UserDetailsEntity user = new UserDetailsEntity();
    user.setUserId("u1");
    user.setStatus(StatusDto.ACTIVE);
    user.setSeed("seed1");

    UserDetailsEntity saved = userDetailsRepository.insert(user);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<UserDetailsEntity> all = userDetailsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("u1", all.get(0).getUserId());
    assertEquals(StatusDto.ACTIVE, all.get(0).getStatus());
  }

  @Test
  void findByUserIdReturnsMatchingUserDetails() {
    UserDetailsEntity u1 = new UserDetailsEntity();
    u1.setUserId("u1");
    u1.setStatus(StatusDto.ACTIVE);
    userDetailsRepository.insert(u1);

    UserDetailsEntity u2 = new UserDetailsEntity();
    u2.setUserId("u2");
    u2.setStatus(StatusDto.LOCKED);
    userDetailsRepository.insert(u2);

    List<UserDetailsEntity> byU1 = userDetailsRepository.findByUserId("u1");
    assertEquals(1, byU1.size());
    assertEquals("u1", byU1.get(0).getUserId());
    assertEquals(StatusDto.ACTIVE, byU1.get(0).getStatus());
  }

  @Test
  void findByUserIdWithNullReturnsAll() {
    UserDetailsEntity user = new UserDetailsEntity();
    user.setUserId("u1");
    user.setStatus(StatusDto.ACTIVE);
    userDetailsRepository.insert(user);

    List<UserDetailsEntity> result = userDetailsRepository.findByUserId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findActiveUsersReturnsOnlyActive() {
    UserDetailsEntity active = new UserDetailsEntity();
    active.setUserId("u1");
    active.setStatus(StatusDto.ACTIVE);
    userDetailsRepository.insert(active);

    UserDetailsEntity locked = new UserDetailsEntity();
    locked.setUserId("u2");
    locked.setStatus(StatusDto.LOCKED);
    userDetailsRepository.insert(locked);

    List<UserDetailsEntity> activeList = userDetailsRepository.findActiveUsers();
    assertEquals(1, activeList.size());
    assertEquals(StatusDto.ACTIVE, activeList.get(0).getStatus());
  }

  @Test
  void findLockedUsersReturnsOnlyLocked() {
    UserDetailsEntity locked = new UserDetailsEntity();
    locked.setUserId("u1");
    locked.setStatus(StatusDto.LOCKED);
    userDetailsRepository.insert(locked);

    UserDetailsEntity active = new UserDetailsEntity();
    active.setUserId("u2");
    active.setStatus(StatusDto.ACTIVE);
    userDetailsRepository.insert(active);

    List<UserDetailsEntity> lockedList =
      userDetailsRepository.findLockedUsers();
    assertEquals(1, lockedList.size());
    assertEquals(StatusDto.LOCKED, lockedList.get(0).getStatus());
  }

  @Test
  void findBannedUsersReturnsOnlyBanned() {
    UserDetailsEntity banned = new UserDetailsEntity();
    banned.setUserId("u1");
    banned.setStatus(StatusDto.BANNED);
    userDetailsRepository.insert(banned);

    UserDetailsEntity active = new UserDetailsEntity();
    active.setUserId("u2");
    active.setStatus(StatusDto.ACTIVE);
    userDetailsRepository.insert(active);

    List<UserDetailsEntity> bannedList = userDetailsRepository.findBannedUsers();
    assertEquals(1, bannedList.size());
    assertEquals(StatusDto.BANNED, bannedList.get(0).getStatus());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    UserDetailsEntity user = new UserDetailsEntity();
    user.setUserId("u1");
    user.setStatus(StatusDto.ACTIVE);
    UserDetailsEntity saved = userDetailsRepository.insert(user);

    var found = userDetailsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("u1", found.get().getUserId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = userDetailsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    UserDetailsEntity user = new UserDetailsEntity();
    user.setUserId("u1");
    user.setStatus(StatusDto.ACTIVE);
    UserDetailsEntity saved = userDetailsRepository.insert(user);

    saved.setStatus(StatusDto.LOCKED);
    UserDetailsEntity updated = userDetailsRepository.save(saved);

    assertEquals(StatusDto.LOCKED, updated.getStatus());
  }

  @Test
  void deleteByIdRemovesEntity() {
    UserDetailsEntity user = new UserDetailsEntity();
    user.setUserId("u1");
    user.setStatus(StatusDto.ACTIVE);
    UserDetailsEntity saved = userDetailsRepository.insert(user);

    userDetailsRepository.deleteById(saved.getId());

    var found = userDetailsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
