package com.bumbumapps.utility.pdfmerge.Review;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReviewPromptTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Test
    public void notBeforeThirdSuccess() {
        assertFalse(ReviewPrompt.shouldRequest(1, 5, 0, NOW));
        assertFalse(ReviewPrompt.shouldRequest(2, 5, 0, NOW));
        assertTrue(ReviewPrompt.shouldRequest(3, 5, 0, NOW));
        assertTrue(ReviewPrompt.shouldRequest(7, 5, 0, NOW));
    }

    @Test
    public void neverOnFirstLaunch() {
        assertFalse(ReviewPrompt.shouldRequest(3, 0, 0, NOW));
        assertFalse(ReviewPrompt.shouldRequest(10, 1, 0, NOW));
        assertTrue(ReviewPrompt.shouldRequest(3, 2, 0, NOW));
    }

    @Test
    public void localCooldownAfterARequest() {
        assertFalse(ReviewPrompt.shouldRequest(4, 3, NOW - DAY, NOW));
        assertFalse(ReviewPrompt.shouldRequest(4, 3, NOW - 29 * DAY, NOW));
        assertTrue(ReviewPrompt.shouldRequest(4, 3, NOW - 30 * DAY, NOW));
    }
}
