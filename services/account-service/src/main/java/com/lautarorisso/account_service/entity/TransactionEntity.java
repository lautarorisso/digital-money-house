package com.lautarorisso.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor
public class TransactionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "type", nullable = false, length = 30)
  private String type;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "transaction_date", nullable = false)
  private LocalDateTime transactionDate;

  @Column(name = "balance_after", precision = 12, scale = 2)
  private BigDecimal balanceAfter;

  public TransactionEntity(Long accountId, BigDecimal amount, String type, String description,
      LocalDateTime transactionDate, BigDecimal balanceAfter) {
    this.accountId = accountId;
    this.amount = amount;
    this.type = type;
    this.description = description;
    this.transactionDate = transactionDate;
    this.balanceAfter = balanceAfter;
  }
}
