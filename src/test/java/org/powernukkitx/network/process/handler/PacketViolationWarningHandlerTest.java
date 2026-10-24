package org.powernukkitx.network.process.handler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The violation context is free text the client picks, and the handler that logs it runs before the
 * session has authenticated, so what reaches the log has to stay one bounded line.
 */
class PacketViolationWarningHandlerTest {

    private static final int MAX_CONTEXT_LENGTH = 256;

    @Test
    void saysSoWhenThereIsNoContext() {
        assertEquals("no context", PacketViolationWarningHandler.describeContext(null));
        assertEquals("no context", PacketViolationWarningHandler.describeContext(""));
    }

    @Test
    void keepsAShortContextAsItIs() {
        assertEquals("unknown packet id 42", PacketViolationWarningHandler.describeContext("unknown packet id 42"));
    }

    @Test
    void cutsALongContextDown() {
        String context = "x".repeat(MAX_CONTEXT_LENGTH * 4);

        String described = PacketViolationWarningHandler.describeContext(context);

        assertEquals(MAX_CONTEXT_LENGTH + 3, described.length());
        assertTrue(described.endsWith("..."));
    }

    @Test
    void leavesNoLineBreakToForgeALogEntryWith() {
        String described = PacketViolationWarningHandler.describeContext(
            "harmless\n2026-01-01 INFO  forged line\r\nand another");

        assertEquals(-1, described.indexOf('\n'));
        assertEquals(-1, described.indexOf('\r'));
        assertEquals("harmless 2026-01-01 INFO  forged line  and another", described);
    }

    @Test
    void cutsBeforeItStripsSoTheLineStaysBounded() {
        String described = PacketViolationWarningHandler.describeContext("\n".repeat(MAX_CONTEXT_LENGTH * 2));

        assertEquals(MAX_CONTEXT_LENGTH + 3, described.length());
        assertEquals(-1, described.indexOf('\n'));
    }
}
