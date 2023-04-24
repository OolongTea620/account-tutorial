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
import com.zero.account.domain.Transaction;
import com.zero.account.domain.TransactionDto;
import com.zero.account.exception.AccountException;
import com.zero.account.repository.AccountRepository;
import com.zero.account.repository.AccountUserRepository;
import com.zero.account.repository.TransactionRepository;
import com.zero.account.type.AccountStatus;
import com.zero.account.type.ErrorCode;
import com.zero.account.type.TransactionResultType;
import com.zero.account.type.TransactionType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
        void successUseBalance() {
            //given
            AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();

            Account account =  Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

            //given : Mock 데이터 작성 목적
            given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

            given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

            given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                    .account(account)
                    .transactionResultType(TransactionResultType.S)
                    .transactionType(TransactionType.USE)
                    .transactionId("transactionId")
                    .transactedAt(LocalDateTime.now())
                    .amount(1000L)
                    .balanceSnapshot(9000L)
                    .build());
            ArgumentCaptor<Transaction> captor =  ArgumentCaptor.forClass(Transaction.class);
            //when
            TransactionDto transactionDto = transactionService.useBalance(
                1L, "1000000012", 1000L);

            //then
            verify(transactionRepository, times(1)).save(captor.capture());
            assertEquals(1000L, captor.getValue().getAmount());
            assertEquals(9000L, captor.getValue().getBalanceSnapshot());
            assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
            assertEquals(TransactionType.USE, transactionDto.getTransactionType());
            assertEquals(9000L, transactionDto.getBalanceSnapshot());
            assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1234567890", 12345L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void failUseBalance_AccountNotFound() {
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        //given : Mock 데이터 작성 목적
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());
        // 계좌 정보 생성, 잔고는 0
        //when
        AccountException exception =  assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1234567890", 12345L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void useBalanceFailed_userUnMatch() {
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
                .accountUser(harry).balance(10000L)
                .accountNumber("1000000012").build()));

        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(12L, "1000000012", 12345L));

        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("해지 계좌는 결제가 불가능 하다")
    void useBalanceFail_alreadyUnregistered() {
        //given
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(pobi).balance(0L)
                .accountStatus(AccountStatus.UNREGISTERED)
                .accountNumber("1000000012").build()));
        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(12L, "1000000012", 12345L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래금액이 잔액보다 큰 경우")
    void failUseBalance() {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();

        Account account =  Account.builder()
            .accountUser(user)
            .accountStatus(AccountStatus.IN_USE)
            .balance(100L)
            .accountNumber("1000000012").build();

        //given : Mock 데이터 작성 목적
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1000000012", 12345L));

        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());

    }


    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();

        Account account =  Account.builder()
            .accountUser(user)
            .accountStatus(AccountStatus.IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                .account(account)
                .transactionResultType(TransactionResultType.S)
                .transactionType(TransactionType.USE)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build());
        ArgumentCaptor<Transaction> captor =  ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction("1000000012", 1000L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.F, captor.getValue().getTransactionResultType());
    }

    @Test
    void successCancelBalance() {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();

        Account account = Account.builder()
            .accountUser(user)
            .accountStatus(AccountStatus.IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();

        Transaction transaction = Transaction.builder()
            .account(account)
            .transactionResultType(TransactionResultType.S)
            .transactionType(TransactionType.USE)
            .transactionId("transactionIdForCancel")
            .transactedAt(LocalDateTime.now())
            .amount(1000L)
            .balanceSnapshot(9000L)
            .build();

        given(transactionRepository.findById(anyLong()))
            .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                .account(account)
                .transactionResultType(TransactionResultType.S)
                .transactionType(TransactionType.CANCEL)
                .transactionId("transactionIdForCancel")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(10000L)
                .build());
        ArgumentCaptor<Transaction> captor =  ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.cancelBalance(
            "transactionIdForCancel",
            "1000000012", 1000L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(10000L + 1000L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.CANCEL, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_AccountNotFound() {
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        //given : Mock 데이터 작성 목적

        Transaction transaction = Transaction.builder().build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());
        // 계좌 정보 생성, 잔고는 0
        //when
        AccountException exception =  assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId", "1234567890", 12345L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 거래 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionNotFound() {
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        //given : Mock 데이터 작성 목적

        Transaction transaction = Transaction.builder().build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());
        // 계좌 정보 생성, 잔고는 0
        //when
        AccountException exception =  assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId", "1234567890", 12345L));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

}