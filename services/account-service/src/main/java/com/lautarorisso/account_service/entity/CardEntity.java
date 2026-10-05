package com.lautarorisso.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor
public class CardEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "card_number", nullable = false, unique = true, length = 19)
  private String cardNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 10)
  private CardType type;

  @Column(name = "account_id")
  private Long accountId;

  public CardEntity(String cardNumber, CardType type) {
    this.cardNumber = cardNumber;
    this.type = type;
  }

  public void associateWith(Long accountId) {
    this.accountId = accountId;
  }
}
