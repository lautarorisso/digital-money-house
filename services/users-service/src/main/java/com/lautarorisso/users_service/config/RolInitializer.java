package com.lautarorisso.users_service.config;

import com.lautarorisso.users_service.entity.RolEntity;
import com.lautarorisso.users_service.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RolInitializer implements ApplicationRunner {

  private final RolRepository rolRepository;

  @Override
  public void run(ApplicationArguments args) {
    seedRole("USER");
    seedRole("ADMIN");
  }

  private void seedRole(String nombre) {
    if (rolRepository.findByNombre(nombre) == null) {
      rolRepository.save(new RolEntity(nombre));
    }
  }
}