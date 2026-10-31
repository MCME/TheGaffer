package com.mcmiddleearth.thegaffer.integrations;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobDiscordAnnouncerTest {

    @Test
    void discordTimestampFormatsSecondsAndStyle() {
        // millis -> seconds, with the requested style suffix
        assertEquals("<t:2:R>", JobDiscordAnnouncer.discordTimestamp(2000L, 'R'));
        assertEquals("<t:1719774000:f>", JobDiscordAnnouncer.discordTimestamp(1719774000000L, 'f'));
        assertEquals("<t:0:R>", JobDiscordAnnouncer.discordTimestamp(0L, 'R'));
    }

    @Test
    void rolePingNeverMentionsAMemberWhoseNameStartsTheRoleName() {
        // What Discord showed: "@Jobber" became a mention of a member called "J", plus "obber".
        Guild guild = guild(List.of(), List.of(member("2", "J")));

        assertEquals("", JobDiscordAnnouncer.Bridge.rolePing(List.of("Jobber"), guild),
                "a role that isn't found pings nobody");
    }

    @Test
    void rolePingMentionsTheRoleEvenWhenAMemberNameStartsIt() {
        Guild guild = guild(List.of(role("1", "Jobber")), List.of(member("2", "J")));

        assertEquals("<@&1>", JobDiscordAnnouncer.Bridge.rolePing(List.of("jobber"), guild),
                "the role is found by its exact name, ignoring case");
    }

    @Test
    void rolePingJoinsSeveralRolesAndSkipsUnknownAndBlankNames() {
        Guild guild = guild(List.of(role("1", "Jobber"), role("3", "Builder")), List.of());

        assertEquals("<@&1> <@&3>", JobDiscordAnnouncer.Bridge.rolePing(
                Arrays.asList("Jobber", "", null, "Ghost", "Builder"), guild));
    }

    /** A JDA guild that answers only the role and member calls the ping code may make. */
    private static Guild guild(List<Role> roles, List<Member> members) {
        return (Guild) Proxy.newProxyInstance(Guild.class.getClassLoader(), new Class<?>[]{Guild.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRoles" -> roles;
                    case "getMembers" -> members;
                    case "getRolesByName" -> roles.stream()
                            .filter(r -> (Boolean) args[1]
                                    ? r.getName().equalsIgnoreCase((String) args[0])
                                    : r.getName().equals(args[0]))
                            .toList();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Guild";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Role role(String id, String name) {
        return (Role) Proxy.newProxyInstance(Role.class.getClassLoader(), new Class<?>[]{Role.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "getName" -> name;
                    case "getAsMention" -> "<@&" + id + ">";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Role " + name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Member member(String id, String effectiveName) {
        return (Member) Proxy.newProxyInstance(Member.class.getClassLoader(), new Class<?>[]{Member.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "getEffectiveName" -> effectiveName;
                    case "getAsMention" -> "<@" + id + ">";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Member " + effectiveName;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
