package com.groupt1.hms.controller;

import com.groupt1.hms.dto.UserProfileDTO;
import com.groupt1.hms.entity.UserAccount;
import com.groupt1.hms.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionUser = (Map<String, Object>) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login.html";
        }

        try {
            // Load user profile only if it's not already in the model (e.g., from a failed update attempt)
            if (!model.containsAttribute("userProfile")) {
                Integer userId = (Integer) sessionUser.get("userId");
                UserProfileDTO userProfile = profileService.getUserProfile(userId);
                model.addAttribute("userProfile", userProfile);
            }
        } catch (Exception e) {
            log.error("Error loading profile: ", e);
            model.addAttribute("errorMessage", "Could not load profile information.");
        }

        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("userProfile") UserProfileDTO userProfileDTO,
                                BindingResult bindingResult,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionUser = (Map<String, Object>) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login.html";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userProfile", bindingResult);
            redirectAttributes.addFlashAttribute("userProfile", userProfileDTO);
            redirectAttributes.addFlashAttribute("errorMessage", "Please check your inputs.");
            return "redirect:/profile";
        }

        try {
            Integer userId = (Integer) sessionUser.get("userId");
            profileService.updateUserProfile(userId, userProfileDTO);
            
            // Update session if email was changed (since email is sometimes used in session or for display)
            String currentEmail = (String) sessionUser.get("email");
            if (currentEmail != null && !currentEmail.equalsIgnoreCase(userProfileDTO.getEmail())) {
                sessionUser.put("email", userProfileDTO.getEmail());
                session.setAttribute("user", sessionUser);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (Exception e) {
            log.error("Error updating profile: ", e);
            redirectAttributes.addFlashAttribute("userProfile", userProfileDTO);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }
}
