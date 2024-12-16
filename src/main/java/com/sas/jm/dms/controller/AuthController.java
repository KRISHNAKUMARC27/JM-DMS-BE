package com.sas.jm.dms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sas.jm.dms.config.JwtUtil;
import com.sas.jm.dms.entity.Role;
import com.sas.jm.dms.entity.User;
import com.sas.jm.dms.model.AuthRequest;
import com.sas.jm.dms.model.AuthResponse;
import com.sas.jm.dms.repository.RoleRepository;
import com.sas.jm.dms.repository.UserRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder encoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

        if (!authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String jwtToken = jwtUtil.generateToken(authRequest.getUsername());
        String username = authentication.getName(); // Returns the username
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()) // Convert roles to String
                .toList();
        return AuthResponse.builder().token(jwtToken).username(username).roles(roles).build();
    }
    
    @PostMapping("/signup")
    public User signup(@RequestBody User user) {
    	user.setPassword(encoder.encode(user.getPassword()));
    	return userRepository.save(user);
    }
    
    @PostMapping("/createRole")
    public Role createRole(@RequestBody Role role) {
    	return roleRepository.save(role);
    }
    
    @GetMapping("/role/{name}")
    public Role getRole(@PathVariable String name) {
    	return roleRepository.findByName(name);
    }
    
}

