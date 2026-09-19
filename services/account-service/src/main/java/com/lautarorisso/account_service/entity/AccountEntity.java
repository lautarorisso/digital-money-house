package com.lautarorisso.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_accounts_alias", columnNames = "alias"),
      @UniqueConstraint(name = "uk_accounts_cvu", columnNames = "cvu"),
      @UniqueConstraint(name = "uk_accounts_user_id", columnNames = "user_id")
    })
@Getter
@NoArgsConstructor
public class AccountEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "cvu", nullable = false, length = 22)
  private String cvu;

  @Column(name = "alias", nullable = false, length = 60)
  private String alias;

  public AccountEntity(Long userId, String cvu, String alias) {
    this.userId = userId;
    this.cvu = cvu;
    this.alias = alias;
  }
}