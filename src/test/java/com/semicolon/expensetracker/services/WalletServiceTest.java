package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.Currency;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.CreateWalletRequest;
import com.semicolon.expensetracker.dtos.response.CreateWalletResponse;
import com.semicolon.expensetracker.dtos.response.WalletBalanceResponse;
import com.semicolon.expensetracker.dtos.response.WalletResponse;
import com.semicolon.expensetracker.exceptions.ForbiddenException;
import com.semicolon.expensetracker.exceptions.ResourceNotFoundException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.WalletMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    // ─── createWallet ─────────────────────────────────────────────────────────

    @Test
    void createWallet_withCurrencyProvided_savesWalletWithGivenCurrency() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setName("My Wallet");
        request.setCurrency(Currency.DOLLAR);

        User currentUser = userWithId(1L);
        UUID walletPublicId = UUID.randomUUID();

        Wallet savedWallet = new Wallet();
        savedWallet.setPublicId(walletPublicId);
        savedWallet.setName("My Wallet");
        savedWallet.setCurrency(Currency.DOLLAR);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

            CreateWalletResponse response = walletService.createWallet(request);

            assertThat(response.getPublicId()).isEqualTo(walletPublicId);
            assertThat(response.getName()).isEqualTo("My Wallet");
            assertThat(response.getCurrency()).isEqualTo(Currency.DOLLAR);
            assertThat(response.getMessage()).isEqualTo("Wallet created successfully");
        }
    }

    @Test
    void createWallet_withNullCurrency_defaultsToNaira() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setName("Naira Wallet");
        request.setCurrency(null);

        User currentUser = userWithId(1L);

        Wallet savedWallet = new Wallet();
        savedWallet.setPublicId(UUID.randomUUID());
        savedWallet.setName("Naira Wallet");
        savedWallet.setCurrency(Currency.NAIRA);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);

            ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
            when(walletRepository.save(captor.capture())).thenReturn(savedWallet);

            walletService.createWallet(request);

            assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.NAIRA);
        }
    }

    // ─── getWalletsByUser ─────────────────────────────────────────────────────

    @Test
    void getWalletsByUser_returnsAllWalletsForCurrentUser() {
        Long userId = 42L;
        Wallet w1 = walletWithOwner(1L, new User());
        Wallet w2 = walletWithOwner(2L, new User());
        WalletResponse r1 = WalletResponse.builder().name("W1").build();
        WalletResponse r2 = WalletResponse.builder().name("W2").build();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByUserId(userId)).thenReturn(List.of(w1, w2));
            when(walletMapper.toWalletResponse(w1)).thenReturn(r1);
            when(walletMapper.toWalletResponse(w2)).thenReturn(r2);

            List<WalletResponse> result = walletService.getWalletsByUser();

            assertThat(result).containsExactly(r1, r2);
        }
    }

    @Test
    void getWalletsByUser_noWallets_returnsEmptyList() {
        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(99L);
            when(walletRepository.findByUserId(99L)).thenReturn(List.of());

            List<WalletResponse> result = walletService.getWalletsByUser();

            assertThat(result).isEmpty();
        }
    }

    // ─── getWalletBalance ─────────────────────────────────────────────────────

    @Test
    void getWalletBalance_ownedWallet_returnsInflowMinusOutflow() {
        Long userId = 1L;
        UUID publicId = UUID.randomUUID();

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);

        WalletBalanceResponse expected = WalletBalanceResponse.builder()
                .balance(new BigDecimal("300.00")).build();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByPublicId(publicId)).thenReturn(Optional.of(wallet));
            when(expenseRepository.sumByWalletIdAndTypes(eq(10L),
                    eq(of(TransactionType.INFLOW)))).thenReturn(Optional.of(new BigDecimal("500")));
            when(expenseRepository.sumByWalletIdAndTypes(eq(10L),
                    eq(of(TransactionType.OUTFLOW, TransactionType.OUTFLOW_FIXED_COST,
                            TransactionType.OUTFLOW_VARIABLE_COST))))
                    .thenReturn(Optional.of(new BigDecimal("200")));
            when(walletMapper.toWalletBalanceResponse(wallet, new BigDecimal("300")))
                    .thenReturn(expected);

            WalletBalanceResponse result = walletService.getWalletBalance(publicId);

            assertThat(result).isEqualTo(expected);
        }
    }

    @Test
    void getWalletBalance_noExpenses_usesZeroAsDefaults() {
        Long userId = 1L;
        UUID publicId = UUID.randomUUID();

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByPublicId(publicId)).thenReturn(Optional.of(wallet));
            when(expenseRepository.sumByWalletIdAndTypes(eq(10L), any())).thenReturn(Optional.empty());
            when(walletMapper.toWalletBalanceResponse(eq(wallet), eq(BigDecimal.ZERO)))
                    .thenReturn(WalletBalanceResponse.builder().balance(BigDecimal.ZERO).build());

            WalletBalanceResponse result = walletService.getWalletBalance(publicId);

            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Test
    void getWalletBalance_walletNotFound_throwsResourceNotFoundException() {
        UUID publicId = UUID.randomUUID();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(walletRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getWalletBalance(publicId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wallet does not exist");
        }
    }

    @Test
    void getWalletBalance_walletBelongsToDifferentUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        Long anotherUserId = 2L;
        UUID publicId = UUID.randomUUID();

        User anotherUser = userWithId(anotherUserId);
        Wallet wallet = walletWithOwner(10L, anotherUser);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(walletRepository.findByPublicId(publicId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.getWalletBalance(publicId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Wallet does not belong to this user");
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Wallet walletWithOwner(Long walletId, User owner) {
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUser(owner);
        return wallet;
    }
}
