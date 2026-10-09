package com.lautarorisso.account_service.repository;

import com.lautarorisso.account_service.entity.TransactionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

  List<TransactionEntity> findTop5ByAccountIdOrderByTransactionDateDescIdDesc(Long accountId);

  List<TransactionEntity> findByAccountIdOrderByTransactionDateDescIdDesc(Long accountId);

  Optional<TransactionEntity> findByIdAndAccountId(Long id, Long accountId);
}
