package com.lautarorisso.users_service.repository;

import com.lautarorisso.users_service.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolRepository extends JpaRepository<RolEntity, Long> {

  RolEntity findByNombre(String nombre);
}