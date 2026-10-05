package com.lautarorisso.account_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AliasGeneratorTest {

  @Test
  void generateReturnsThreeDictionaryWordsSeparatedByDots() throws Exception {
    Set<String> dictionary;
    var stream = getClass().getResourceAsStream("/words.txt");
    assertNotNull(stream);
    try (stream; var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      dictionary = reader.lines().map(String::trim).filter(word -> !word.isEmpty()).collect(Collectors.toSet());
    }

    String[] words = new AliasGenerator().generate().split("\\.", -1);

    assertEquals(3, words.length);
    for (String word : words) {
      assertFalse(word.isBlank());
      assertTrue(dictionary.contains(word));
    }
  }
}
