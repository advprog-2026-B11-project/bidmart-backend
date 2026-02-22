package com.example.bidmart.controller;

import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.repository.WalletRepository;
import com.example.bidmart.wallet.controller.WalletController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletController walletController;

    @Test
    void testGetAllWallets() {
        Wallet dummyWallet = new Wallet("PembeliLelang", new BigDecimal("10000.00"));
        when(walletRepository.findAll()).thenReturn(Arrays.asList(dummyWallet));

        ResponseEntity<List<Wallet>> response = walletController.getAllWallets();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("PembeliLelang", response.getBody().get(0).getUsername());
    }

    @Test
    void testCreateDummyWallet() {
        Wallet dummyWallet = new Wallet("PembeliBaru", new BigDecimal("10000.00"));
        when(walletRepository.save(any(Wallet.class))).thenReturn(dummyWallet);

        ResponseEntity<Wallet> response = walletController.createDummyWallet("PembeliBaru");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("PembeliBaru", response.getBody().getUsername());
        assertEquals(new BigDecimal("10000.00"), response.getBody().getBalance());
    }
}