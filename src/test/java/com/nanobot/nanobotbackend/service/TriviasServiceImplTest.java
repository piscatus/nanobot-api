package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.TriviaBulkResultDto;
import com.nanobot.nanobotbackend.dto.TriviaDto;
import com.nanobot.nanobotbackend.entity.TriviaEntity;
import com.nanobot.nanobotbackend.repository.TriviasRepository;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ExtendWith(MockitoExtension.class)
class TriviasServiceImplTest {

  @Mock
  private TriviasRepository triviasRepository;

  @Mock
  private MongoTemplate mongoTemplate;

  private TriviasServiceImpl triviasService;

  @BeforeEach
  void setUp() {
    triviasService = new TriviasServiceImpl(triviasRepository, mongoTemplate);
  }

  @Test
  void normalizeQuestionAndHashQuestionAreDeterministic() {
    assertEquals(
      "what is x?",
      TriviasServiceImpl.normalizeQuestion("  What   is X? ")
    );
    assertEquals(
      TriviasServiceImpl.hashQuestion("  What   is X? "),
      TriviasServiceImpl.hashQuestion("what is x?")
    );
    assertEquals(
      "6e22679a137eaa792e8b1fd28c37c1547f6c9b89",
      TriviasServiceImpl.hashQuestion("what is x?")
    );
  }

  @Test
  void validateTriviaReturnsMissingBody() {
    assertEquals("Trivia body is missing.", triviasService.validateTrivia(null));
  }

  @Test
  void validateTriviaReturnsMissingCategory() {
    TriviaDto dto = validTrivia();
    dto.setCategory("  ");
    assertEquals("category is required.", triviasService.validateTrivia(dto));
  }

  @Test
  void validateTriviaReturnsMissingQuestion() {
    TriviaDto dto = validTrivia();
    dto.setQuestion(null);
    assertEquals("question is required.", triviasService.validateTrivia(dto));
  }

  @Test
  void validateTriviaReturnsQuestionTooLong() {
    TriviaDto dto = validTrivia();
    dto.setQuestion("q".repeat(Constants.maximumTriviaQuestionLength + 1));
    assertEquals(
      "question is longer than " +
      Constants.maximumTriviaQuestionLength +
      " characters.",
      triviasService.validateTrivia(dto)
    );
  }

  @Test
  void validateTriviaReturnsMissingCorrectAnswer() {
    TriviaDto dto = validTrivia();
    dto.setCorrectAnswer("");
    assertEquals(
      "correctAnswer is required.",
      triviasService.validateTrivia(dto)
    );
  }

  @Test
  void validateTriviaReturnsMissingIncorrectAnswers() {
    TriviaDto dto = validTrivia();
    dto.setIncorrectAnswers(List.of());
    assertEquals(
      "incorrectAnswers must have at least one answer.",
      triviasService.validateTrivia(dto)
    );
    dto.setIncorrectAnswers(null);
    assertEquals(
      "incorrectAnswers must have at least one answer.",
      triviasService.validateTrivia(dto)
    );
  }

  @Test
  void validateTriviaReturnsTooManyAnswers() {
    TriviaDto dto = validTrivia();
    dto.setIncorrectAnswers(List.of("A", "B", "C", "D"));
    assertEquals(
      "A question can have at most " +
      Constants.maximumTriviaAnswers +
      " answers (Discord shows one button per answer).",
      triviasService.validateTrivia(dto)
    );
  }

  @Test
  void validateTriviaReturnsBlankAnswer() {
    TriviaDto dto = validTrivia();
    dto.setIncorrectAnswers(List.of("  "));
    assertEquals("Answers cannot be blank.", triviasService.validateTrivia(dto));
  }

  @Test
  void validateTriviaReturnsAnswerLongerThan80Characters() {
    String tooLong = "x".repeat(Constants.maximumTriviaAnswerLength + 1);
    TriviaDto dto = validTrivia();
    dto.setIncorrectAnswers(List.of(tooLong));
    assertEquals(
      "Answer `" +
      tooLong +
      "` is longer than " +
      Constants.maximumTriviaAnswerLength +
      " characters, the Discord button label limit. Reword or drop it.",
      triviasService.validateTrivia(dto)
    );
  }

  @Test
  void validateTriviaReturnsDuplicateAnswer() {
    TriviaDto dto = validTrivia();
    dto.setCorrectAnswer("Paris");
    dto.setIncorrectAnswers(List.of("London", "paris"));
    assertEquals(
      "Answers must be distinct: `paris` repeats.",
      triviasService.validateTrivia(dto)
    );
  }

  @Test
  void validateTriviaReturnsBadType() {
    TriviaDto dto = validTrivia();
    dto.setType("foo");
    assertEquals(
      "type must be `multiple` or `boolean`.",
      triviasService.validateTrivia(dto)
    );
  }

  @Test
  void validateTriviaReturnsBadDifficulty() {
    TriviaDto dto = validTrivia();
    dto.setDifficulty("extreme");
    assertEquals(
      "difficulty must be `easy`, `medium` or `hard`.",
      triviasService.validateTrivia(dto)
    );
  }

  @Test
  void validateTriviaReturnsNullWhenValid() {
    assertNull(triviasService.validateTrivia(validTrivia()));
  }

  @Test
  void createTriviaReturnsEmptyOnValidationFailureWithoutTouchingTheRepository() {
    TriviaDto dto = validTrivia();
    dto.setCategory(null);

    Optional<TriviaEntity> result = triviasService.createTrivia(dto);

    assertTrue(result.isEmpty());
    verify(triviasRepository, never()).findByHash(any());
    verify(triviasRepository, never()).insert(any(TriviaEntity.class));
  }

  @Test
  void createTriviaReturnsEmptyWhenHashAlreadyExists() {
    when(triviasRepository.findByHash(any())).thenReturn(
      Optional.of(new TriviaEntity())
    );

    Optional<TriviaEntity> result = triviasService.createTrivia(validTrivia());

    assertTrue(result.isEmpty());
    verify(triviasRepository, never()).insert(any(TriviaEntity.class));
  }

  @Test
  void createTriviaInsertsBooleanDefaultsWhenThereIsOneDistractor() {
    when(triviasRepository.findByHash(any())).thenReturn(Optional.empty());
    when(triviasRepository.insert(any(TriviaEntity.class))).thenAnswer(
      inv -> inv.getArgument(0)
    );

    Optional<TriviaEntity> result = triviasService.createTrivia(validTrivia());

    assertTrue(result.isPresent());
    TriviaEntity created = result.get();
    assertNotNull(created.getId());
    assertEquals(24, created.getId().length());
    assertEquals("boolean", created.getType());
    assertEquals("medium", created.getDifficulty());
    assertEquals("manual", created.getSource());
    assertFalse(created.getEnabled());
    assertEquals(0, created.getTimesUsed());
    assertEquals(
      TriviasServiceImpl.hashQuestion("What is X?"),
      created.getHash()
    );
  }

  @Test
  void createTriviaDefaultsTypeToMultipleWhenThereAreSeveralDistractors() {
    TriviaDto dto = validTrivia();
    dto.setIncorrectAnswers(List.of("No", "Maybe", "Skip"));
    when(triviasRepository.findByHash(any())).thenReturn(Optional.empty());
    when(triviasRepository.insert(any(TriviaEntity.class))).thenAnswer(
      inv -> inv.getArgument(0)
    );

    ArgumentCaptor<TriviaEntity> captor = ArgumentCaptor.forClass(
      TriviaEntity.class
    );
    Optional<TriviaEntity> result = triviasService.createTrivia(dto);

    assertTrue(result.isPresent());
    verify(triviasRepository).insert(captor.capture());
    assertEquals("multiple", captor.getValue().getType());
  }

  @Test
  void createTriviasCountsInsertedDuplicatesAndRejectedIncludingInBatchDuplicates() {
    TriviaDto first = validTrivia();
    TriviaDto inBatchDuplicate = validTrivia();
    inBatchDuplicate.setQuestion("  What   is X? ");
    TriviaDto rejected = validTrivia();
    rejected.setCategory(null);
    TriviaDto second = validTrivia();
    second.setQuestion("What is Y?");
    second.setCorrectAnswer("Yep");
    second.setIncorrectAnswers(List.of("Nope"));

    when(triviasRepository.findByHash(any())).thenReturn(Optional.empty());
    when(triviasRepository.insert(any(TriviaEntity.class))).thenAnswer(
      inv -> inv.getArgument(0)
    );

    TriviaBulkResultDto result = triviasService.createTrivias(
      List.of(first, inBatchDuplicate, rejected, second)
    );

    assertEquals(4, result.getReceived());
    assertEquals(2, result.getInserted());
    assertEquals(1, result.getDuplicates());
    assertEquals(List.of("[2] category is required."), result.getRejected());
  }

  @Test
  void markUsedWithNullIdDoesNotCallMongoTemplate() {
    triviasService.markUsed(null);

    verify(mongoTemplate, never()).updateFirst(
      any(Query.class),
      any(Update.class),
      eq(TriviaEntity.class)
    );
  }

  @Test
  void pickQuestionReturnsTheSampledEntityWhenAggregationFindsOne() {
    TriviaEntity sampled = new TriviaEntity();
    sampled.setId("sampled-1");
    AggregationResults<TriviaEntity> results = new AggregationResults<>(
      List.of(sampled),
      new Document()
    );
    when(
      mongoTemplate.aggregate(
        any(Aggregation.class),
        eq(TriviaEntity.class),
        eq(TriviaEntity.class)
      )
    )
      .thenReturn(results);

    Optional<TriviaEntity> result = triviasService.pickQuestion(null);

    assertTrue(result.isPresent());
    assertEquals("sampled-1", result.get().getId());
    verify(mongoTemplate, never()).findOne(
      any(Query.class),
      eq(TriviaEntity.class)
    );
  }

  @Test
  void pickQuestionFallsBackToFindOneWhenAggregationIsEmpty() {
    when(
      mongoTemplate.aggregate(
        any(Aggregation.class),
        eq(TriviaEntity.class),
        eq(TriviaEntity.class)
      )
    )
      .thenReturn(new AggregationResults<>(List.of(), new Document()));
    TriviaEntity leastRecent = new TriviaEntity();
    leastRecent.setId("lru-1");
    when(mongoTemplate.findOne(any(Query.class), eq(TriviaEntity.class)))
      .thenReturn(leastRecent);

    Optional<TriviaEntity> result = triviasService.pickQuestion(null);

    assertTrue(result.isPresent());
    assertEquals("lru-1", result.get().getId());
  }

  @Test
  void pickQuestionReturnsEmptyWhenAggregationAndFindOneAreEmpty() {
    when(
      mongoTemplate.aggregate(
        any(Aggregation.class),
        eq(TriviaEntity.class),
        eq(TriviaEntity.class)
      )
    )
      .thenReturn(new AggregationResults<>(List.of(), new Document()));
    when(mongoTemplate.findOne(any(Query.class), eq(TriviaEntity.class)))
      .thenReturn(null);

    Optional<TriviaEntity> result = triviasService.pickQuestion(null);

    assertTrue(result.isEmpty());
  }

  @Test
  void resolveCategoryReturnsEmptyForNullOrBlankWithoutTouchingMongoTemplate() {
    assertTrue(triviasService.resolveCategory(null).isEmpty());
    assertTrue(triviasService.resolveCategory("").isEmpty());
    assertTrue(triviasService.resolveCategory("   ").isEmpty());
    verifyNoInteractions(mongoTemplate);
  }

  @Test
  void pickQuestionWithCategoryAndDifficultyReturnsTheSampledEntityWhenAggregationFindsOne() {
    TriviaEntity sampled = new TriviaEntity();
    sampled.setId("sampled-hard");
    AggregationResults<TriviaEntity> results = new AggregationResults<>(
      List.of(sampled),
      new Document()
    );
    when(
      mongoTemplate.aggregate(
        any(Aggregation.class),
        eq(TriviaEntity.class),
        eq(TriviaEntity.class)
      )
    )
      .thenReturn(results);

    Optional<TriviaEntity> result = triviasService.pickQuestion(
      "Science",
      "hard"
    );

    assertTrue(result.isPresent());
    assertEquals("sampled-hard", result.get().getId());
    verify(mongoTemplate, never()).findOne(
      any(Query.class),
      eq(TriviaEntity.class)
    );
  }

  private static TriviaDto validTrivia() {
    TriviaDto dto = new TriviaDto();
    dto.setCategory("Science");
    dto.setQuestion("What is X?");
    dto.setCorrectAnswer("Yes");
    dto.setIncorrectAnswers(List.of("No"));
    return dto;
  }
}
