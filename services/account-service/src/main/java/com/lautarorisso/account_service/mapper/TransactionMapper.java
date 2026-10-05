package com.lautarorisso.account_service.mapper;

import com.lautarorisso.account_service.dto.TransactionResponse;
import com.lautarorisso.account_service.entity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

  public TransactionResponse toResponse(TransactionEntity transaction) {
    return new TransactionResponse(transaction.getId(), transaction.getAmount(), transaction.getType(),
        transaction.getDescription(), transaction.getTransactionDate(), transaction.getBalanceAfter());
  }
}
