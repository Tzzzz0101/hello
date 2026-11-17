package com.lab.emp;

import com.lab.emp.util.PasswordUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 口令摘要工具单元测试（不依赖 Spring 容器）。
 */
public class PasswordUtilTest {

    /** 123456 的 SHA-256 标准值，用于确认算法实现与数据库种子数据一致 */
    private static final String SHA256_OF_123456 =
            "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

    @Test
    public void sha256ShouldMatchKnownDigest() {
        assertEquals(SHA256_OF_123456, PasswordUtil.sha256Hex("123456"));
    }

    @Test
    public void matchesShouldVerifyCorrectPassword() {
        assertTrue(PasswordUtil.matches("123456", SHA256_OF_123456));
    }

    @Test
    public void matchesShouldRejectWrongPassword() {
        assertFalse(PasswordUtil.matches("1234567", SHA256_OF_123456));
        assertFalse(PasswordUtil.matches(null, SHA256_OF_123456));
        assertFalse(PasswordUtil.matches("123456", null));
    }
}