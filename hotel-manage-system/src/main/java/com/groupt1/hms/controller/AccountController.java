package com.groupt1.hms.controller;

import com.groupt1.hms.entity.UserAccount;
import com.groupt1.hms.enums.AccountStatus;
import com.groupt1.hms.repository.RoleRepository;
import com.groupt1.hms.service.AccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public String listAccounts(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "roleId", required = false) Integer roleId,
            Model model) {
        List<UserAccount> accounts;
        if ((email != null && !email.trim().isEmpty()) || roleId != null) {
            accounts = accountService.searchAccounts(email, roleId);
        } else {
            accounts = accountService.findAllAccounts();
        }
        model.addAttribute("accounts", accounts);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("activePage", "accounts");
        model.addAttribute("searchEmail", email);
        model.addAttribute("searchRoleId", roleId);
        return "admin/accounts";
    }

    @PostMapping("/save")
    public String saveAccount(
            @RequestParam(value = "accountId", required = false) Integer accountId,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam("roleId") Integer roleId,
            RedirectAttributes redirectAttributes) {

        try {
            if (accountId == null) {
                // Create new
                if (password == null || password.trim().isEmpty()) {
                    throw new Exception("Password is required for new accounts.");
                }
                accountService.saveAccount(username, email, password, roleId);
                redirectAttributes.addFlashAttribute("successMessage", "Account created successfully.");
            } else {
                // Update
                accountService.updateAccount(accountId, email, password, roleId);
                redirectAttributes.addFlashAttribute("successMessage", "Account updated successfully.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/status")
    public String updateStatus(
            @RequestParam("accountId") Integer accountId,
            @RequestParam("status") String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Check self-demote
            Map<String, Object> sessionUser = (Map<String, Object>) session.getAttribute("user");
            if (sessionUser != null) {
                Integer currentUserId = (Integer) sessionUser.get("userId");
                if (currentUserId != null && currentUserId.equals(accountId)) {
                    throw new Exception("You cannot change the status of your own account.");
                }
            }

            AccountStatus newStatus = AccountStatus.valueOf(status.toUpperCase());
            accountService.updateAccountStatus(accountId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Account status updated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status value.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/accounts";
    }
}
