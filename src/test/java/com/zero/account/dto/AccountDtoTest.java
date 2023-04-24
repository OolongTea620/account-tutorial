package com.zero.account.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccountDtoTest {

    @Test
    void accountDtoTest() {
        AccountDto accountDto = new AccountDto();
        accountDto.setAccountNumber("accountNumber");
        System.out.println(accountDto.getAccountNumber());
        System.out.println(accountDto.toString());
    }

}