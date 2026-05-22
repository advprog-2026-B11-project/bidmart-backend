package com.example.bidmart.user.service;

import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_shouldReturnAuthorities() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("USER");
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setName("bid:place");
        role.setPermissions(Set.of(permission));

        User user = new User();
        user.setUsername("alice");
        user.setPassword("encoded");
        user.setActive(true);
        user.setRole(role);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        assertEquals("alice", details.getUsername());
        assertTrue(details.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(details.getAuthorities().contains(new SimpleGrantedAuthority("bid:place")));
    }

    @Test
    void loadUserByUsername_shouldThrowWhenInactive() {
        Role role = new Role();
        role.setName("USER");

        User user = new User();
        user.setUsername("alice");
        user.setPassword("encoded");
        user.setActive(false);
        user.setRole(role);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("alice"));
    }
}
