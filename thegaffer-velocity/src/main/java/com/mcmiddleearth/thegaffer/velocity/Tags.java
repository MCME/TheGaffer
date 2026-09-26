package com.mcmiddleearth.thegaffer.velocity;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class Tags {
   public static final TagResolver player = Placeholder.styling("player-tag", TextColor.color(66, 196, 66));
}
