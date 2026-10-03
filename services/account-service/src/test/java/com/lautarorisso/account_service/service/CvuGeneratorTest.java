package com.lautarorisso.account_service.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CvuGeneratorTest {

  @Test
  void generateReturnsExactlyTwentyTwoDigits() {
    assertTrue(new CvuGenerator().generate().matches("\\d{22}"));
  }
}
