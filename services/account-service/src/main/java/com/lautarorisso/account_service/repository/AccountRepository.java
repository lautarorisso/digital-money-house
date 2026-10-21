package com.lautarorisso.account_service.repository;

import com.lautarorisso.account_service.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

  Optional<AccountEntity> findByUserId(Long userId);

  @Query("select a.ownerSub from AccountEntity a where a.id = :id")
  Optional<String> findOwnerSubById(@Param("id") Long id);

  @Query("select a.id from AccountEntity a where a.cvu = :destination or a.alias = :destination")
  Optional<Long> findIdByDestination(@Param("destination") String destination);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select account from AccountEntity account where account.id = :id")
  Optional<AccountEntity> findByIdForUpdate(@Param("id") Long id);
}
