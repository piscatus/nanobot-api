package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.dto.UserWalletsResponseDto;
import com.nanobot.nanobotbackend.service.WalletServices;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

  private static final String PATH = "/requests/wallet";

  private MockMvc mockMvc;

  @Mock
  private WalletServices walletServices;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new WalletController(walletServices))
        .build();
  }

  @Test
  void walletShouldReturnOkAndDelegateToService() throws Exception {
    UserWalletsResponseDto response = new UserWalletsResponseDto();
    response.setUserWallets(Collections.emptyList());
    when(walletServices.wallet(any())).thenReturn(response);

    mockMvc
      .perform(
        post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.userWallets").isArray());

    verify(walletServices).wallet(any());
  }

  @Test
  void walletShouldReturnErrorResponseWhenServiceReturnsError() throws Exception {
    UserWalletsResponseDto errorResponse = new UserWalletsResponseDto(
      "Wallet lookup failed"
    );
    when(walletServices.wallet(any())).thenReturn(errorResponse);

    mockMvc
      .perform(
        post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Wallet lookup failed"));

    verify(walletServices).wallet(any());
  }
}
