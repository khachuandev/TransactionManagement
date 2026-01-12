package com.example.Transaction.service;

import com.example.Transaction.dto.request.TransactionRequest;
import com.example.Transaction.dto.response.TransactionHistoryResponse;
import com.example.Transaction.dto.response.TransactionResponse;

import java.util.List;

public interface ITransactionService {
    TransactionResponse processTransfer(TransactionRequest request);
    List<TransactionHistoryResponse> getTransactionsByTransactionId(String transactionId);
}
