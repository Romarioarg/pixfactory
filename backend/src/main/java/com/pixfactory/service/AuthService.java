package com.pixfactory.service;

import com.pixfactory.domain.AccountStatus;
import com.pixfactory.domain.PasswordResetToken;
import com.pixfactory.domain.Role;
import com.pixfactory.domain.User;
import com.pixfactory.exception.ApiException;
import com.pixfactory.exception.ConflictException;
import com.pixfactory.exception.NotFoundException;
import com.pixfactory.integration.EmailService;
import com.pixfactory.mapper.DtoMapper;
import com.pixfactory.repo.PasswordResetTokenRepository;
import com.pixfactory.repo.UserRepository;
import com.pixfactory.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DtoMapper mapper;
    private final EmailService emailService;
    private final PasswordResetTokenRepository resetTokenRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            DtoMapper mapper,
            EmailService emailService,
            PasswordResetTokenRepository resetTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.emailService = emailService;
        this.resetTokenRepository = resetTokenRepository;
    }

    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ApiException(401, "E-mail ou senha incorretos."));
        if (user.getStatus() != AccountStatus.ATIVO) {
            throw new ApiException(401, "Usuário inativo.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(401, "E-mail ou senha incorretos.");
        }
        return tokenResponse(user);
    }

    @Transactional
    public Map<String, Object> register(String name, String email, String phone, String password) {
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ConflictException("Este e-mail já está cadastrado.");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        user.setPlan("gratuito");
        user.setStatus(AccountStatus.ATIVO);
        userRepository.save(user);
        emailService.send(user.getEmail(), "Bem-vindo ao PixFactory", "Conta criada no ambiente DEMO.");
        return tokenResponse(user);
    }

    @Transactional
    public Map<String, Object> createUser(String name, String email, String phone, String password, String role) {
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ConflictException("Este e-mail já está cadastrado.");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role == null || role.isBlank() ? Role.USER : Role.valueOf(role.toUpperCase()));
        user.setPlan("gratuito");
        user.setStatus(AccountStatus.ATIVO);
        return mapper.user(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        userRepository.delete(user);
    }

    public Map<String, Object> me(User user) {
        return mapper.user(user);
    }

    @Transactional
    public Map<String, Object> updateMe(User user, Map<String, Object> body) {
        if (body.get("name") != null) user.setName(String.valueOf(body.get("name")));
        if (body.get("phone") != null) user.setPhone(String.valueOf(body.get("phone")));
        if (body.get("address") != null) user.setAddress(String.valueOf(body.get("address")));
        if (body.get("email") != null) {
            String email = String.valueOf(body.get("email")).trim().toLowerCase();
            userRepository.findByEmailIgnoreCase(email)
                    .filter(other -> !other.getId().equals(user.getId()))
                    .ifPresent(other -> { throw new ConflictException("Este e-mail já está cadastrado."); });
            user.setEmail(email);
        }
        return mapper.user(userRepository.save(user));
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String nextPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(400, "Senha atual incorreta.");
        }
        user.setPasswordHash(passwordEncoder.encode(nextPassword));
        userRepository.save(user);
    }

    @Transactional
    public Map<String, Object> forgotPassword(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new NotFoundException("E-mail não encontrado."));
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(user.getEmail());
        token.setCode(code);
        token.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        resetTokenRepository.save(token);
        emailService.send(user.getEmail(), "Recuperação de senha (DEMO)", "Seu código DEMO é: " + code);
        return Map.of(
                "message", "Código enviado (EMAIL DEMO). No ambiente de portfólio o código também é retornado.",
                "demoCode", code,
                "demo", true
        );
    }

    @Transactional
    public void resetPassword(String email, String code, String nextPassword) {
        PasswordResetToken token = resetTokenRepository.findTopByEmailIgnoreCaseAndUsedFalseOrderByIdDesc(email.trim())
                .orElseThrow(() -> new ApiException(400, "Código inválido."));
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now()) || !token.getCode().equals(code)) {
            throw new ApiException(400, "Código inválido.");
        }
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new NotFoundException("E-mail não encontrado."));
        user.setPasswordHash(passwordEncoder.encode(nextPassword));
        userRepository.save(user);
        token.setUsed(true);
        resetTokenRepository.save(token);
    }

    @Transactional
    public Map<String, Object> updatePlan(User user, String plan) {
        user.setPlan(plan);
        return mapper.user(userRepository.save(user));
    }

    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream().map(mapper::user).toList();
    }

    @Transactional
    public Map<String, Object> updateUser(Long id, Map<String, Object> body) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        if (body.get("name") != null) user.setName(String.valueOf(body.get("name")));
        if (body.get("role") != null) user.setRole(Role.valueOf(String.valueOf(body.get("role")).toUpperCase()));
        if (body.get("status") != null) user.setStatus(AccountStatus.from(String.valueOf(body.get("status"))));
        if (body.get("plan") != null) user.setPlan(String.valueOf(body.get("plan")));
        return mapper.user(userRepository.save(user));
    }

    private Map<String, Object> tokenResponse(User user) {
        return Map.of(
                "token", jwtService.generate(user.getId(), user.getEmail(), user.getRole().name()),
                "user", mapper.user(user)
        );
    }
}
