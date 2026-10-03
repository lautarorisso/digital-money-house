package com.lautarorisso.account_service.repository;

import com.lautarorisso.account_service.entity.TransactionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

  List<TransactionEntity> findTop5ByAccountIdOrderByTransactionDateDescIdDesc(Long accountId);
}
