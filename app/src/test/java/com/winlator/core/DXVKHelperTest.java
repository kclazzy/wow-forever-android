package com.winlator.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DXVKHelperTest {

    @Test
    public void lowEndOptions_areSemicolonSeparatedWithoutQuotesOrNewlines() {
        String config = DXVKHelper.buildConfigString(new KeyValueSet("version=2.4.1-1,numCompilerThreads=2,maxDeviceMemory=2048"));
        assertEquals("dxgi.maxDeviceMemory = 2048;dxgi.maxSharedMemory = 2048;dxvk.numCompilerThreads = 2", config);
        assertFalse(config.contains("\""));
        assertFalse(config.contains("\n"));
    }

    @Test
    public void noOptions_givesAnEmptyString() {
        assertEquals("", DXVKHelper.buildConfigString(new KeyValueSet("version=2.4.1-1")));
    }

    @Test
    public void zeroMeansUnset() {
        assertEquals("", DXVKHelper.buildConfigString(new KeyValueSet("version=1,maxDeviceMemory=0,numCompilerThreads=0")));
    }
}
