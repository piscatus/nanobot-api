package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface DropsService {
  Optional<DropEntity> createDrop(DropDto dropDto);

  List<DropEntity> getDrops(String messageId);

  Optional<DropEntity> getDropById(String id);

  Optional<DropEntity> getDropByMessageId(String messageId);

  Optional<DropEntity> updateDrop(String id, DropDto dropDto);

  Optional<DropEntity> updateDropMessageId(RequestDto requestDto);

  Optional<DropEntity> deleteDrop(String id);

  void setDrops(Consumer<List<DropDto>> setDrops);
}
