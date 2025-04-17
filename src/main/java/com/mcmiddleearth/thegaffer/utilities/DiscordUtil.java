package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscordUtil {

    public static void sendDiscord(String message) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        ParseResult result = parseMentions(message);
        List<String> pingable = TheGaffer.getPingableDiscordRoles().stream()
                .map(role->guild.getRolesByName(role,true).getFirst().getId()).toList();
        if ((TheGaffer.getDiscordChannel() != null) && (!TheGaffer.getDiscordChannel().isEmpty()))
        {
            DiscordSRV discordPlugin = DiscordSRV.getPlugin();
            if (discordPlugin != null)
            {
                TextChannel channel = discordPlugin.getDestinationTextChannelForGameChannelName(TheGaffer.getDiscordChannel());
                if (channel != null) {
                            channel.sendMessage(result.messageWithMentions)
                            //.allowedMentions(Arrays.asList(Message.MentionType.ROLE,Message.MentionType.USER)) <- allows to ping all roles and users
                            .allowedMentions(Collections.singletonList(Message.MentionType.USER))
                            .mentionRoles(pingable.toArray(new String[0]))
                            .queue();
                } else {
                    Logger.getLogger("TheGaffer").warning("Discord channel not found.");
                }
            }
            else
            {
                Logger.getLogger("TheGaffer").warning("DiscordSRV plugin not found.");
            }
        }
    }

    public static ParseResult parseMentions(String message) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        ParseResult parseResult = new ParseResult();
        Pattern pattern = Pattern.compile("@(\"[^\"]+\"|\\w+)");
        Matcher matcher = pattern.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String original = matcher.group(0);  // z.B. @"Max Mustermann" oder @Hans
            String name = matcher.group(1);      // nur der Name oder der Text in Anführungszeichen
            if (name.startsWith("\"") && name.endsWith("\"")) {
                name = name.substring(1, name.length() - 1); // Anführungszeichen entfernen
            }
            String ersatz = "";
            for(Member member: guild.getMembersByEffectiveName(name,true)) {
                parseResult.userMentions.add(member.getId());
                if(!ersatz.isEmpty()) ersatz = ersatz + ", ";
                ersatz = ersatz + "<@"+member.getId()+">";
            }
            for(Role role: guild.getRolesByName(name,true)) {
                parseResult.roleMentions.add(role.getId());
                if(!ersatz.isEmpty()) ersatz = ersatz + ", ";
                ersatz = ersatz + "<@&"+role.getId()+">";
            }
            if(ersatz.isEmpty()) ersatz = "@"+name;
            matcher.appendReplacement(result, Matcher.quoteReplacement(ersatz));
        }

        matcher.appendTail(result);
        parseResult.messageWithMentions = result.toString();
        return parseResult;

    }

    public static class ParseResult {
        String messageWithMentions;
        List<String> roleMentions;
        List<String> userMentions;

        public ParseResult() {
            messageWithMentions = "";
            roleMentions = new ArrayList<>();
            userMentions = new ArrayList<>();
        }
    }
}
