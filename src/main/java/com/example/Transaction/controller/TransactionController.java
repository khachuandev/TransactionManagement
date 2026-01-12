package com.example.Transaction.controller;

import com.example.Transaction.dto.request.TransactionRequest;
import com.example.Transaction.dto.response.ApiRes;
import com.example.Transaction.dto.response.TransactionHistoryResponse;
import com.example.Transaction.dto.response.TransactionResponse;
import com.example.Transaction.service.ITransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/transactions")
@Tag(name = "Transaction", description = "Transaction APIs")
public class TransactionController {
    private final ITransactionService transactionService;

    /**
     * API xử lý giao dịch chuyển khoản
     * Tất cả parameters trong request đã được mã hóa RSA
     */
    @Operation(summary = "Thực hiện giao dịch chuyển khoản")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Giao dịch thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Giao dịch thất bại do request không hợp lệ",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống, không thể xử lý giao dịch",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/transfer")
    public ResponseEntity<ApiRes<TransactionResponse>> processTransfer(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.processTransfer(request);
        return ResponseEntity.ok(ApiRes.success(response));
    }

    /**
     * API lấy tất cả lịch sử giao dịch theo TransactionID
     * TransactionID được truyền vào là plain text, service sẽ handle tìm và decrypt
     */
    @Operation(summary = "Lấy danh sách lịch sử giao dịch theo TransactionID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy lịch sử giao dịch thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionHistoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "TransactionID không hợp lệ",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống khi truy xuất lịch sử giao dịch",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/history")
    public ResponseEntity<ApiRes<List<TransactionHistoryResponse>>> getTransactionsByTransactionId(
            @RequestParam String transactionId) {
        List<TransactionHistoryResponse> historyList =
                transactionService.getTransactionsByTransactionId(transactionId);
        return ResponseEntity.ok(ApiRes.success(historyList));
    }
}
