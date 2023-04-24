package com.zero.account.controller;

import static com.zero.account.type.TransactionResultType.S;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zero.account.domain.TransactionDto;
import com.zero.account.dto.CancelBalance;
import com.zero.account.dto.UseBalance;
import com.zero.account.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successUseBalance() throws Exception {
        //given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
            .willReturn(TransactionDto.builder()
                .accountNumber("1000000000")
                .amount(12345L)
                .transactionId("transactionId")
                .transactionResultType(S)
                .build());
        //when

        //then
        mockMvc.perform(post("/transaction/use")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new UseBalance.Request(1L,
                    "1000000000", 3000L)
            ))
        ).andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountNumber").value("1000000000"))
        .andExpect(jsonPath("$.transactionResult").value("S"))
        .andExpect(jsonPath("$.amount").value(12345L))
        .andExpect(jsonPath("$.transactionId").value("transactionId"));
    }


    @Test
    void successCancelBalance() throws Exception {
        //given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
            .willReturn(TransactionDto.builder()
                .accountNumber("1000000000")
                .amount(54321L)
                .transactionId("transactionIdForCancel")
                .transactionResultType(S)
                .build());
        //when

        //then
        mockMvc.perform(post("/transaction/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CancelBalance.Request("transactionId","1000000000", 3000L)
                ))
            ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("1000000000"))
            .andExpect(jsonPath("$.transactionResult").value("S"))
            .andExpect(jsonPath("$.amount").value(54321L))
            .andExpect(jsonPath("$.transactionId").value("transactionIdForCancel"));
    }
}