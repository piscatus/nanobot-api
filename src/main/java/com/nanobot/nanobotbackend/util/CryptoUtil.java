package com.nanobot.nanobotbackend.util;

import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import uk.oczadly.karl.jnano.model.HexData;
import uk.oczadly.karl.jnano.model.NanoAccount;
import uk.oczadly.karl.jnano.util.WalletUtil;

public class CryptoUtil {

    public static final long MAX_ACCOUNT_INDEX = 0xFFFFFFFFL;

    public static boolean isValidPrivateKey(String privateKey) {
        return privateKey != null && privateKey.matches("[0-9a-fA-F]{64}");
    }

    public static boolean isValidIndex(Long index) {
        return index != null && index >= 0L && index <= MAX_ACCOUNT_INDEX;
    }

    public static boolean canResolvePrivateKey(String seed, String privateKey) {
        return isValidPrivateKey(privateKey) || seed != null;
    }

    public static HexData resolvePrivateKey(
        String seed,
        Long index,
        String privateKey
    ) {
        if (isValidPrivateKey(privateKey)) {
            return new HexData(privateKey);
        }
        long resolvedIndex = isValidIndex(index) ? index : 0L;
        if (resolvedIndex == 0L) {
            return WalletUtil.deriveKeyFromSeed(seed);
        }
        return deriveKeyFromSeedWithIndex(seed, resolvedIndex);
    }

    public static HexData resolvePrivateKey(UserDetailsEntity userDetails) {
        return resolvePrivateKey(
            userDetails.getSeed(),
            userDetails.getIndex(),
            userDetails.getPrivateKey()
        );
    }

    public static HexData resolvePrivateKey(UserDetailsDto userDetails) {
        return resolvePrivateKey(
            userDetails.getSeed(),
            userDetails.getIndex(),
            userDetails.getPrivateKey()
        );
    }

    public static HexData resolvePrivateKey(QueueEntity queue) {
        return resolvePrivateKey(
            queue.getSeed(),
            queue.getIndex(),
            queue.getPrivateKey()
        );
    }

    public static String deriveAddressFromSeed(String seed, String ticker) {
        return deriveAddress(seed, null, null, ticker);
    }

    public static String deriveAddress(
        String seed,
        Long index,
        String privateKey,
        String ticker
    ) {
        NanoAccount account = NanoAccount.fromPrivateKey(
            resolvePrivateKey(seed, index, privateKey)
        );
        return formatAddress(account.toAddress(), ticker);
    }

    public static String deriveAddress(
        UserDetailsEntity userDetails,
        String ticker
    ) {
        return deriveAddress(
            userDetails.getSeed(),
            userDetails.getIndex(),
            userDetails.getPrivateKey(),
            ticker
        );
    }

    public static String deriveAddress(
        UserDetailsDto userDetails,
        String ticker
    ) {
        return deriveAddress(
            userDetails.getSeed(),
            userDetails.getIndex(),
            userDetails.getPrivateKey(),
            ticker
        );
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

    public static void applySigningMaterial(
        QueueDto queue,
        UserDetailsEntity userDetails
    ) {
        if (queue == null || userDetails == null) {
            return;
        }
        queue.setIndex(userDetails.getIndex());
        queue.setPrivateKey(userDetails.getPrivateKey());
    }

    static HexData deriveKeyFromSeedWithIndex(String seed, long index) {
        byte[] seedBytes = hexToBytes(seed);
        byte[] indexBytes = ByteBuffer.allocate(4).putInt((int) index).array();
        byte[] data = new byte[seedBytes.length + 4];
        System.arraycopy(seedBytes, 0, data, 0, seedBytes.length);
        System.arraycopy(indexBytes, 0, data, seedBytes.length, 4);
        Blake2bDigest digest = new Blake2bDigest(256);
        digest.update(data, 0, data.length);
        byte[] privateKey = new byte[32];
        digest.doFinal(privateKey, 0);
        return new HexData(bytesToHex(privateKey));
    }

    private static String formatAddress(String nanoAddress, String ticker) {
        return ticker.equalsIgnoreCase("BAN")
            ? nanoAddress.replace("nano", "ban")
            : nanoAddress;
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
