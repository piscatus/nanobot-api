package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PickupServicesImplTest {

  @Mock
  private CoreServices coreServices;

  @Mock
  private DropsService dropsService;

  @Mock
  private PickupsService pickupsService;

  private PickupServicesImpl pickupServicesImpl;

  @BeforeEach
  void setUp() {
    pickupServicesImpl =
      new PickupServicesImpl(coreServices, dropsService, pickupsService);
  }

  @Test
  void pickupShouldReturnDropNotFoundWhenDropsEmpty() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    when(dropsService.getDrops("drop1")).thenReturn(Collections.emptyList());

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertEquals("Drop not found.", result.getErrorMessage());
    verify(dropsService).getDrops("drop1");
  }

  @Test
  void pickupShouldReturnDropEndedWhenEndTimePassed() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    DropEntity drop = validDrop("owner1");
    drop.setEndTime(new Date(System.currentTimeMillis() - 60_000));

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertEquals("This drop has ended.", result.getErrorMessage());
  }

  @Test
  void pickupShouldReturnErrorWhenUserEntersOwnDrop() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    DropEntity drop = validDrop("user1");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null)).thenReturn(Collections.emptyList());

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertEquals(
      "You cannot enter your own drop!\n\nUsers Joined: 0",
      result.getErrorMessage()
    );
  }

  @Test
  void pickupShouldReturnErrorWhenUserAlreadyEntered() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    DropEntity drop = validDrop("owner1");
    PickupEntity existingPickup = new PickupEntity();
    existingPickup.setUserId("user1");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null))
      .thenReturn(List.of(existingPickup));

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertEquals(
      "You have already entered this drop!\n\nUsers Joined: 1",
      result.getErrorMessage()
    );
  }

  @Test
  void pickupShouldReturnErrorWhenRequiredRoleNotHeld() {
    RequestDto request = request("drop1", "user1", List.of("role-other"));
    DropEntity drop = validDrop("owner1");
    drop.setRequiredRole("role-required");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null)).thenReturn(Collections.emptyList());

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertEquals(
      "You do not hold the <@&role-required> role required to enter, sorry!\n\nUsers Joined: 0",
      result.getErrorMessage()
    );
  }

  @Test
  void pickupShouldSucceedWhenRequiredRoleIsZero() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    DropEntity drop = validDrop("owner1");
    drop.setRequiredRole("0");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null)).thenReturn(Collections.emptyList());
    when(pickupsService.createPickup(any(PickupDto.class)))
      .thenReturn(Optional.of(new PickupEntity()));

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertNull(result.getErrorMessage());
    verify(pickupsService).createPickup(any(PickupDto.class));
  }

  @Test
  void pickupShouldSucceedWhenUserHasRequiredRole() {
    RequestDto request = request("drop1", "user1", List.of("role-required"));
    DropEntity drop = validDrop("owner1");
    drop.setRequiredRole("role-required");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null)).thenReturn(Collections.emptyList());
    when(pickupsService.createPickup(any(PickupDto.class)))
      .thenReturn(Optional.of(new PickupEntity()));

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertNull(result.getErrorMessage());
    verify(pickupsService).createPickup(any(PickupDto.class));
  }

  @Test
  void pickupShouldReturnErrorWhenMaxEntriesReached() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    DropEntity drop = validDrop("owner1");
    drop.setMaximumEntries("1");

    PickupEntity existingPickup = new PickupEntity();
    existingPickup.setUserId("other-user");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null))
      .thenReturn(List.of(existingPickup));

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertEquals(
      "This drop has reached its maximum number of entries.",
      result.getErrorMessage()
    );
  }

  @Test
  void pickupShouldReturnErrorWhenCreatePickupReturnsEmpty() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    DropEntity drop = validDrop("owner1");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null)).thenReturn(Collections.emptyList());
    when(pickupsService.createPickup(any(PickupDto.class)))
      .thenReturn(Optional.empty());

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertEquals(
      "Error joining drop, please try again later.",
      result.getErrorMessage()
    );
  }

  @Test
  void pickupShouldReturnSuccessWhenCreatePickupSucceeds() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    DropEntity drop = validDrop("owner1");
    PickupEntity created = new PickupEntity();
    created.setId("pickup-1");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null)).thenReturn(Collections.emptyList());
    when(pickupsService.createPickup(any(PickupDto.class)))
      .thenReturn(Optional.of(created));

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertNull(result.getErrorMessage());
    verify(pickupsService).createPickup(any(PickupDto.class));
  }

  @Test
  void pickupShouldEndDropWhenLastEntryFillsMax() {
    RequestDto request = request("drop1", "user1", Collections.emptyList());
    DropEntity drop = validDrop("owner1");
    drop.setMaximumEntries("2");
    drop.setId("drop-entity-id");

    PickupEntity existingPickup = new PickupEntity();
    existingPickup.setUserId("other-user");

    when(dropsService.getDrops("drop1")).thenReturn(List.of(drop));
    when(pickupsService.getPickups("drop1", null))
      .thenReturn(List.of(existingPickup));
    when(pickupsService.createPickup(any(PickupDto.class)))
      .thenReturn(Optional.of(new PickupEntity()));
    when(dropsService.updateDrop(eq("drop-entity-id"), any(DropDto.class)))
      .thenReturn(Optional.of(drop));

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertNull(result.getErrorMessage());
    verify(dropsService).updateDrop(eq("drop-entity-id"), any(DropDto.class));
  }

  @Test
  void pickupShouldReturnUnknownErrorWhenExceptionThrown() {
    RequestDto request = request("drop1", "user1", null);
    when(dropsService.getDrops("drop1")).thenThrow(new RuntimeException("DB error"));

    BaseResponseDto result = pickupServicesImpl.pickup(request);

    assertEquals(
      "An unknown error occured, please try again or contact support. Sorry for the inconvenience!",
      result.getErrorMessage()
    );
  }

  private RequestDto request(String dropId, String userId, List<String> userRoles) {
    return new RequestDto(
      null,
      null,
      false,
      dropId,
      null,
      false,
      "guild1",
      null,
      null,
      null,
      null,
      null,
      userId,
      null,
      userRoles != null ? userRoles : new ArrayList<>(),
      null
    );
  }

  private DropEntity validDrop(String ownerId) {
    DropEntity drop = new DropEntity();
    drop.setId("drop-entity-id");
    drop.setUserId(ownerId);
    drop.setEndTime(new Date(System.currentTimeMillis() + 60_000));
    drop.setMaximumEntries("100");
    drop.setRequiredRole("0");
    return drop;
  }
}
