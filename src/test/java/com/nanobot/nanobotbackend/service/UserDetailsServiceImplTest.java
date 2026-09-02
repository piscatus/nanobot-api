package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.repository.UserDetailsRepository;
import com.nanobot.nanobotbackend.service.chain.DepositAddressResolver;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDetailsServiceImplTest {

  @InjectMocks
  private UserDetailsServiceImpl userDetailsService;

  @Mock
  private UserDetailsRepository userDetailsRepositoryMock;

  @Mock
  private DepositAddressResolver depositAddressResolverMock;

  public UserDetailsServiceImplTest() {
    MockitoAnnotations.openMocks(this);
    // Address generation now goes through the chain adapter. For Nano the
    // adapter derives from the user's seed, so delegate to the same helper it
    // uses and keep these tests covering the derivation rules.
    when(depositAddressResolverMock.resolve(any(UserDetailsDto.class), anyString()))
      .thenAnswer(invocation ->
        CryptoUtil.deriveAddress(
          (UserDetailsDto) invocation.getArgument(0),
          invocation.getArgument(1)
        )
      );
  }

  @Test
  void testGenerateAddresses() {
    String userId = "123";
    String unusedSeed = 
      "3D1B8B0003EEEB65C4782D24231A3EEA8A795E2CAFA1B86EFEBB82255CFEC224";

    UserDetailsEntity mockEntity = new UserDetailsEntity();
    mockEntity.setId("abc123");
    mockEntity.setUserId(userId);
    mockEntity.setSeed(unusedSeed);
    mockEntity.setSubordinateUserId("321");
    mockEntity.setStatus(StatusDto.ACTIVE);

    UserDetailsDto mockDto = new UserDetailsDto(mockEntity);

    when(userDetailsRepositoryMock.findByUserId(userId))
      .thenReturn(List.of(mockEntity));

    when(userDetailsRepositoryMock.findByUserId(userId))
      .thenReturn(List.of(mockEntity));

    List<CurrencyDto> currencies = List.of(
      new CurrencyDto("XNO", "Nano", true),
      new CurrencyDto("BAN", "Banano", true)
    );

    List<WalletDto> testAddresses =
      userDetailsService.generateAddresses(currencies, mockDto);
    
    assertNotNull(testAddresses);
    assertFalse(testAddresses.isEmpty()); 

    WalletDto address1 = testAddresses.get(0);
    WalletDto address2 = testAddresses.get(1);
    assertEquals(address1.getTicker(), "XNO");
    assertEquals(
      address1.getAddress(), 
      "nano_3nesn4knc35xzeizidwxoxmatirmytcc3crctax8k5uy6fqi9xzxnqcmgizq"
    );
    assertEquals(address2.getTicker(), "BAN");
    assertEquals(
      address2.getAddress(), 
      "ban_3nesn4knc35xzeizidwxoxmatirmytcc3crctax8k5uy6fqi9xzxnqcmgizq"
    );
  }

  @Test
  void generateAddressesUsesCustomIndex() {
    String seed =
      "3D1B8B0003EEEB65C4782D24231A3EEA8A795E2CAFA1B86EFEBB82255CFEC224";
    UserDetailsDto mockDto = new UserDetailsDto();
    mockDto.setSeed(seed);
    mockDto.setIndex(1L);

    List<WalletDto> addresses = userDetailsService.generateAddresses(
      List.of(new CurrencyDto("XNO", "Nano", true)),
      mockDto
    );

    assertEquals(1, addresses.size());
    assertNotEquals(
      "nano_3nesn4knc35xzeizidwxoxmatirmytcc3crctax8k5uy6fqi9xzxnqcmgizq",
      addresses.get(0).getAddress()
    );
    assertEquals(
      CryptoUtil.deriveAddress(seed, 1L, null, "XNO"),
      addresses.get(0).getAddress()
    );
  }

  @Test
  void generateAddressesPrefersPrivateKeyOverSeedAndIndex() {
    String seed =
      "3D1B8B0003EEEB65C4782D24231A3EEA8A795E2CAFA1B86EFEBB82255CFEC224";
    String privateKey =
      "0000000000000000000000000000000000000000000000000000000000000002";
    UserDetailsDto mockDto = new UserDetailsDto();
    mockDto.setSeed(seed);
    mockDto.setIndex(1L);
    mockDto.setPrivateKey(privateKey);

    List<WalletDto> addresses = userDetailsService.generateAddresses(
      List.of(
        new CurrencyDto("XNO", "Nano", true),
        new CurrencyDto("BAN", "Banano", true)
      ),
      mockDto
    );

    String expectedNano = CryptoUtil.deriveAddress(null, null, privateKey, "XNO");
    assertEquals(expectedNano, addresses.get(0).getAddress());
    assertEquals(
      expectedNano.replace("nano", "ban"),
      addresses.get(1).getAddress()
    );
    assertNotEquals(
      "nano_3nesn4knc35xzeizidwxoxmatirmytcc3crctax8k5uy6fqi9xzxnqcmgizq",
      addresses.get(0).getAddress()
    );
  }

  @Test
  void testCreateUserDetails() {
    String generatedId = "generated-id-123";
    String userId = "user123";
    String seed = "seed123";
    String subordinateUserId = "sub456";
    StatusDto status = StatusDto.ACTIVE;

    UserDetailsDto dto = new UserDetailsDto();
    dto.setUserId(userId);
    dto.setSeed(seed);
    dto.setSubordinateUserId(subordinateUserId);
    dto.setStatus(status);

    UserDetailsEntity savedEntity = new UserDetailsEntity(dto);
    savedEntity.setId(generatedId);

    when(userDetailsRepositoryMock.insert(any(UserDetailsEntity.class)))
        .thenReturn(savedEntity);

    Optional<UserDetailsEntity> result = userDetailsService.createUserDetails(dto);

    assertTrue(result.isPresent());

    UserDetailsEntity resultEntity = result.get();
    assertEquals(generatedId, resultEntity.getId());
    assertEquals(userId, resultEntity.getUserId());
    assertEquals(seed, resultEntity.getSeed());
    assertEquals(subordinateUserId, resultEntity.getSubordinateUserId());
    assertEquals(status, resultEntity.getStatus());

    verify(userDetailsRepositoryMock).insert(any(UserDetailsEntity.class));
  }

  @Test
  void testGetUsersDetails() {
    String id = "abc123";
    String userId = "123";
    String subordinateUserId = "321";
    StatusDto status = StatusDto.ACTIVE;

    UserDetailsDto dto = new UserDetailsDto(userId, subordinateUserId, status, null);

    UserDetailsEntity mockEntity = new UserDetailsEntity(dto);
    mockEntity.setId(id);

    when(userDetailsRepositoryMock.findByUserId(userId))
      .thenReturn(List.of(mockEntity));

    List<UserDetailsEntity> result = userDetailsService.getUsersDetails(userId);
    assertFalse(result.isEmpty());
    UserDetailsEntity resultEntity = result.get(0);
    assertEquals(id, resultEntity.getId());
    assertEquals(userId, resultEntity.getUserId());
    assertEquals(subordinateUserId, resultEntity.getSubordinateUserId());
    assertEquals(status, resultEntity.getStatus());
  }

  @Test
  void testGetUserDetailsById() {
      String id = "112233";
      String userId = "validId123";
      StatusDto status = StatusDto.ACTIVE;

      UserDetailsEntity mockEntity = new UserDetailsEntity();
      mockEntity.setId(id);
      mockEntity.setUserId(userId);
      mockEntity.setStatus(status);

      when(userDetailsRepositoryMock.findById(id))
          .thenReturn(Optional.of(mockEntity));

      Optional<UserDetailsEntity> result = userDetailsService.getUserDetailsById(id);

      assertTrue(result.isPresent());

      UserDetailsEntity resultEntity = result.get();
      assertEquals(id, resultEntity.getId());
      assertEquals(userId, resultEntity.getUserId());
      assertEquals(status, resultEntity.getStatus());
  }

  @Test
  void testGetUserDetailsByUserId() {
      String userId = "id1";

      UserDetailsEntity user1 = new UserDetailsEntity();
      user1.setId(userId);

      when(userDetailsRepositoryMock.findByUserId(userId))
          .thenReturn(List.of(user1));

      Optional<UserDetailsEntity> result = userDetailsService.getUserDetailsByUserId(userId);

      assertTrue(result.isPresent());
  }

  @Test
  void testGetActiveUsersDetails() {
      UserDetailsEntity user1 = new UserDetailsEntity();
      user1.setId("1");
      user1.setUserId("active1");
      user1.setStatus(StatusDto.ACTIVE);

      when(userDetailsRepositoryMock.findActiveUsers())
          .thenReturn(List.of(user1));

      List<UserDetailsEntity> activeUsers = userDetailsService.getActiveUsersDetails();

      assertNotNull(activeUsers);
      assertEquals(1, activeUsers.size());
      assertEquals(StatusDto.ACTIVE, activeUsers.get(0).getStatus());
  }

  @Test
  void testGetLockedUsersDetails() {
      UserDetailsEntity lockedUser = new UserDetailsEntity();
      lockedUser.setId("3");
      lockedUser.setUserId("lockedUser");
      lockedUser.setStatus(StatusDto.LOCKED);

      when(userDetailsRepositoryMock.findLockedUsers())
          .thenReturn(List.of(lockedUser));

      List<UserDetailsEntity> lockedUsers = userDetailsService.getLockedUsersDetails();

      assertNotNull(lockedUsers);
      assertEquals(1, lockedUsers.size());
      assertEquals(StatusDto.LOCKED, lockedUsers.get(0).getStatus());
  }

  @Test
  void testGetBannedUsersDetails() {
      UserDetailsEntity bannedUser = new UserDetailsEntity();
      bannedUser.setId("2");
      bannedUser.setUserId("bannedUser");
      bannedUser.setStatus(StatusDto.BANNED);

      when(userDetailsRepositoryMock.findBannedUsers())
          .thenReturn(List.of(bannedUser));

      List<UserDetailsEntity> bannedUsers = userDetailsService.getBannedUsersDetails();

      assertNotNull(bannedUsers);
      assertEquals(1, bannedUsers.size());
      assertEquals(StatusDto.BANNED, bannedUsers.get(0).getStatus());
  }

  @Test
  void testUpdateUserDetails() {
      String id = "update123";
      UserDetailsEntity existing = new UserDetailsEntity();
      existing.setId(id);
      existing.setUserId("oldUser");
      existing.setSeed("oldSeed");
      existing.setStatus(StatusDto.LOCKED);

      UserDetailsDto updatedDto = new UserDetailsDto();
      updatedDto.setUserId("newUser");
      updatedDto.setSeed("newSeed");
      updatedDto.setIndex(12L);
      updatedDto.setPrivateKey("newPk");
      updatedDto.setStatus(StatusDto.ACTIVE);
      updatedDto.setSubordinateUserId("sub999");

      when(userDetailsRepositoryMock.findById(id))
          .thenReturn(Optional.of(existing));
      when(userDetailsRepositoryMock.save(any(UserDetailsEntity.class)))
          .thenReturn(existing);

      Optional<UserDetailsEntity> updated = userDetailsService.updateUserDetails(id, updatedDto);

      assertTrue(updated.isPresent());
      assertEquals("newUser", updated.get().getUserId());
      assertEquals("newSeed", updated.get().getSeed());
      assertEquals(12L, updated.get().getIndex());
      assertEquals("newPk", updated.get().getPrivateKey());
      assertEquals(StatusDto.ACTIVE, updated.get().getStatus());
  }

  @Test
  void testDeleteUserDetails() {
      String id = "toDelete";

      UserDetailsEntity entity = new UserDetailsEntity();
      entity.setId(id);
      entity.setUserId(id);
      entity.setStatus(StatusDto.ACTIVE);

      // Mock findById to return the user
      when(userDetailsRepositoryMock.findById(id)).thenReturn(Optional.of(entity));

      // For void method, use doNothing
      doNothing().when(userDetailsRepositoryMock).deleteById(id);

      Optional<UserDetailsEntity> deleted = userDetailsService.deleteUserDetails(id);

      assertTrue(deleted.isPresent());
      assertEquals(id, deleted.get().getId());

      verify(userDetailsRepositoryMock).findById(id);
      verify(userDetailsRepositoryMock).deleteById(id);
  }

  @Test
  void testGetOrCreateUserDetails() {
      String id = "id123";
      String userId = "existingUser";
      String seed = "seed123";
      StatusDto status = StatusDto.ACTIVE;
      UserDetailsEntity existing = new UserDetailsEntity();
      existing.setId(id);
      existing.setUserId(userId);
      existing.setSeed(seed);
      existing.setStatus(status);

      when(userDetailsRepositoryMock.findByUserId(userId))
          .thenReturn(List.of(existing));

      UserDetailsEntity result = userDetailsService.getOrCreateUserDetails(userId);

      assertNotNull(result);
      assertEquals(id, result.getId());
      assertEquals(userId, result.getUserId());
      assertEquals(seed, result.getSeed());
      assertEquals(status, result.getStatus());
      verify(userDetailsRepositoryMock, never()).save(any());
  }
}