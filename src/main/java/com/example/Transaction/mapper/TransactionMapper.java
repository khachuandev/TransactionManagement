package com.example.Transaction.mapper;

import com.example.Transaction.dto.response.TransactionHistoryResponse;
import com.example.Transaction.entity.TransactionHistory;
import com.example.Transaction.util.AESUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionMapper {
    private final AESUtils aesUtils;

    public TransactionHistoryResponse toHistoryResponse(TransactionHistory th) {
        return TransactionHistoryResponse.builder()
                .transactionId(th.getTransactionId())
                .account(aesUtils.decryptFromDB(th.getAccount()))
                .inDebt(th.getInDebt())
                .have(th.getHave())
                .time(th.getTime())
                .build();
    }
}
