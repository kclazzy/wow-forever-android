package com.winlator.core;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProcessExitStatusTest {

    @Test
    public void zero_isACleanExit() {
        assertTrue(ProcessHelper.describeExitStatus(0).contains("clean exit"));
    }

    @Test
    public void smallNonZero_isTheProgramsOwnErrorCode() {
        assertTrue(ProcessHelper.describeExitStatus(1).contains("status=1"));
        assertTrue(ProcessHelper.describeExitStatus(1).contains("by itself"));
    }

    @Test
    public void status137_isSigkill() {
        assertTrue(ProcessHelper.describeExitStatus(137).contains("SIGKILL"));
    }

    @Test
    public void status139_isANativeCrash() {
        assertTrue(ProcessHelper.describeExitStatus(139).contains("SIGSEGV"));
    }

    @Test
    public void unknownSignal_isStillReported() {
        assertTrue(ProcessHelper.describeExitStatus(128 + 31).contains("signal 31"));
    }
}
