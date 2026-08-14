package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.service.QueuesService;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("queues")
public class QueuesController {

  private QueuesService queuesService;

  public QueuesController(QueuesService queuesService) {
    this.queuesService = queuesService;
  }

  @PostMapping
  public ResponseEntity<QueueEntity> createQueue(
    @RequestBody QueueDto newQueueDto
  ) {
    return new ResponseEntity<>(
      queuesService.createQueue(newQueueDto),
      HttpStatus.CREATED
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getQueueById(
    @PathVariable String id
  ) {
    Optional<QueueEntity> queues = queuesService.getQueueById(id);

    if (queues.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, "Queue ID not found", "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(queues), HttpStatus.OK);
  }

  @GetMapping
  public ResponseEntity<List<QueueEntity>> getQueues() {
    return new ResponseEntity<>(queuesService.getQueues(), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateQueue(
    @PathVariable String id,
    @RequestBody QueueDto updatedQueueDto
  ) {
    Optional<QueueEntity> queues = queuesService.updateQueue(
      id,
      updatedQueueDto
    );

    if (queues.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, "Queue ID not found", "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(queues), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteQueue(@PathVariable String id) {
    Optional<QueueEntity> queues = queuesService.deleteQueue(id);

    if (queues.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, "Queue id not found", "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
