package com.eugene.commands;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import com.eugene.LecternActions;
import com.eugene.LecternJob;
import com.eugene.TickDelayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerLecternCommand {
    private static final String COMMAND_PREFIX = "player";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, net.minecraft.command.CommandRegistryAccess registryAccess) {
        LiteralArgumentBuilder<ServerCommandSource> argumentBuilder = literal(COMMAND_PREFIX)
                .then(argument(COMMAND_PREFIX, StringArgumentType.word())
                                .then(literal("lectern")
                                        .then(literal("use")
                                            .executes(PlayerLecternCommand::lecternUse))
                                        .then(literal("page").
                                                then(argument("page", IntegerArgumentType.integer(1))
                                                        .executes(PlayerLecternCommand::lecternTurn)))
                                        .then(literal("load")
                                                .executes(PlayerLecternCommand::dataLoad))
                                        .then(literal("unload")
                                                .executes(PlayerLecternCommand::dataUnload))
                                        .then(literal("start")
                                                .executes(PlayerLecternCommand::lecternStart))
                                        .then(literal("stop").
                                                executes(PlayerLecternCommand::lecternStop))
                                )
                );
        dispatcher.register(argumentBuilder);
    }

    private static int dataUnload(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity target = getPlayerFromContext(context);
        if(target == null) return 0;
        if(!LecternActions.unloadData(target, context.getSource())){
            return 0;
        }
        return 1;
    }

    private static int dataLoad(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity target = getPlayerFromContext(context);
        if(target == null) return 0;
        LecternBlockEntity lectern = LecternActions.findLecternWithBook(target, context.getSource());
        if(lectern == null) return 0;

        if(LecternActions.loadDataFromLectern(target, lectern, context.getSource())){
            return 0;
        }
        return 1;
    }

    private static int lecternStart(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity target = getPlayerFromContext(context);
        if(target == null) return 0;
        if(!LecternActions.startLecternJob(target, context.getSource())) return 0;
        return 1;
    }
    private static int lecternStop(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity target = getPlayerFromContext(context);
        if(target == null) return 0;
        if(!LecternActions.stopLecternJob(target, context.getSource())) return 0;
        return 1;
    }

    private static int lecternUse(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity target = getPlayerFromContext(context);
        if(target == null) return 0;
        LecternBlockEntity lectern = LecternActions.findLecternWithBook(target, context.getSource());
        if(lectern == null) return 0;
        if(LecternActions.use(target, lectern, context.getSource())){
            return 0;
        }
        return 1;
    }

    private static int lecternTurn(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity target = getPlayerFromContext(context);
        int page1Based = IntegerArgumentType.getInteger(context, "page");
        if (target == null) return 0;
        if(!LecternActions.turnToPage(target, page1Based, context.getSource())){
            return 0;
        }
        return 1;
    }

    private static ServerPlayerEntity getPlayerFromContext(CommandContext<ServerCommandSource> context){
        String targetName = StringArgumentType.getString(context, COMMAND_PREFIX);
        ServerPlayerEntity target = CarpetServer.minecraft_server.getPlayerManager().getPlayer(targetName);
        if (target == null){
            Messenger.m(context.getSource(), "r No such Player found.");
        }
        return target;
    }
}
