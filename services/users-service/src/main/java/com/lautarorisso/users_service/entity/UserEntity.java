package com.lautarorisso.users_service.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@NoArgsConstructor
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nombre", nullable = false, length = 30)
  @NotBlank(message = "Name is required")
  @Size(max = 30, message = "Name must not exceed 30 characters")
  private String nombre;

  @Column(name = "apellido", nullable = false, length = 30)
  @NotBlank(message = "Last name is required")
  @Size(max = 30, message = "Last name must not exceed 30 characters")
  private String apellido;

  @Column(name = "dni", nullable = false)
  @NotNull(message = "DNI is required")
  @Positive(message = "DNI must be a positive number")
  private Long dni;

  @Column(name = "email", nullable = false, length = 254)
  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  @Size(max = 254, message = "Email must not exceed 254 characters")
  private String email;

  @Column(name = "telefono", length = 30)
  @Pattern(regexp = "^\\+?[0-9\\s()-]{6,20}$", message = "Phone format is invalid")
  private String telefono;

  @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  @JoinTable(name = "user_rol", joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_rol_user")), inverseJoinColumns = @JoinColumn(name = "rol_id", foreignKey = @ForeignKey(name = "fk_user_rol_rol")))
  private List<RolEntity> roles = new ArrayList<>();

  public UserEntity(String nombre, String apellido, Long dni, String email, String telefono, List<RolEntity> roles) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.dni = dni;
    this.email = email;
    this.telefono = telefono;
    this.roles = roles;
  }
}
