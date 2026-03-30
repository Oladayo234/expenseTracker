package com.semicolon.expensetracker.utils.mappers;

import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.dtos.response.WalletBalanceResponse;
import com.semicolon.expensetracker.dtos.response.WalletResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WalletMapper {

    public WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .name(wallet.getName())
                .currency(wallet.getCurrency())
                .build();
    }

    public WalletBalanceResponse toWalletBalanceResponse(Wallet wallet, BigDecimal balance) {
        return WalletBalanceResponse.builder()
                .walletId(wallet.getId())
                .walletName(wallet.getName())
                .currency(wallet.getCurrency())
                .balance(balance)
                .build();
    }
}