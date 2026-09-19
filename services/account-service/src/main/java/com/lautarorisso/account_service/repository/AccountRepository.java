package com.lautarorisso.account_service.repository;

import com.lautarorisso.account_service.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

  boolean existsByUserId(Long userId);
}