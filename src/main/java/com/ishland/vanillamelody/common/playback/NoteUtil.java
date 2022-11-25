package com.ishland.vanillamelody.common.playback;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class NoteUtil {

    private static final float[] pitches = new float[2401];

    static {
        for (int i = 0; i < 2401; i++){
            pitches[i] = (float) Math.pow(2, (i - 1200d) / 1200d);
        }
    }

    /**
     * Add suffix to vanilla instrument to use sound outside 2 octave range
     * @param instrument instrument id
     * @param key sound key
     * @param pitch
     * @return warped name
     */
    public static String warpNameOutOfRange(byte instrument, short key, short pitch) {
        return warpNameOutOfRange(getSoundNameByInstrument(instrument), key, pitch);
    }

    /**
     * Add suffix to qualified name to use sound outside 2 octave range
     *
     * @param name qualified name
     * @param key sound key
     * @param pitch
     * @return warped name
     */
    public static String warpNameOutOfRange(String name, short key, short pitch) {
        key = applyPitchToKey(key, pitch);
        // -15 base_-2
        // 9 base_-1
        // 33 base
        // 57 base_1
        // 81 base_2
        // 105 base_3
        int suffix = 0;
        while (key < 33) {
            suffix -= 1;
            key += 24;
        }
        while (key > 56) {
            suffix += 1;
            key -= 24;
        }
        return name + (suffix != 0 ? "_" + suffix : "");
    }

    /**
     * Returns the name of vanilla instrument
     *
     * @param instrument instrument identifier
     * @return Sound name with full qualified name
     */
    public static String getSoundNameByInstrument(byte instrument) {
        //noinspection RedundantSuppression
        switch (instrument) {
            case 0:
                //noinspection DuplicateBranchesInSwitch
                return "minecraft:block.note_block.harp";
            case 1:
                return "minecraft:block.note_block.bass";
            case 2:
                //noinspection SpellCheckingInspection
                return "minecraft:block.note_block.basedrum";
            case 3:
                return "minecraft:block.note_block.snare";
            case 4:
                return "minecraft:block.note_block.hat";
            case 5:
                return "minecraft:block.note_block.guitar";
            case 6:
                return "minecraft:block.note_block.flute";
            case 7:
                return "minecraft:block.note_block.bell";
            case 8:
                return "minecraft:block.note_block.chime";
            case 9:
                return "minecraft:block.note_block.xylophone";
            case 10:
                return "minecraft:block.note_block.iron_xylophone";
            case 11:
                return "minecraft:block.note_block.cow_bell";
            case 12:
                return "minecraft:block.note_block.didgeridoo";
            case 13:
                return "minecraft:block.note_block.bit";
            case 14:
                return "minecraft:block.note_block.banjo";
            case 15:
                //noinspection SpellCheckingInspection
                return "minecraft:block.note_block.pling";
            default:
                return "minecraft:block.note_block.harp";
        }
    }

    /**
     * Get pitch in specific octave range
     *
     * @param key   sound key
     * @param pitch extra pitch
     * @return pitch
     */
    public static float getPitchInOctave(short key, short pitch) {
        // Apply pitch to key
        key = applyPitchToKey(key, pitch);
        pitch %= 100;
        if(pitch < 0) pitch = (short) (100 + pitch);

        // -15 base_-2
        // 9 base_-1
        // 33 base
        // 57 base_1
        // 81 base_2
        // 105 base_3
        while (key < 33) key += 24;
        while (key > 56) key -= 24;

        key -= 33;

        return pitches[key * 100 + pitch];
    }

    public static float getPitchOnBaseOctave(short key, short pitch) {
        // Apply pitch to key
        key = applyPitchToKey(key, pitch);
        pitch %= 100;
        if(pitch < 0) pitch = (short) (100 + pitch);

        // -15 base_-2
        // 9 base_-1
        // 33 base
        // 57 base_1
        // 81 base_2
        // 105 base_3
        float level = 1;
        while (key < 33) {
            key += 24;
            level *= 0.25f;
        }
        while (key > 56) {
            key -= 24;
            level *= 4.0f;
        }

        key -= 33;

        return pitches[key * 100 + pitch] * level;
    }

    public static short applyPitchToKey(short key, short pitch) {
        if(pitch == 0) return key;
        if(pitch < 0) return (short) (key - (-pitch / 100) - (Math.abs(pitch) % 100 != 0 ? 1 : 0));
        return (short) (key + (pitch / 100));
    }

    public static Vec3d stereoPan(Vec3d location, float yaw, float offset) {
        return location.add(MathHelper.cos(yaw * (float) (Math.PI / 180.0)) * offset, 0, MathHelper.sin(yaw * (float) (Math.PI / 180.0)) * offset);
    }

}
