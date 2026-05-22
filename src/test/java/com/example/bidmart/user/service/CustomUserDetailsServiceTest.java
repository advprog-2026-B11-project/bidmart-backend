package com.example.bidmart.user.service;

import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        Permission p1 = new Permission(UUID.randomUUID(), "READ_PRIVILEGES");
        Permission p2 = new Permission(UUID.randomUUID(), "WRITE_PRIVILEGES");

        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("USER");
        role.setPermissions(Set.of(p1, p2));

        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("testuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setActive(true);
        mockUser.setRole(role);
    }

    @Test
    void loadUserByUsername_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(3, authorities.size()); // 1 Role + 2 Permissions
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("READ_PRIVILEGES")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("WRITE_PRIVILEGES")));

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("unknown"));
        verify(userRepository).findByUsername("unknown");
    }

    @Test
    void loadUserByUsername_userDeactivated_throwsException() {
        mockUser.setActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("testuser"));
        verify(userRepository).findByUsername("testuser");
    }
}
