package com.ivillager;

import com.ivillager.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic tests that do not require a Bukkit/Paper runtime.
 * See TESTING.md for config parsing, command parsing, and UI tests (manual or with MockBukkit).
 */
class IVillagerPluginTest {

    @Test
    void configResultReturnsCopyOfShops() {
        ConfigLoader.ConfigResult result = new ConfigLoader.ConfigResult(Collections.emptyMap(), null);
        assertNotNull(result.getShops());
        assertEquals(0, result.getShops().size());
        assertEquals(null, result.getDefaultShop());
    }

    @Test
    void configResultPreservesDefaultShop() {
        ConfigLoader.ConfigResult result = new ConfigLoader.ConfigResult(Collections.emptyMap(), "default");
        assertEquals("default", result.getDefaultShop());
    }
}
