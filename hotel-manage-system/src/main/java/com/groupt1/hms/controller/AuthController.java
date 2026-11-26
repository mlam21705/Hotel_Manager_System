package com.groupt1.hms.controller;

import com.groupt1.hms.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Spring MVC controller for authentication pages.
 *
 * <p>Replaces the former {@code @RestController} that exposed {@code /api/auth/**}
 * JSON endpoints consumed by {@code auth.js}. Login, registration and the
 * password-reset wizard are now handled by server-side form submissions that
 * render the shared {@code free-hotel-website-template} views directly.
 */
@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    // ──────────────── LOGIN ────────────────

    @GetMapping("/login.html")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        Model model) {

        Map<String, Object> response = authService.login(username, password);

        if (Boolean.TRUE.equals(response.get("success"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) response.get("user");
            session.setAttribute("user", user);

            String redirectUrl = (String) response.get("redirectUrl");
            return "redirect:" + normalizeRedirect(redirectUrl);
        }

        model.addAttribute("errorMessage", response.get("message"));
        model.addAttribute("username", username);
        return "login";
    }

    // ──────────────── REGISTER ────────────────

    @GetMapping("/register.html")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam("fullname") String fullname,
                           @RequestParam("username") String username,
                           @RequestParam("email") String email,
                           @RequestParam("password") String password,
                           @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                           @RequestParam(value = "phone", required = false) String phone,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        // Server-side confirmation check (mirrors the old client-side validation).
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            addRegisterEcho(model, fullname, username, email, phone);
            return "register";
        }

        Map<String, Object> response = authService.register(fullname, username, email, password, phone);

        if (Boolean.TRUE.equals(response.get("success"))) {
            redirectAttributes.addFlashAttribute("successMessage", response.get("message") + " Please sign in.");
            return "redirect:/login.html";
        }

        model.addAttribute("errorMessage", response.get("message"));
        addRegisterEcho(model, fullname, username, email, phone);
        return "register";
    }

    // ──────────────── FORGOT PASSWORD (multi-step wizard) ────────────────

    @GetMapping("/forgot-password.html")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("step", 1);
        return "forgot-password";
    }

    @PostMapping("/forgot-password/send-code")
    public String sendResetCode(@RequestParam("email") String email,
                                HttpSession session,
                                Model model) {

        Map<String, Object> response = authService.forgotPassword(email);

        if (Boolean.TRUE.equals(response.get("success"))) {
            session.setAttribute("resetEmail", email);
            model.addAttribute("step", 2);
            model.addAttribute("resetEmail", email);
        } else {
            model.addAttribute("step", 1);
            model.addAttribute("errorMessage", response.get("message"));
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password/verify-code")
    public String verifyResetCode(@RequestParam("code") String code,
                                  HttpSession session,
                                  Model model) {

        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            model.addAttribute("step", 1);
            model.addAttribute("errorMessage", "Your session expired. Please request a new code.");
            return "forgot-password";
        }

        Map<String, Object> response = authService.verifyCode(email, code);

        if (Boolean.TRUE.equals(response.get("success"))) {
            model.addAttribute("step", 3);
        } else {
            model.addAttribute("step", 2);
            model.addAttribute("resetEmail", email);
            model.addAttribute("errorMessage", response.get("message"));
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password/reset")
    public String resetPassword(@RequestParam("newPassword") String newPassword,
                                @RequestParam(value = "confirmNewPassword", required = false) String confirmNewPassword,
                                HttpSession session,
                                Model model) {

        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            model.addAttribute("step", 1);
            model.addAttribute("errorMessage", "Your session expired. Please request a new code.");
            return "forgot-password";
        }

        if (confirmNewPassword != null && !newPassword.equals(confirmNewPassword)) {
            model.addAttribute("step", 3);
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "forgot-password";
        }

        Map<String, Object> response = authService.resetPassword(email, newPassword);

        if (Boolean.TRUE.equals(response.get("success"))) {
            session.removeAttribute("resetEmail");
            model.addAttribute("step", 4); // success step
        } else {
            model.addAttribute("step", 3);
            model.addAttribute("errorMessage", response.get("message"));
        }
        return "forgot-password";
    }

    // ──────────────── LOGOUT ────────────────

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ──────────────── Helpers ────────────────

    /** Ensure the redirect target from AuthService is an absolute, context-relative path. */
    private String normalizeRedirect(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return "/index.html";
        }
        return redirectUrl.startsWith("/") ? redirectUrl : "/" + redirectUrl;
    }

    private void addRegisterEcho(Model model, String fullname, String username, String email, String phone) {
        model.addAttribute("fullname", fullname);
        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone);
    }
}
