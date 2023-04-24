package com.zero.account.controller;

import com.zero.account.domain.TransactionDto;
import com.zero.account.dto.CancelBalance;
import com.zero.account.dto.UseBalance;
import com.zero.account.exception.AccountException;
import com.zero.account.service.TransactionService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 잔액 사용 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("transaction/use")
    public UseBalance.Response useBalance(
        @Valid @RequestBody UseBalance.Request request
    ){
        try {
            return UseBalance.Response.from(transactionService.useBalance(
                request.getUserId(),
                request.getAccountNumber(), request.getAmount()));
        }catch (AccountException e) {
            log.error("Failed ");

            // 거래 실패 기록
            transactionService.saveFailedUseTransaction(
                request.getAccountNumber(),
                request.getAmount()
            );
            throw e; // 에러를 밖으로 던짐
        }
    }

    @PostMapping("transaction/cancel")
    public CancelBalance.Response cancelBalance(
        @Valid @RequestBody CancelBalance.Request request
    ){
        try {
            return CancelBalance.Response.from(
                transactionService.cancelBalance(request.getTransactionId(),
                    request.getAccountNumber(), request.getAmount()));
        }catch (AccountException e) {
            log.error("Failed ");

            // 거래 실패 기록
            transactionService.saveFailedCancelTransaction(
                request.getAccountNumber(),
                request.getAmount()
            );
            throw e; // 에러를 밖으로 던짐
        }
    }
}
