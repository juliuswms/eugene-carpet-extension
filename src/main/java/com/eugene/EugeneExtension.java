package com.eugene;

import carpet.CarpetExtension;
import carpet.CarpetServer;

import com.eugene.commands.PlayerLecternCommand;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import static com.mojang.text2speech.Narrator.LOGGER;

public final class EugeneExtension implements CarpetExtension, ModInitializer {

    public static void noop() { }

    @Override
    public void onInitialize() {
    }

    @Override
    public void onGameStarted() {
        LOGGER.info("Eugene's-Carpet-Extensions have been loaded");
    }

    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, final CommandRegistryAccess commandBuildContext){
    }
}