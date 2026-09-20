package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.TriviaBulkResultDto;
import com.nanobot.nanobotbackend.dto.TriviaDto;
import com.nanobot.nanobotbackend.entity.TriviaEntity;
import java.util.List;
import java.util.Optional;

public interface TriviasService {
  /** Null when the question is well formed, otherwise the reason it is not. */
  String validateTrivia(TriviaDto triviaDto);

  /** Empty when the question is invalid or its hash already exists. */
  Optional<TriviaEntity> createTrivia(TriviaDto triviaDto);

  TriviaBulkResultDto createTrivias(List<TriviaDto> triviaDtos);

  List<TriviaEntity> getTrivias(String category, Boolean enabled, String source);

  Optional<TriviaEntity> getTriviaById(String id);

  /** Distinct categories that have at least one enabled question, sorted. */
  List<String> getCategories();

  Optional<TriviaEntity> updateTrivia(String id, TriviaDto triviaDto);

  Optional<TriviaEntity> deleteTrivia(String id);

  /**
   * Chooses a random enabled question, preferring ones not used recently and
   * falling back to the least recently used. Does not mark it used; call
   * {@link #markUsed(String)} once the drop that will show it exists.
   *
   * @param category exact category name (case-insensitive), or null for any
   * @param difficulty easy, medium or hard (case-insensitive), or null for any
   */
  Optional<TriviaEntity> pickQuestion(String category, String difficulty);

  /** Any difficulty; see {@link #pickQuestion(String, String)}. */
  default Optional<TriviaEntity> pickQuestion(String category) {
    return pickQuestion(category, null);
  }

  /**
   * The stored spelling of a category the user typed, matched ignoring case,
   * or empty when no enabled question has that category.
   */
  Optional<String> resolveCategory(String category);

  void markUsed(String id);
}
