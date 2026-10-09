package com.lautarorisso.account_service.repository;

import com.lautarorisso.account_service.entity.TransactionEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

  List<TransactionEntity> findTop5ByAccountIdOrderByTransactionDateDescIdDesc(Long accountId);

  List<TransactionEntity> findByAccountIdOrderByTransactionDateDescIdDesc(Long accountId);

  @Query("""
      select t from TransactionEntity t
      where t.accountId = :accountId
        and (:from is null or t.transactionDate >= :from)
        and (:to is null or t.transactionDate < :to)
        and (:type is null or t.type = :type)
        and (:minAmount is null or :minAmount = 0 or abs(t.amount) > :minAmount)
        and (:maxAmount is null or abs(t.amount) <= :maxAmount)
      order by t.transactionDate desc, t.id desc
      """)
  List<TransactionEntity> findActivity(@Param("accountId") Long accountId,
      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
      @Param("type") String type, @Param("minAmount") BigDecimal minAmount,
      @Param("maxAmount") BigDecimal maxAmount);

  Optional<TransactionEntity> findByIdAndAccountId(Long id, Long accountId);
}
