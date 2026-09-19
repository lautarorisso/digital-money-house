package com.lautarorisso.account_service.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class AliasGenerator {

  private static final int WORDS_PER_ALIAS = 3;

  private final List<String> words;
  private final SecureRandom random = new SecureRandom();

  public AliasGenerator() {
    this.words = loadWords();
  }

  private List<String> loadWords() {
    try (InputStream in = new ClassPathResource("words.txt").getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      List<String> loaded = reader.lines()
          .map(String::trim)
          .filter(word -> !word.isEmpty())
          .toList();
      if (loaded.size() < WORDS_PER_ALIAS) {
        throw new IllegalStateException(
            "words.txt must contain at least " + WORDS_PER_ALIAS + " words");
      }
      return loaded;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load words.txt from classpath", e);
    }
  }

  public String generate() {
    List<String> shuffled = new ArrayList<>(words);
    Collections.shuffle(shuffled, random);
    return String.join(".", shuffled.subList(0, WORDS_PER_ALIAS));
  }
}