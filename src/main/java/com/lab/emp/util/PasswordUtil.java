package com.lab.emp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 口令摘要工具。
 * <p>实验环境使用 SHA-256 便于观察与验证；生产环境应使用带盐、可调迭代次数的
 * BCrypt / Argon2，并禁止把口令明文或摘要写入日志。</p>
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String sha256Hex(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("原始口令不能为空");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                sha256Hex(rawPassword).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}