package com.aris.auth;
import com.aris.auth.repository.UserRepository;
import com.aris.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    public User register(String name, String email, String password) {
        String cleanEmail = email != null ? email.trim() : "";
        String cleanName = name != null ? name.trim() : "Developer";
        if (cleanEmail.isEmpty() || password == null || password.isBlank()) {
            throw new RuntimeException("Email and password are required");
        }
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new RuntimeException("Email already registered");
        }
        User user = new User(
                cleanName,
                cleanEmail,
                passwordEncoder.encode(password),
                Role.DEVELOPER
        );
        return userRepository.save(user);
    }
    public LoginResponse login(String email, String password) {
        String cleanEmail = email != null ? email.trim() : "";
        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .or(() -> {
                    if ("rakshit".equalsIgnoreCase(cleanEmail)) {
                        return userRepository.findByEmailIgnoreCase("rakshit@aris.dev");
                    }
                    if ("rakshit@aris.dev".equalsIgnoreCase(cleanEmail)) {
                        return userRepository.findByEmailIgnoreCase("rakshit");
                    }
                    return java.util.Optional.empty();
                })
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );
        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}