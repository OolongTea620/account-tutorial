package com.zero.account.service;

import static com.zero.account.type.ErrorCode.*;

import com.zero.account.domain.Account;
import com.zero.account.domain.AccountUser;
import com.zero.account.dto.AccountDto;
import com.zero.account.exception.AccountException;
import com.zero.account.repository.AccountRepository;
import com.zero.account.repository.AccountUserRepository;
import com.zero.account.type.AccountStatus;
import com.zero.account.type.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // 꼭 필요한 요소를 (private 타입) 삽입
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;
    /**
     * 사용자가 있는지 확인
     * 계좌의 번호 생성
     * 계좌를 저장하고, 그 정보를 넘긴다.
     * @param userId userId
     * @param initialBalance
     */
    @Transactional
    public AccountDto creatAccount(Long userId, Long initialBalance) {
        // 유저가 없으면 에러 발생
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        if(accountRepository.countByAccountUser(accountUser) == 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
        // 새 계정 생성 시, 최근 계좌 번호 + 1의 값으로 계좌 번호 생성
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
            .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
            .orElse("1000000000");

        return AccountDto.fromEntity(
            accountRepository.save(
                Account.builder()
                    .accountUser(accountUser)
                    .accountStatus(AccountStatus.IN_USE)
                    .accountNumber(newAccountNumber)
                    .balance(initialBalance)
                    .registeredAt(LocalDateTime.now())
                    .build())
        );
    }
    @Transactional
    public Account getAccount(Long id) {
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        Account account =  accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validDeleteAccount(accountUser, account);

        accountRepository.save(account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());
        return AccountDto.fromEntity(account);
    }

    private void validDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsbyUserId(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        List<Account> accounts = accountRepository
            .findByAccountUser(accountUser);

        return accounts.stream()
            .map(AccountDto::fromEntity)
            .collect(Collectors.toList());
    }
}
