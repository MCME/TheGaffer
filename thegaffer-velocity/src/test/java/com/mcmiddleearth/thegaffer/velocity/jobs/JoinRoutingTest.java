package com.mcmiddleearth.thegaffer.velocity.jobs;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinRoutingTest {

    private static Job job(String name, String server) {
        return new Job(name, "Gandalf", server, "a description");
    }

    @Test
    void transfersWhenTheNamedJobIsOnAnotherServer() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job join Minas_Tirith", "alpha", List.of(job("Minas_Tirith", "beta")));

        assertTrue(decision.isPresent(), "the job is on beta, the player is on alpha");
        assertEquals("beta", decision.get().targetServer());
        assertEquals("/job join Minas_Tirith", decision.get().commandToReplay());
    }

    @Test
    void staysOutOfTheWayWhenTheJobIsOnThePlayersOwnServer() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job join Minas_Tirith", "alpha", List.of(job("Minas_Tirith", "alpha")));

        assertTrue(decision.isEmpty(),
            "the backend owns this one: it has the job, the permissions and the ban list");
    }

    @Test
    void ignoresTheBareJoinFormWhichTheListenerHandlesItself() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job join", "alpha", List.of(job("Minas_Tirith", "beta")));

        assertTrue(decision.isEmpty(), "no name was typed, so there is nothing to resolve");
    }

    @Test
    void ignoresOtherJobSubcommandsThatTakeAJobName() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job check Minas_Tirith", "alpha", List.of(job("Minas_Tirith", "beta")));

        assertTrue(decision.isEmpty(), "only join is routed; check must not teleport anyone");
    }

    @Test
    void refusesToGuessWhenTwoServersRunAJobOfTheSameName() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job join Minas_Tirith", "alpha",
            List.of(job("Minas_Tirith", "beta"), job("Minas_Tirith", "gamma")));

        assertTrue(decision.isEmpty(), "moving someone to a coin-flip server is worse than not moving them");
    }

    @Test
    void refusesToGuessEvenWhenOneOfTheClashingJobsIsLocal() {
        // 'beta' deliberately first: the answer must not depend on iteration order.
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job join Minas_Tirith", "alpha",
            List.of(job("Minas_Tirith", "beta"), job("Minas_Tirith", "alpha")));

        assertTrue(decision.isEmpty(), "the player already stands on a server running that job");
    }

    @Test
    void matchesTheCommandLabelCaseInsensitivelyAsBukkitDoes() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "JOB Join Minas_Tirith", "alpha", List.of(job("Minas_Tirith", "beta")));

        assertTrue(decision.isPresent(), "/JOB Join works on the backend, so it must route here too");
        assertEquals("beta", decision.get().targetServer());
    }

    // --- regression guards: these passed the moment they were written. They are here so that
    // --- "let's make the name match case-insensitively" cannot quietly become a cross-server
    // --- transfer to a name the backend will then refuse.

    @Test
    void doesNotTransferOnACaseMismatchBecauseTheBackendWouldRejectTheName() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job join minas_tirith", "alpha", List.of(job("Minas_Tirith", "beta")));

        assertTrue(decision.isEmpty(), "backend keys jobs in a case-sensitive TreeMap");
    }

    @Test
    void doesNotTransferForAJobNobodyIsRunning() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job join Osgiliath", "alpha", List.of(job("Minas_Tirith", "beta")));

        assertTrue(decision.isEmpty(), "let the backend say its own piece about unknown jobs");
    }

    @Test
    void toleratesSloppyWhitespace() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "job   join    Minas_Tirith", "alpha", List.of(job("Minas_Tirith", "beta")));

        assertEquals("beta", decision.orElseThrow().targetServer());
    }

    @Test
    void ignoresCommandsThatMerelyMentionAJobJoin() {
        Optional<JoinRouting.Transfer> decision = JoinRouting.routeNamedJoin(
            "msg Frodo job join Minas_Tirith", "alpha", List.of(job("Minas_Tirith", "beta")));

        assertTrue(decision.isEmpty(), "this listener sees every command on the proxy");
    }
}
