package com.groupt1.hms.service;

import com.groupt1.hms.entity.UserAccount;
import com.groupt1.hms.enums.AccountStatus;

import java.util.List;

public interface AccountService {
    List<UserAccount> findAllAccounts();
    List<UserAccount> searchAccounts(String email, Integer roleId);
    UserAccount saveAccount(String username, String email, String password, Integer roleId) throws Exception;
    UserAccount updateAccount(Integer accountId, String email, String password, Integer roleId) throws Exception;
    void updateAccountStatus(Integer accountId, AccountStatus newStatus) throws Exception;
}
