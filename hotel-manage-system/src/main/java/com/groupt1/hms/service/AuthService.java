package com.groupt1.hms.service;

import com.groupt1.hms.entity.Guest;
import com.groupt1.hms.entity.Role;
import com.groupt1.hms.entity.UserAccount;
import com.groupt1.hms.repository.GuestRepository;
import com.groupt1.hms.repository.RoleRepository;
import com.groupt1.hms.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AuthService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailSenderService emailSenderService;

    // ──────────────── LOGIN ────────────────

    /**
     * Authenticate a user by username/email and password.
     * Returns a map with user info on success, or error message on failure.
     */
    public Map<String, Object> login(String usernameOrEmail, String password) {
        Map<String, Object> result = new HashMap<>();

        Optional<UserAccount> optUser = userAccountRepository
                .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);

        if (optUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "Invalid username or email.");
            return result;
        }

        UserAccount user = optUser.get();

        if (user.getStatus() != com.groupt1.hms.enums.AccountStatus.ACTIVE) {
            result.put("success", false);
            result.put("message", "Your account has been deactivated or banned. Please contact support.");
            return result;
        }

        boolean matches = false;
        String dbHash = user.getPasswordHash();
        if (dbHash != null) {
            if (dbHash.startsWith("$2a$") || dbHash.startsWith("$2b$") || dbHash.startsWith("$2y$")) {
                matches = passwordEncoder.matches(password, dbHash);
            } else {
                // Fallback for pre-existing plain text dummy data
                matches = password.equals(dbHash);
            }
        }

        if (!matches) {
            result.put("success", false);
            result.put("message", "Incorrect password. Please try again.");
            return result;
        }

        // Build user info response
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        
        String redirectUrl = "/";
        if (user.getRole() != null) {
            String roleName = user.getRole().getRoleName();
            userInfo.put("role", roleName);
            if ("Admin".equalsIgnoreCase(roleName)) {
                redirectUrl = "/admin/home";
            } else if ("Manager".equalsIgnoreCase(roleName)) {
                redirectUrl = "/manager/home";
            } else if ("Receptionist".equalsIgnoreCase(roleName)) {
                redirectUrl = "/receptionist/home";
            } else if ("Housekeeper".equalsIgnoreCase(roleName)) {
                redirectUrl = "/housekeeper/home";
            } else if ("Customer".equalsIgnoreCase(roleName)) {
                redirectUrl = "/customer/home";
            }
        }

        result.put("success", true);
        result.put("user", userInfo);
        result.put("redirectUrl", redirectUrl);
        return result;
    }


    // ──────────────── REGISTER ────────────────

    /**
     * Register a new user. Creates UserAccount + Guest profile.
     */
    @Transactional
    public Map<String, Object> register(String fullname, String username, String email,
                                         String password, String phone) {
        Map<String, Object> result = new HashMap<>();

        // Check duplicates
        if (userAccountRepository.existsByUsername(username)) {
            result.put("success", false);
            result.put("message", "Username is already taken.");
            return result;
        }

        if (userAccountRepository.existsByEmail(email)) {
            result.put("success", false);
            result.put("message", "Email is already registered.");
            return result;
        }

        // Find or create Guest role
        Role guestRole = roleRepository.findByRoleName("Customer")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("Customer");
                    newRole.setDescription("Hotel guest with booking privileges");
                    return roleRepository.save(newRole);
                });

        // Create user account
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(guestRole);
        user.setStatus(com.groupt1.hms.enums.AccountStatus.ACTIVE);
        userAccountRepository.save(user);

        // Create guest profile
        Guest guest = new Guest();
        guest.setUserAccount(user);
        guest.setGuestName(fullname);
        guest.setCccd("PENDING_" + java.util.UUID.randomUUID().toString().substring(0, 8)); // Will be updated later via profile
        if (phone != null && !phone.isEmpty()) {
            guest.setPhoneNumber(phone);
        }
        guestRepository.save(guest);

        result.put("success", true);
        result.put("message", "Account created successfully!");
        return result;
    }


    // ──────────────── FORGOT PASSWORD ────────────────

    /**
     * Send a password reset code to the given email.
     */
    @Transactional
    public Map<String, Object> forgotPassword(String email) {
        Map<String, Object> result = new HashMap<>();

        Optional<UserAccount> optUser = userAccountRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "No account found with this email address.");
            return result;
        }

        UserAccount user = optUser.get();

        // Generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

        // Store code with 5-minute expiry
        user.setResetCode(code);
        user.setResetCodeExpiry(java.time.LocalDateTime.now().plusMinutes(5));
        userAccountRepository.save(user);

        // Send email
        try {
            emailSenderService.sendResetPasswordEmail(email, code);
            System.out.println("Reset password email sent to " + email + " with code " + code);
        } catch (Exception e) {
            System.err.println("Failed to send reset email to " + email + ": " + e.getMessage());
            System.out.println("Code: " + code);
        }

        result.put("success", true);
        result.put("message", "Verification code sent to your email.");
        return result;
    }

    /**
     * Verify a reset code.
     */
    public Map<String, Object> verifyCode(String email, String code) {
        Map<String, Object> result = new HashMap<>();

        Optional<UserAccount> optUser = userAccountRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "Account not found.");
            return result;
        }

        UserAccount user = optUser.get();
        
        if (user.getResetCode() == null || user.getResetCode().isEmpty()) {
            result.put("success", false);
            result.put("message", "No reset code found. Please request a new one.");
            return result;
        }

        if (user.getResetCodeExpiry() != null && java.time.LocalDateTime.now().isAfter(user.getResetCodeExpiry())) {
            result.put("success", false);
            result.put("message", "Code has expired. Please request a new one.");
            return result;
        }

        if (!user.getResetCode().equals(code)) {
            result.put("success", false);
            result.put("message", "Invalid verification code.");
            return result;
        }

        result.put("success", true);
        result.put("message", "Code verified successfully.");
        return result;
    }

    /**
     * Reset password after code verification.
     */
    @Transactional
    public Map<String, Object> resetPassword(String email, String newPassword) {
        Map<String, Object> result = new HashMap<>();

        Optional<UserAccount> optUser = userAccountRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "Account not found.");
            return result;
        }

        UserAccount user = optUser.get();
        
        // Ensure they still have a valid code (to prevent bypassing step 2 if done carefully)
        if (user.getResetCode() == null) {
            result.put("success", false);
            result.put("message", "Please verify your code first.");
            return result;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Clean up
        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        userAccountRepository.save(user);

        result.put("success", true);
        result.put("message", "Password reset successfully.");
        return result;
    }
}
