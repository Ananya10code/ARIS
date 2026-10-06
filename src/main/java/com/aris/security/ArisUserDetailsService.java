package com.aris.security;
import com.aris.auth.User;
import com.aris.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class ArisUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    public ArisUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String clean = email != null ? email.trim() : "";
        User user = userRepository.findByEmailIgnoreCase(clean)
                .or(() -> {
                    if ("rakshit".equalsIgnoreCase(clean)) {
                        return userRepository.findByEmailIgnoreCase("rakshit@aris.dev");
                    }
                    if ("rakshit@aris.dev".equalsIgnoreCase(clean)) {
                        return userRepository.findByEmailIgnoreCase("rakshit");
                    }
                    return java.util.Optional.empty();
                })
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + email));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                )
        );
    }
}
