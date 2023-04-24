package com.zero.account.controller;

import com.zero.account.domain.Account;
import com.zero.account.dto.AccountDto;
import com.zero.account.dto.AccountInfo;
import com.zero.account.dto.CreateAccount;
import com.zero.account.dto.DeleteAccount;
import com.zero.account.service.AccountService;
import com.zero.account.service.RedisTestService;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final RedisTestService redisTestService;

    @PostMapping("/account")
    public CreateAccount.Response createAccount(
        @RequestBody @Valid CreateAccount.Request request
    ) {
        return CreateAccount.Response.from(
            accountService.creatAccount(
                request.getUserId(),
                request.getInitialBalance()
            )
        );
    }

    @GetMapping("/account")
    public List<AccountInfo> getAccountsByUserId(
        @RequestParam("user_id") Long userId
    ) {
        return accountService.getAccountsbyUserId(userId)
            .stream().map(accountDto ->
                AccountInfo.builder()
                .accountNumber(accountDto.getAccountNumber())
                .balance(accountDto.getBalance())
                    .build())
            .collect(Collectors.toList());
    }
    @DeleteMapping ("/account")
    public DeleteAccount.Response createAccount(
        @RequestBody @Valid DeleteAccount.Request request
    ) {
        return DeleteAccount.Response.from(
            accountService.deleteAccount(
                request.getUserId(),
                request.getAccountNumber()
            )
        );
    }

    @GetMapping("/get-lock")
    public String getLock() {
       return redisTestService.getLock();
    }

    @GetMapping("/account/{id}")
    public Account getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }
}
