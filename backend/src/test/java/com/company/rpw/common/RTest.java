package com.company.rpw.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一响应 R 单元测试
 */
class RTest {

    @Test
    void testOk() {
        R<String> result = R.ok("test data");
        assertEquals(200, result.getCode());
        assertEquals("test data", result.getData());
    }

    @Test
    void testOkNoData() {
        R<Void> result = R.ok();
        assertEquals(200, result.getCode());
    }

    @Test
    void testFail() {
        R<String> result = R.fail("error message");
        assertEquals(500, result.getCode());
        assertEquals("error message", result.getMessage());
    }

    @Test
    void testFailWithCode() {
        R<String> result = R.fail(400, "bad request");
        assertEquals(400, result.getCode());
        assertEquals("bad request", result.getMessage());
    }
}
