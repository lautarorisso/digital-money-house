package com.lautarorisso.account_service.repository;

import com.lautarorisso.account_service.entity.CardEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, Long> {

  Optional<CardEntity> findByCardNumber(String cardNumber);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update CardEntity card set card.accountId = :accountId where card.id = :cardId and card.accountId is null")
  int associateIfUnassociated(@Param("cardId") Long cardId, @Param("accountId") Long accountId);
}
