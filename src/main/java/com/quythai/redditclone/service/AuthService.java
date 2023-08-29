package com.quythai.redditclone.service;

import com.quythai.redditclone.dto.AuthenticationResponse;
import com.quythai.redditclone.dto.LoginRequest;
import com.quythai.redditclone.dto.RegisterRequest;
import com.quythai.redditclone.exceptions.SpringRedditException;
import com.quythai.redditclone.model.NotificationEmail;
import com.quythai.redditclone.model.RedditUser;
import com.quythai.redditclone.model.VerificatitonToken;
import com.quythai.redditclone.repository.UserRepository;
import com.quythai.redditclone.repository.VerificationTokenRepository;
import com.quythai.redditclone.security.JwtProvider;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signup(RegisterRequest resgisterRequest) {
        RedditUser redditUser = new RedditUser();
        redditUser.setUsername(resgisterRequest.getUsername());
        redditUser.setEmail(resgisterRequest.getEmail());
        redditUser.setPassword(passwordEncoder.encode(resgisterRequest.getPassword()));
        redditUser.setCreated(Instant.now());
        redditUser.setEnabled(false);

        userRepository.save(redditUser);

        String token = generateVerificationToken(redditUser);
        mailService.sendMail(new NotificationEmail("Please Activate your Account",
                redditUser.getEmail(), "Thank you for signing up to Spring Reddit, " +
                "please click on the below url to activate your account : " +
                "http://localhost:8080/api/auth/accountVerification/" + token));
    }

    private String generateVerificationToken(RedditUser redditUser) {
        String token = UUID.randomUUID().toString();
        VerificatitonToken verificatitonToken = new VerificatitonToken();
        verificatitonToken.setToken(token);
        verificatitonToken.setRedditUser(redditUser);

        verificationTokenRepository.save(verificatitonToken);
        return token;
    }

    @Transactional
    public void verifyAccount(String token) throws Exception {
        Optional<VerificatitonToken> verificationToken = verificationTokenRepository.findByToken(token);
        verificationToken.orElseThrow(() -> new SpringRedditException("Invalid Token"));
        fetchUserAndEnable(verificationToken.get());
    }

    private void fetchUserAndEnable(VerificatitonToken verificatitonToken) throws Exception {
        String username = verificatitonToken.getRedditUser().getUsername();
        RedditUser user = userRepository.findByUsername(username).orElseThrow(() -> new SpringRedditException("User not found with name "+ username));
        user.setEnabled(true);
        userRepository.save(user);
    }

    public AuthenticationResponse login(LoginRequest loginRequest) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        String token = jwtProvider.generateToken(authenticate);
        return new AuthenticationResponse(token, loginRequest.getUsername());
    }
}
