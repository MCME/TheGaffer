package com.mcmiddleearth.thegaffer.utilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptStyleTest {

    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void tagContainsJobs() {
        assertTrue(PromptStyle.TAG.contains("Jobs"),
                "TAG should contain the word 'Jobs'");
    }

    @Test
    void askStartsWithWhiteCode() {
        String result = PromptStyle.ask("Hello");
        assertTrue(result.startsWith(ChatColor.WHITE.toString()),
                "ask() should start with the WHITE colour code");
    }

    @Test
    void askContainsQuestion() {
        String result = PromptStyle.ask("Hello");
        assertTrue(result.contains("Hello"),
                "ask() should contain the question text");
    }

    @Test
    void optsContainsText() {
        String result = PromptStyle.opts("true / false");
        assertTrue(result.contains("true / false"),
                "opts() should contain the options text");
    }

    @Test
    void hintStartsWithNewline() {
        String result = PromptStyle.hint("x");
        assertTrue(result.startsWith("\n"),
                "hint() should start with a newline");
    }

    @Test
    void hintContainsText() {
        String result = PromptStyle.hint("example hint");
        assertTrue(result.contains("example hint"),
                "hint() should contain the hint text");
    }
}
