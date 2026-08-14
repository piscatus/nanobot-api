package com.nanobot.nanobotbackend.util;

import java.security.SecureRandom;
import uk.oczadly.karl.jnano.model.HexData;
import uk.oczadly.karl.jnano.model.NanoAccount;
import uk.oczadly.karl.jnano.util.WalletUtil;

public class CryptoUtil {

    public static String deriveAddressFromSeed(String seed, String ticker) {
        HexData privateKey = WalletUtil.deriveKeyFromSeed(seed);
        NanoAccount account = NanoAccount.fromPrivateKey(privateKey);
        return ticker.equalsIgnoreCase("BAN")
            ? account.toAddress().replace("nano", "ban")
            : account.toAddress();
    }

    public static NanoAccount getAccountFromSeed(String seed) {
        HexData privateKey = WalletUtil.deriveKeyFromSeed(seed);
        return NanoAccount.fromPrivateKey(privateKey);
    }

    public static HexData getPrivateKeyFromSeed(String seedHex) {
        return WalletUtil.deriveKeyFromSeed(seedHex);
    }

    public static String getRandomKey() {
        return WalletUtil.generateRandomKey(new SecureRandom()).toString();
    }
}
