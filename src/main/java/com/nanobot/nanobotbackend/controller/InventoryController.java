package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserItemsResponseDto;
import com.nanobot.nanobotbackend.service.InventoryServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests")
public class InventoryController {

  private final InventoryServices inventoryServices;

  public InventoryController(InventoryServices inventoryServices) {
    this.inventoryServices = inventoryServices;
  }

  @PostMapping("/inventory")
  public ResponseEntity<UserItemsResponseDto> inventory(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      inventoryServices.inventory(requestData),
      HttpStatus.OK
    );
  }
}
