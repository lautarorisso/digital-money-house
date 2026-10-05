package com.lautarorisso.account_service.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum CardType {
  DEBIT,
  CREDIT;

  @JsonCreator
  public static CardType fromJson(String value) {
    return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
  }
}
