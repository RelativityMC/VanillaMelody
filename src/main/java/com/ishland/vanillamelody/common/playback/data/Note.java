package com.ishland.vanillamelody.common.playback.data;

import com.ishland.vanillamelody.common.playback.NoteUtil;

public record Note(String instrument, float volume, int panning, float pitch) {

    /**
     * Used for vanilla instrument input
     *
     * @param instrument vanilla instrument ordinal
     * @param key midi key
     * @param velocity key velocity scaled to 0 - 1
     * @param panning panning
     * @param pitch pitch modifier scaled to 0 - 100
     */
    public Note(byte instrument, short key, float velocity, int panning, short pitch) {
        this(
                NoteUtil.warpNameOutOfRange(instrument, key, pitch),
                velocity,
                panning,
                NoteUtil.getPitchInOctave(key, pitch)
        );
    }

}
