package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
   List<Wallet> findByUserId(Long userId);
   Optional<Wallet> findByPublicId(UUID publicId);
}