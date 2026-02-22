package com.eugene;

import carpet.utils.Messenger;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

public final class LecternActions {
    private record LecternSession(RegistryKey<World> worldKey, BlockPos pos) {}
    private static final Map<UUID, LecternActions.LecternSession> sessions = new ConcurrentHashMap<>();
    private static final Map<UUID, LecternInstructions> instructionsSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, LecternJob> jobSessions = new ConcurrentHashMap<>();

    public static boolean startLecternJob(ServerPlayerEntity target, ServerCommandSource source){
        if(jobSessions.get(target.getUuid()) != null){
            Messenger.m(source, "r Already performing a job.");
            return false;
        }
        LecternInstructions instructions = instructionsSessions.get(target.getUuid());
        if(instructions == null){
            Messenger.m(source, "r No instructions found. Use load first.");
            return false;
        }
        LecternBlockEntity lectern = getLecternFromSession(target, source);
        if(lectern == null) return false;

        LecternJob job = new LecternJob(target, source, lectern, instructions);
        jobSessions.put(target.getUuid(), job);
        job.start();
        return true;
    }

    public static boolean stopLecternJob(ServerPlayerEntity target, ServerCommandSource source){
        LecternJob job = jobSessions.get(target.getUuid());
        if(job == null){
            Messenger.m(source, "r Not performing a job.");
            return false;
        }
        job.stop();
        jobSessions.remove(target.getUuid());
        return true;
    }

    public static boolean turnToPage(ServerPlayerEntity target, int page1Based, ServerCommandSource source){
        LecternBlockEntity lectern = getLecternFromSession(target, source);
        if (lectern == null) return false;

        ItemStack book = lectern.getBook();
        int maxPages = getBookPageCount(book);
        if (maxPages <= 0) {
            Messenger.m(source, "r Book has no readable pages.");
            return false;
        }
        if (page1Based > maxPages) {
            Messenger.m(source, "r Page out of range. Max pages: " + maxPages);
            return false;
        }

        int page0 = page1Based - 1;
        int buttonId = LecternScreenHandler.BASE_JUMP_TO_PAGE_BUTTON_ID + page0;
        boolean ok = target.currentScreenHandler.onButtonClick(target, buttonId);
        if(!ok){
            Messenger.m(source, "r Lectern GUI is not open.");
            return false;
        }

        Messenger.m(source, "w Changed page: " + page1Based);
        return true;
    }

    private static LecternBlockEntity getLecternFromSession(ServerPlayerEntity target, ServerCommandSource source) {
        LecternSession sess = sessions.get(target.getUuid());
        if (sess == null) {
            Messenger.m(source, "r No lectern selected. Run: /player <name> lectern use");
            return null;
        }
        if (target.getWorld().getRegistryKey() != sess.worldKey()) {
            Messenger.m(source, "r Target player is in a different dimension than the selected lectern.");
            return null;
        }

        BlockEntity blockEntity = target.getWorld().getBlockEntity(sess.pos());
        if (!(blockEntity instanceof LecternBlockEntity lectern)) {
            Messenger.m(source, "r Selected lectern is missing or not a lectern anymore.");
            return null;
        }
        return lectern;
    }

    public static boolean use(ServerPlayerEntity target, LecternBlockEntity lectern, ServerCommandSource source){
        sessions.put(target.getUuid(), new LecternSession(target.getWorld().getRegistryKey(), lectern.getPos()));
        OptionalInt sync = target.openHandledScreen(lectern);
        if (sync.isEmpty()) {
            Messenger.m(source, "r Failed to open lectern screen.");
            return false;
        }
        Messenger.m(source, "w Opened Lectern.");
        return true;
    }

    public static boolean loadDataFromLectern(ServerPlayerEntity target, LecternBlockEntity lectern, ServerCommandSource source){
        LecternInstructions instructions = instructionsSessions.get(target.getUuid());
        if(instructions != null && !instructions.isEmpty()) {
            Messenger.m(source, "r Instructions not empty. Use unload first.");
            return false;
        }
        ArrayList<Integer> rawInstructions = new ArrayList<>();
        ItemStack book = lectern.getBook();
        int pageCount = getBookPageCount(book);
        if(pageCount < 1) {
            Messenger.m(source, "r Book does not contain minimum amount of pages.");
            return false;
        }
        String firstPage = getBookPageContent(book, 0);
        Map<String, String> keyMap = parsePage(firstPage);
        String jobName = keyMap.get("name");
        double estimatedTotalTime = parseDouble(keyMap.get("est"));
        int[] delays = parseIntArray(keyMap.get("delays"));
        int pauseDelay = parseInt(keyMap.get("pdelay"));

        for (int i = 1; i < pageCount; i++) {
            String page = getBookPageContent(book, i);
            for (int j = 0; j < page.length(); j++) {
                switch (page.charAt(j)) {
                    case '0' -> rawInstructions.add(1);
                    case '1' -> rawInstructions.add(2);
                    case '2' -> rawInstructions.add(3);
                    case '3' -> rawInstructions.add(4);
                    case '4' -> rawInstructions.add(5);
                    case '5' -> rawInstructions.add(6);
                    case '6' -> rawInstructions.add(7);
                    case '7' -> rawInstructions.add(8);
                    case '8' -> rawInstructions.add(9);
                    case '9' -> rawInstructions.add(10);
                    case 'A' -> rawInstructions.add(11);
                    case 'B' -> rawInstructions.add(12);
                    case 'C' -> rawInstructions.add(13);
                    case 'D' -> rawInstructions.add(14);
                    case 'E' -> rawInstructions.add(15);
                    default -> { Messenger.m(source, "r Book contains illegal char."); return false; }
                }
            }
        }
        instructions = new LecternInstructions(jobName, estimatedTotalTime, rawInstructions, delays, pauseDelay);
        instructionsSessions.put(target.getUuid(), instructions);
        Messenger.m(source,"w Loaded Job: " + jobName);
        Messenger.m(source,"w Estimated Total Time: " + estimatedTotalTime + "h");
        Messenger.m(source,"w " +  rawInstructions.size() + " instructions have been loaded.");
        return true;
    }

    public static boolean unloadData(ServerPlayerEntity target, ServerCommandSource source){
        instructionsSessions.remove(target.getUuid());
        Messenger.m(source, "w Removed instructions");
        return true;
    }

    public static LecternBlockEntity findLecternWithBook(ServerPlayerEntity target, ServerCommandSource source){
        LecternBlockEntity lectern = raycastLectern(target, 5.0);
        if (lectern == null) {
            Messenger.m(source, "r No lectern in reach.");
            return null;
        }
        if (!lectern.hasBook()) {
            Messenger.m(source, "r Lectern has no book.");
            return null;
        }
        return lectern;
    }

    private static LecternBlockEntity raycastLectern(ServerPlayerEntity player, double reach) {
        HitResult hit = player.raycast(reach, 0.0f, false);
        if (!(hit instanceof BlockHitResult bhr)) return null;

        BlockPos pos = bhr.getBlockPos();
        var be = player.getWorld().getBlockEntity(pos);
        return (be instanceof LecternBlockEntity lectern) ? lectern : null;
    }

    private static int getBookPageCount(ItemStack book) {
        WritableBookContentComponent writable = book.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
        if (writable != null) return writable.pages().size();

        return 0;
    }

    private static String getBookPageContent(ItemStack book, Integer pageIndex){
        WritableBookContentComponent writable = book.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
        if (writable == null) {
            return "";
        }
        if(writable.pages().size() < pageIndex) {
            return "";
        }
        return writable.pages().get(pageIndex).get(false);
    }

    private static Map<String, String> parsePage(String pageText){
        String[] lines = pageText.split("\\R");
        Map<String, String> keyMap = new HashMap<>();

        for(String raw : lines){
            String line = raw.trim();
            if(line.isEmpty()) continue;

            int qualSign = line.indexOf("=");
            if (qualSign <= 0) continue;

            String key = line.substring(0, qualSign).trim();
            String val = line.substring(qualSign + 1).trim();
            keyMap.put(key, val);
        }
        return keyMap;
    }

    private static int[] parseIntArray(String val){
        String[] vals = val.split(",");
        int[] out = new int[vals.length];
        for (int i = 0; i < vals.length; i++){
            out[i] = parseInt(vals[i].trim());
        }
        return out;
    }
}
