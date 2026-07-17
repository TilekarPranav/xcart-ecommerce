package com.ecommerce.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.user.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> existsByName(String name);

	Optional<Role> findByName(String name);

}
