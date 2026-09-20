package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.TriviaBulkResultDto;
import com.nanobot.nanobotbackend.dto.TriviaDto;
import com.nanobot.nanobotbackend.entity.TriviaEntity;
import com.nanobot.nanobotbackend.repository.TriviasRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class TriviasServiceImpl implements TriviasService {

  private static final String TYPE_MULTIPLE = "multiple";

  private static final String TYPE_BOOLEAN = "boolean";

  private static final Set<String> DIFFICULTIES = Set.copyOf(
    Constants.TRIVIA_DIFFICULTIES
  );

  private static final String NOT_FOUND_BY_ID = "Trivia not found with ID: ";

  private final TriviasRepository triviasRepository;

  private final MongoTemplate mongoTemplate;

  private final FileLogger fileLogger;

  public TriviasServiceImpl(
    TriviasRepository triviasRepository,
    MongoTemplate mongoTemplate
  ) {
    this.fileLogger = new FileLogger("TriviasService");
    this.triviasRepository = triviasRepository;
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Same normalization the import scripts use for their hash, so a question
   * posted through the API collides with the same question loaded by
   * mongoimport: trimmed, runs of whitespace collapsed, lower-cased.
   */
  public static String normalizeQuestion(String question) {
    return question.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  public static String hashQuestion(String question) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] bytes = digest.digest(
        normalizeQuestion(question).getBytes(StandardCharsets.UTF_8)
      );
      StringBuilder hex = new StringBuilder();
      for (byte b : bytes) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 unavailable", e);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  @Override
  public String validateTrivia(TriviaDto trivia) {
    if (trivia == null) {
      return "Trivia body is missing.";
    }
    if (isBlank(trivia.getCategory())) {
      return "category is required.";
    }
    if (isBlank(trivia.getQuestion())) {
      return "question is required.";
    }
    if (trivia.getQuestion().trim().length() > Constants.maximumTriviaQuestionLength) {
      return (
        "question is longer than " +
        Constants.maximumTriviaQuestionLength +
        " characters."
      );
    }
    if (isBlank(trivia.getCorrectAnswer())) {
      return "correctAnswer is required.";
    }
    if (
      trivia.getIncorrectAnswers() == null ||
      trivia.getIncorrectAnswers().isEmpty()
    ) {
      return "incorrectAnswers must have at least one answer.";
    }
    int total = trivia.getIncorrectAnswers().size() + 1;
    if (total > Constants.maximumTriviaAnswers) {
      return (
        "A question can have at most " +
        Constants.maximumTriviaAnswers +
        " answers (Discord shows one button per answer)."
      );
    }

    List<String> answers = new ArrayList<>();
    answers.add(trivia.getCorrectAnswer());
    answers.addAll(trivia.getIncorrectAnswers());
    Set<String> seen = new HashSet<>();
    for (String answer : answers) {
      if (isBlank(answer)) {
        return "Answers cannot be blank.";
      }
      if (answer.trim().length() > Constants.maximumTriviaAnswerLength) {
        return (
          "Answer `" +
          answer.trim() +
          "` is longer than " +
          Constants.maximumTriviaAnswerLength +
          " characters, the Discord button label limit. Reword or drop it."
        );
      }
      if (!seen.add(answer.trim().toLowerCase(Locale.ROOT))) {
        return "Answers must be distinct: `" + answer.trim() + "` repeats.";
      }
    }

    if (
      !isBlank(trivia.getType()) &&
      !TYPE_MULTIPLE.equalsIgnoreCase(trivia.getType()) &&
      !TYPE_BOOLEAN.equalsIgnoreCase(trivia.getType())
    ) {
      return "type must be `multiple` or `boolean`.";
    }
    if (
      !isBlank(trivia.getDifficulty()) &&
      !DIFFICULTIES.contains(trivia.getDifficulty().toLowerCase(Locale.ROOT))
    ) {
      return "difficulty must be `easy`, `medium` or `hard`.";
    }
    return null;
  }

  /** Fills in derived and defaulted fields after validation has passed. */
  private TriviaEntity toEntity(TriviaDto trivia) {
    TriviaDto clean = new TriviaDto();
    clean.setId(trivia.getId());
    clean.setCategory(trivia.getCategory().trim());
    clean.setQuestion(trivia.getQuestion().trim().replaceAll("\\s+", " "));
    clean.setCorrectAnswer(trivia.getCorrectAnswer().trim());
    clean.setIncorrectAnswers(
      trivia
        .getIncorrectAnswers()
        .stream()
        .map(String::trim)
        .collect(Collectors.toList())
    );
    clean.setType(
      isBlank(trivia.getType())
        ? (clean.getIncorrectAnswers().size() == 1 ? TYPE_BOOLEAN : TYPE_MULTIPLE)
        : trivia.getType().trim().toLowerCase(Locale.ROOT)
    );
    clean.setDifficulty(
      isBlank(trivia.getDifficulty())
        ? "medium"
        : trivia.getDifficulty().trim().toLowerCase(Locale.ROOT)
    );
    clean.setSource(
      isBlank(trivia.getSource()) ? "manual" : trivia.getSource().trim()
    );
    clean.setHash(
      isBlank(trivia.getHash())
        ? hashQuestion(clean.getQuestion())
        : trivia.getHash().trim()
    );
    // Nothing goes live by accident: a question is disabled unless asked for.
    clean.setEnabled(trivia.getEnabled() != null && trivia.getEnabled());
    clean.setLastUsed(trivia.getLastUsed());
    clean.setTimesUsed(trivia.getTimesUsed() == null ? 0 : trivia.getTimesUsed());
    return new TriviaEntity(clean);
  }

  @Override
  public Optional<TriviaEntity> createTrivia(TriviaDto triviaDto) {
    String error = validateTrivia(triviaDto);
    if (error != null) {
      fileLogger.warn("Rejected trivia: " + error);
      return Optional.empty();
    }
    TriviaEntity triviaEntity = toEntity(triviaDto);
    if (triviasRepository.findByHash(triviaEntity.getHash()).isPresent()) {
      fileLogger.warn(
        "Trivia already exists with hash: " + triviaEntity.getHash()
      );
      return Optional.empty();
    }
    ObjectId id = new ObjectId();
    triviaEntity.setId(id.toHexString());
    try {
      TriviaEntity created = triviasRepository.insert(triviaEntity);
      fileLogger.info("Trivia created with ID: " + created.getId());
      return Optional.of(created);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Trivia already exists with hash: " + triviaEntity.getHash()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating trivia: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public TriviaBulkResultDto createTrivias(List<TriviaDto> triviaDtos) {
    TriviaBulkResultDto result = new TriviaBulkResultDto();
    if (triviaDtos == null) {
      return result;
    }
    result.setReceived(triviaDtos.size());
    Set<String> hashesInBatch = new HashSet<>();
    int inserted = 0;
    int duplicates = 0;
    for (int i = 0; i < triviaDtos.size(); i++) {
      TriviaDto dto = triviaDtos.get(i);
      String error = validateTrivia(dto);
      if (error != null) {
        result.getRejected().add("[" + i + "] " + error);
        continue;
      }
      TriviaEntity entity = toEntity(dto);
      if (
        !hashesInBatch.add(entity.getHash()) ||
        triviasRepository.findByHash(entity.getHash()).isPresent()
      ) {
        duplicates++;
        continue;
      }
      entity.setId(new ObjectId().toHexString());
      try {
        triviasRepository.insert(entity);
        inserted++;
      } catch (DuplicateKeyException e) {
        duplicates++;
      }
    }
    result.setInserted(inserted);
    result.setDuplicates(duplicates);
    fileLogger.info(
      "Bulk trivia import: received=" +
      result.getReceived() +
      " inserted=" +
      inserted +
      " duplicates=" +
      duplicates +
      " rejected=" +
      result.getRejected().size()
    );
    return result;
  }

  @Override
  public List<TriviaEntity> getTrivias(
    String category,
    Boolean enabled,
    String source
  ) {
    try {
      if (category == null && enabled == null && source == null) {
        return triviasRepository.findAll();
      }
      Criteria criteria = new Criteria();
      if (category != null) {
        criteria = criteria.and("category").regex(exactIgnoreCase(category));
      }
      if (enabled != null) {
        criteria = criteria.and("enabled").is(enabled);
      }
      if (source != null) {
        criteria = criteria.and("source").is(source);
      }
      return mongoTemplate.find(Query.query(criteria), TriviaEntity.class);
    } catch (Exception e) {
      fileLogger.error("Error fetching trivias: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<TriviaEntity> getTriviaById(String id) {
    if (id != null) {
      try {
        return triviasRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching trivia by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public List<String> getCategories() {
    try {
      List<String> categories = mongoTemplate
        .query(TriviaEntity.class)
        .distinct("category")
        .matching(Query.query(Criteria.where("enabled").is(true)))
        .as(String.class)
        .all();
      return categories
        .stream()
        .filter(c -> c != null && !c.isBlank())
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .collect(Collectors.toList());
    } catch (Exception e) {
      fileLogger.error("Error fetching trivia categories: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<TriviaEntity> updateTrivia(String id, TriviaDto triviaDto) {
    if (id == null) {
      return Optional.empty();
    }
    String error = validateTrivia(triviaDto);
    if (error != null) {
      fileLogger.warn("Rejected trivia update " + id + ": " + error);
      return Optional.empty();
    }
    try {
      Optional<TriviaEntity> existing = triviasRepository.findById(id);
      if (existing.isEmpty()) {
        fileLogger.warn(NOT_FOUND_BY_ID + id);
        return Optional.empty();
      }
      TriviaEntity current = existing.get();
      TriviaEntity incoming = toEntity(triviaDto);
      current.setCategory(incoming.getCategory());
      current.setType(incoming.getType());
      current.setDifficulty(incoming.getDifficulty());
      current.setQuestion(incoming.getQuestion());
      current.setCorrectAnswer(incoming.getCorrectAnswer());
      current.setIncorrectAnswers(incoming.getIncorrectAnswers());
      current.setSource(incoming.getSource());
      current.setHash(hashQuestion(incoming.getQuestion()));
      current.setEnabled(incoming.getEnabled());
      if (triviaDto.getLastUsed() != null) {
        current.setLastUsed(triviaDto.getLastUsed());
      }
      if (triviaDto.getTimesUsed() != null) {
        current.setTimesUsed(triviaDto.getTimesUsed());
      }
      TriviaEntity updated = triviasRepository.save(current);
      fileLogger.info("Trivia updated with ID: " + id);
      return Optional.of(updated);
    } catch (DuplicateKeyException e) {
      fileLogger.warn("Trivia update " + id + " collides with another hash.");
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error updating trivia: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<TriviaEntity> deleteTrivia(String id) {
    if (id != null) {
      Optional<TriviaEntity> existing = triviasRepository.findById(id);
      if (existing.isPresent()) {
        triviasRepository.deleteById(id);
        fileLogger.info("Trivia deleted with ID: " + id);
        return existing;
      }
      fileLogger.warn(NOT_FOUND_BY_ID + id);
    }
    return Optional.empty();
  }

  private static Pattern exactIgnoreCase(String value) {
    return Pattern.compile(
      "^" + Pattern.quote(value.trim()) + "$",
      Pattern.CASE_INSENSITIVE
    );
  }

  @Override
  public Optional<String> resolveCategory(String category) {
    if (isBlank(category)) {
      return Optional.empty();
    }
    String wanted = category.trim();
    return getCategories()
      .stream()
      .filter(c -> c.equalsIgnoreCase(wanted))
      .findFirst();
  }

  @Override
  public Optional<TriviaEntity> pickQuestion(String category, String difficulty) {
    try {
      Criteria pool = Criteria.where("enabled").is(true);
      if (category != null && !category.isBlank()) {
        pool = pool.and("category").regex(exactIgnoreCase(category));
      }
      if (difficulty != null && !difficulty.isBlank()) {
        pool = pool.and("difficulty").regex(exactIgnoreCase(difficulty));
      }

      // First choice: a random question nobody has seen in a while.
      Date cutoff = new Date(
        System.currentTimeMillis() -
        Constants.triviaRecentlyUsedDays * 24L * 60L * 60L * 1000L
      );
      Criteria fresh = new Criteria()
        .andOperator(
          pool,
          new Criteria()
            .orOperator(
              Criteria.where("lastUsed").is(null),
              Criteria.where("lastUsed").lt(cutoff)
            )
        );
      List<TriviaEntity> sampled = mongoTemplate
        .aggregate(
          Aggregation.newAggregation(
            Aggregation.match(fresh),
            Aggregation.sample(1)
          ),
          TriviaEntity.class,
          TriviaEntity.class
        )
        .getMappedResults();
      if (!sampled.isEmpty()) {
        return Optional.of(sampled.get(0));
      }

      // Every enabled question in the pool was used recently, so take the one
      // used longest ago. Never-used questions sort first because null < date.
      TriviaEntity leastRecent = mongoTemplate.findOne(
        Query
          .query(pool)
          .with(Sort.by(Sort.Direction.ASC, "lastUsed"))
          .limit(1),
        TriviaEntity.class
      );
      return Optional.ofNullable(leastRecent);
    } catch (Exception e) {
      fileLogger.error("Error picking trivia question: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public void markUsed(String id) {
    if (id == null) {
      return;
    }
    try {
      mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(id)),
        new Update().set("lastUsed", new Date()).inc("timesUsed", 1),
        TriviaEntity.class
      );
    } catch (Exception e) {
      // Losing the usage stamp only weakens repeat avoidance; the drop itself
      // has already been paid for, so this must not fail the request.
      fileLogger.error("Error marking trivia used: " + e.getMessage());
    }
  }
}
