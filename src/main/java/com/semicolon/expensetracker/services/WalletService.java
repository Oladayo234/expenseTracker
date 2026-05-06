package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.Currency;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.CreateWalletRequest;
import com.semicolon.expensetracker.dtos.response.CreateWalletResponse;
import com.semicolon.expensetracker.dtos.response.WalletBalanceResponse;
import com.semicolon.expensetracker.dtos.response.WalletResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.utils.mappers.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.List.of;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ExpenseRepository expenseRepository;
    private final WalletMapper walletMapper;

    public CreateWalletResponse createWallet(CreateWalletRequest request) {
        User user = validateWithUserId(request.getUserId());
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCurrency(request.getCurrency() != null ? request.getCurrency() : Currency.NAIRA);
        wallet.setName(request.getName());
        Wallet savedWallet = walletRepository.save(wallet);
        CreateWalletResponse response = new CreateWalletResponse();
        response.setName(savedWallet.getName());
        response.setCurrency(savedWallet.getCurrency());
        response.setMessage("Wallet created successfully");
        return response;
    }

    public List<WalletResponse> getWalletsByUser(UUID userId) {
        validateWithUserId(userId);
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        List<WalletResponse> responses = new ArrayList<>();
        for (Wallet wallet : wallets) {
            responses.add(walletMapper.toWalletResponse(wallet));
        }
        return responses;
    }

    public WalletBalanceResponse getWalletBalance(UUID walletId) {
        Wallet wallet = validateWithWalletById(walletId);
        BigDecimal inflow = expenseRepository.sumByWalletIdAndTypes(walletId,
                of(TransactionType.INFLOW)).orElse(BigDecimal.ZERO);
        BigDecimal outflow = expenseRepository.sumByWalletIdAndTypes(walletId,
                of(TransactionType.OUTFLOW, TransactionType.OUTFLOW_FIXED_COST, TransactionType.OUTFLOW_VARIABLE_COST)).orElse(BigDecimal.ZERO);
        return walletMapper.toWalletBalanceResponse(wallet, inflow.subtract(outflow));
    }

    private User validateWithUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidEntryException("User does not exist"));
    }

    private Wallet validateWithWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new InvalidEntryException("Wallet does not exist"));
    }
}