package com.company.rpw.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "3oa5sggvvf5tZ0Dv368aYL0lGqU5HLKUHFYshQcA7PmWkraK56sSrNkr5sJ74y0olm4xEtqlU7DjiOjyFQIFdw==");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400L);
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("testUser", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testGetUsernameFromToken() {
        String token = jwtUtil.generateToken("admin", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        String extracted = jwtUtil.getUsernameFromToken(token);
        assertEquals("admin", extracted);
    }

    @Test
    void testInvalidTokenReturnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid-token"));
    }

    @Test
    void testNullTokenReturnsFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void testGetAuthentication() {
        String token = jwtUtil.generateToken("testUser", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var auth = jwtUtil.getAuthentication(token);
        assertNotNull(auth);
        assertEquals("testUser", auth.getName());
    }
}
