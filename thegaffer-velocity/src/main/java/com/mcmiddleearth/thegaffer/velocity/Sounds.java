package com.mcmiddleearth.thegaffer.velocity;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class Sounds {
    public static final Sound ActiveJob = Sound.sound(
        Key.key("block.anvil.use"),
        Sound.Source.MASTER,
        0.5f,
        2.0f
    );
}
