package com.zero.account.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.zero.account.domain.Account;
import com.zero.account.domain.AccountUser;
import com.zero.account.dto.AccountDto;
import com.zero.account.exception.AccountException;
import com.zero.account.repository.AccountUserRepository;
import com.zero.account.type.AccountStatus;
import com.zero.account.repository.AccountRepository;
import com.zero.account.type.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


//@SpringBootTest // 스프링부트 테스트를 위한 자동 주입 설정 - Junit
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    /**
     * @ Mock -> {@link AccountRepository}의 자동 생성을 @{@link InjectMocks} 내부 AccountRepository 내부에 자동 주입
     */
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    //@Autowired // 스프링부트 테스트를 위한 자동 주입 설정 - Junit
    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        //given : Mock 데이터 작성 목적
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
            .willReturn(Optional.of(Account.builder()
                .accountUser(user)
                .accountNumber("1000000012").build()));
        // save logic에 대한 Mocking
        given(accountRepository.save(any()))
            .willReturn(Account.builder()
                .accountUser(user)
                .accountNumber("100000013").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto =  accountService.creatAccount(1L,1000L);

        //then
        verify(accountRepository,times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("100000013", accountDto.getAccountNumber());
    }

    @Test
    @DisplayName("어떤 유저의 첫 계좌 생성 성공")
    void createFirstAccount() {
        AccountUser user = AccountUser.builder()
            .id(15L)
            .name("Pobi").build();
        //given : Mock 데이터 작성 목적

        // AccountUser Mock
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));

        // 계좌를 처음 생성한다고 가정 (기존 생성 계좌가 없다고 가정)
        given(accountRepository.findFirstByOrderByIdDesc())
            .willReturn(Optional.empty());

        // save logic에 대한 Mocking
        given(accountRepository.save(any()))
            .willReturn(Account.builder()
                .accountNumber("1000000000")
                .accountUser(user)
                .build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto =  accountService.creatAccount(1L,1000L);

        //then
        verify(accountRepository,times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", accountDto.getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_UserNotFound() {
        AccountUser user = AccountUser.builder()
            .id(15L)
            .name("HasTenUser").build();
        //given : Mock 데이터 작성 목적
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty()); // 유저가 없는 경우
        //when
        // AccountException 발생
        AccountException accountException = assertThrows(AccountException.class,
            () -> accountService.creatAccount(1L,1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("유저 당 최대 보유 계좌 가능 수는 10개")
    void createAccount_maxAccountIs10() {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user)); // user Mocking
        given(accountRepository.countByAccountUser(any()))
            .willReturn(10); // 이미 10개가 있다고 Mocking
        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> accountService.creatAccount(1L,1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 삭제 성공")
    void deleteAccountSuccess() {
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        //given : Mock 데이터 작성 목적
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        // 계좌 정보 생성, 잔고는 0
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(user)
                .balance(0L)
                .accountNumber("1000000012").build()));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto =  accountService.deleteAccount(12L,"1000000012");

        //then
        verify(accountRepository,times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccount_UserNotFound() {
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty()); // 유저가 없는 경우
        //when
        // AccountException 발생
        AccountException accountException = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L,"1234567890"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccount_AccountNotFound() {
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        //given : Mock 데이터 작성 목적
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        // 계좌 정보 생성, 잔고는 0
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());

        //when
        AccountException accountException =  assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void deleteAccountFailed_userUnMatch() {
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();

        AccountUser harry = AccountUser.builder()
            .id(13L)
            .name("Harry").build();

        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(harry).balance(0L)
                .accountNumber("1000000012").build()));

        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 잔액이 없어야 한다")
    void deleteAccountFailed_balanceNotEmpty() {
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(pobi)
                .balance(100L)
                .accountNumber("1000000012").build()));

        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    void successGetAccountsByUserId() {
        //given
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        List<Account> accounts = Arrays.asList(
            Account.builder()
                .accountUser(pobi)
                .accountNumber("1234567890")
                .balance(1000L)
                .build(),
            Account.builder()
                .accountUser(pobi)
                .accountNumber("3456789012")
                .balance(2000L)
                .build(),
            Account.builder()
                .accountUser(pobi)
                .accountNumber("5678901234")
                .balance(3000L)
                .build()
        );
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountUser(any()))
            .willReturn(accounts);
        //when
        List<AccountDto> accountDtos = accountService.getAccountsbyUserId(1L);

        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1234567890", accountDtos.get(0).getAccountNumber());
        assertEquals(1000, accountDtos.get(0).getBalance());
        assertEquals("3456789012", accountDtos.get(1).getAccountNumber());
        assertEquals(2000, accountDtos.get(1).getBalance());
        assertEquals("5678901234", accountDtos.get(2).getAccountNumber());
        assertEquals(3000, accountDtos.get(2).getBalance());
    }

    @Test
    @DisplayName("유저가 없는 유저인 경우")
    void faliedToGetAccounts() {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> accountService.getAccountsbyUserId(1L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }
}