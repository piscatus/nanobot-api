package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.repository.UserWalletsRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserWalletsServiceImplTest {

  @InjectMocks
  private UserWalletsServiceImpl userWalletsService;

  @Mock
  private UserWalletsRepository userWalletsRepositoryMock;

  public UserWalletsServiceImplTest() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testCreateUserWallets() {
    String generatedId = "generated-id-123";
    String userId = "abc123";
    WalletDto wallet1 = new WalletDto("XNO", "1000000000000000000000000000000");
    WalletDto wallet2 = new WalletDto("BAN", "200000000000000000000000000000");
    List<WalletDto> wallets = List.of(wallet1, wallet2);
    UserWalletsDto dto = new UserWalletsDto(userId, wallets);

    UserWalletsEntity savedEntity = new UserWalletsEntity(dto);
    savedEntity.setId(generatedId);

    when(userWalletsRepositoryMock.insert(any(UserWalletsEntity.class)))
        .thenReturn(savedEntity);

    Optional<UserWalletsEntity> result = userWalletsService.createUserWallets(dto);

    assertTrue(result.isPresent());

    UserWalletsEntity resultEntity = result.get();
    assertEquals(generatedId, resultEntity.getId());
    assertEquals(userId, resultEntity.getUserId());
    assertEquals(wallets, resultEntity.getWallets());
    verify(userWalletsRepositoryMock).insert(any(UserWalletsEntity.class));
  }

  @Test
  void testGetUsersWallets() {
    String generatedId = "generated-id-123";
    String userId = "abc123";
    WalletDto wallet1 = new WalletDto("XNO", "1000000000000000000000000000000");
    WalletDto wallet2 = new WalletDto("BAN", "200000000000000000000000000000");
    List<WalletDto> wallets = List.of(wallet1, wallet2);
    UserWalletsDto dto = new UserWalletsDto(userId, wallets);

    UserWalletsEntity savedEntity = new UserWalletsEntity(dto);
    savedEntity.setId(generatedId);

    when(userWalletsRepositoryMock.findByUserId(userId))
      .thenReturn(List.of(savedEntity));

    List<UserWalletsEntity> result = userWalletsService.getUsersWallets(userId);
    assertFalse(result.isEmpty());
    UserWalletsEntity resultEntity = result.get(0);
    assertEquals(generatedId, resultEntity.getId());
    assertEquals(userId, resultEntity.getUserId());
    assertEquals(wallets, resultEntity.getWallets());
  }

  @Test
  void testGetUserWalletsById() {
      String id = "112233";
      String userId = "validId123";
      WalletDto wallet1 = new WalletDto("XNO", "1000000000000000000000000000000");
      WalletDto wallet2 = new WalletDto("BAN", "200000000000000000000000000000");
      List<WalletDto> wallets = List.of(wallet1, wallet2);
      UserWalletsDto dto = new UserWalletsDto(userId, wallets);

      UserWalletsEntity savedEntity = new UserWalletsEntity(dto);
      savedEntity.setId(id);

      when(userWalletsRepositoryMock.findById(id))
          .thenReturn(Optional.of(savedEntity));

      Optional<UserWalletsEntity> result = userWalletsService.getUserWalletsById(id);

      assertTrue(result.isPresent());

      UserWalletsEntity resultEntity = result.get();
      assertEquals(id, resultEntity.getId());
      assertEquals(userId, resultEntity.getUserId());
      assertEquals(wallets, resultEntity.getWallets());
  }

  @Test
  void testGetUserWalletsByUserId() {
      String userId = "id1";

      UserWalletsEntity user1 = new UserWalletsEntity();
      user1.setId(userId);

      when(userWalletsRepositoryMock.findByUserId(userId))
          .thenReturn(List.of(user1));

      Optional<UserWalletsEntity> result = userWalletsService.getUserWalletsByUserId(userId);

      assertTrue(result.isPresent());
  }

  @Test
  void testUpdateUserWallets() {
      String id = "update123";
      UserWalletsEntity existing = new UserWalletsEntity();
      existing.setId(id);
      existing.setUserId("oldUser");
      WalletDto wallet1 = new WalletDto("XNO", "1000000000000000000000000000000");
      WalletDto wallet2 = new WalletDto("BAN", "200000000000000000000000000000");
      List<WalletDto> wallets = List.of(wallet1, wallet2);
      existing.setWallets(wallets);

      UserWalletsDto updatedDto = new UserWalletsDto();
      updatedDto.setUserId("newUser");
      WalletDto newWallet1 = new WalletDto("XNO", "2000000000000000000000000000000");
      WalletDto newWallet2 = new WalletDto("BAN", "400000000000000000000000000000");
      List<WalletDto> updatedWallets = List.of(newWallet1, newWallet2);
      updatedDto.setWallets(updatedWallets);

      when(userWalletsRepositoryMock.findById(id))
          .thenReturn(Optional.of(existing));
      when(userWalletsRepositoryMock.save(any(UserWalletsEntity.class)))
          .thenReturn(existing);

      Optional<UserWalletsEntity> updated = userWalletsService.updateUserWallets(id, updatedDto);

      assertTrue(updated.isPresent());
      assertEquals("newUser", updated.get().getUserId());
      assertEquals(updatedWallets, updated.get().getWallets());
  }

  @Test
  void saveWalletShouldPersistForNormalUser() {
    UserWalletsEntity entity = new UserWalletsEntity("user123");
    entity.setId("id1");
    when(userWalletsRepositoryMock.save(any(UserWalletsEntity.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    userWalletsService.saveWallet(entity);

    verify(userWalletsRepositoryMock).save(entity);
  }

  @Test
  void saveWalletShouldNotPersistForSystemUserZero() {
    UserWalletsEntity entity = new UserWalletsEntity("0");
    entity.setId("id0");

    userWalletsService.saveWallet(entity);

    verify(userWalletsRepositoryMock, never()).save(any(UserWalletsEntity.class));
  }

  @Test
  void saveWalletShouldNotPersistForBotUserId() {
    String botUserId = System.getenv("BOT_USER_ID");
    if (botUserId == null) {
      botUserId = "test-bot-user-id";
    }
    UserWalletsEntity entity = new UserWalletsEntity(botUserId);
    entity.setId("id-bot");

    userWalletsService.saveWallet(entity);

    verify(userWalletsRepositoryMock, never()).save(any(UserWalletsEntity.class));
  }

  @Test
  void testCreateUserWalletsReturnsEmptyWhenDuplicateKey() {
    UserWalletsDto dto = new UserWalletsDto("user1", List.of(new WalletDto("XNO", "100")));
    when(userWalletsRepositoryMock.insert(any(UserWalletsEntity.class)))
      .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate userId"));

    Optional<UserWalletsEntity> result = userWalletsService.createUserWallets(dto);

    assertTrue(result.isEmpty());
    verify(userWalletsRepositoryMock).insert(any(UserWalletsEntity.class));
  }

  @Test
  void testDeleteUserWallets() {
      String id = "id";
      String userId = "userId";
      UserWalletsEntity existing = new UserWalletsEntity();
      existing.setId(id);
      existing.setUserId(userId);
      WalletDto wallet1 = new WalletDto("XNO", "1000000000000000000000000000000");
      WalletDto wallet2 = new WalletDto("BAN", "200000000000000000000000000000");
      List<WalletDto> wallets = List.of(wallet1, wallet2);
      existing.setWallets(wallets);

      String newId = "toDelete";
      String newUserId = "userDelete";
      UserWalletsEntity entity = new UserWalletsEntity();
      entity.setId(id);
      entity.setUserId(userId);
      WalletDto emptyWallet1 = new WalletDto("XNO", "0");
      WalletDto emptyWallet2 = new WalletDto("BAN", "0");
      List<WalletDto> updatedWallets = List.of(emptyWallet1, emptyWallet2);
      entity.setWallets(updatedWallets);

      when(userWalletsRepositoryMock.findById(id))
          .thenReturn(Optional.of(existing));
      when(userWalletsRepositoryMock.save(any()))
          .thenReturn(entity);

      doNothing().when(userWalletsRepositoryMock).deleteById(id);

      Optional<UserWalletsEntity> result = userWalletsService.deleteUserWallets(id);

      assertTrue(result.isPresent());
      assertEquals("0", result.get().getWallets().get(0).getRaw());
      assertEquals("0", result.get().getWallets().get(1).getRaw());
  }
}
