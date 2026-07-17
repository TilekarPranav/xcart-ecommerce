package com.ecommerce.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("No user found with email:" + email));

		Collection<SimpleGrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority(role.getName())).toList();

		return org.springframework.security.core.userdetails.User.builder().username(user.getEmail())
				.password(user.getPasswordHash()).disabled(!user.isEnabled())
				.authorities(authorities.isEmpty() ? List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")) : authorities)
				.build();
	}

}
