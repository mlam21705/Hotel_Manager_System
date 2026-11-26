package com.groupt1.hms.service.impl;

import com.groupt1.hms.entity.Role;
import com.groupt1.hms.entity.UserAccount;
import com.groupt1.hms.enums.AccountStatus;
import com.groupt1.hms.repository.RoleRepository;
import com.groupt1.hms.repository.UserAccountRepository;
import com.groupt1.hms.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<UserAccount> findAllAccounts() {
        return userAccountRepository.findAll();
    }

    @Override
    public List<UserAccount> searchAccounts(String email, Integer roleId) {
        return userAccountRepository.searchAccounts(email, roleId);
    }

    @Override
    @Transactional
    public UserAccount saveAccount(String username, String email, String password, Integer roleId) throws Exception {
        if (userAccountRepository.existsByUsername(username)) {
            throw new Exception("Username is already taken.");
        }
        if (userAccountRepository.existsByEmail(email)) {
            throw new Exception("Email is already registered.");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new Exception("Role not found."));

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);

        return userAccountRepository.save(user);
    }

    @Override
    @Transactional
    public UserAccount updateAccount(Integer accountId, String email, String password, Integer roleId) throws Exception {
        UserAccount user = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new Exception("Account not found."));

        // If email changed, check for duplicates
        if (!user.getEmail().equalsIgnoreCase(email)) {
            if (userAccountRepository.existsByEmail(email)) {
                throw new Exception("Email is already registered by another account.");
            }
            user.setEmail(email);
        }

        if (password != null && !password.trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new Exception("Role not found."));
        user.setRole(role);

        return userAccountRepository.save(user);
    }

    @Override
    @Transactional
    public void updateAccountStatus(Integer accountId, AccountStatus newStatus) throws Exception {
        UserAccount user = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new Exception("Account not found."));
        user.setStatus(newStatus);
        userAccountRepository.save(user);
    }
}
