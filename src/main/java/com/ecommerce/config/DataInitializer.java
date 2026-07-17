package com.ecommerce.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ecommerce.user.entity.Role;
import com.ecommerce.user.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final RoleRepository roleRepository;

	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		createRoleIfMissing(Role.ROLE_ADMIN);
		createRoleIfMissing(Role.ROLE_CUSTOMER);
	}

	private void createRoleIfMissing(String roleName) {
		roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
	}

}
