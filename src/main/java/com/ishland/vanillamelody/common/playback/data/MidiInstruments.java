package com.ishland.vanillamelody.common.playback.data;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Map;

/**
 * Midi instrument to Minecraft instrument mapping
 * <a href="https://github.com/HielkeMinecraft/OpenNoteBlockStudio/blob/master/scripts/midi_instruments/midi_instruments.gml">Used mapping from OpenNoteBlockStudio</a>
 * <p>
 * This file is licensed under MIT
 * <p>
 * Copyright (c) 2019 Hielke
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class MidiInstruments {

    public static final Int2ObjectOpenHashMap<MidiInstrument> instrumentMapping = new Int2ObjectOpenHashMap<>(128);
    public static final Int2ObjectOpenHashMap<MidiPercussion> percussionMapping = new Int2ObjectOpenHashMap<>(64);

    static {
        /*
           0 = Harp
           1 = Double Bass
           2 = Bass Drum
           3 = Snare Drum
           4 = Click
           5 = Guitar
           6 = Flute
           7 = Bell
           8 = Chime
           9 = Xylophone
           10 = Iron Xylophone
           11 = Cow Bell
           12 = Didgeridoo
           13 = Bit
           14 = Banjo
           15 = Pling
         */

        // ------------------ Instrument  ------------------

        // Piano
        instrumentMapping.put(0, new MidiInstrument(0, -1, false));
        instrumentMapping.put(1, new MidiInstrument(0, 0, false));
        instrumentMapping.put(2, new MidiInstrument(13, 0, false));
        instrumentMapping.put(3, new MidiInstrument(0, 0, false));
        instrumentMapping.put(4, new MidiInstrument(13, 0, false));
        instrumentMapping.put(5, new MidiInstrument(13, 0, false));
        instrumentMapping.put(6, new MidiInstrument(0, 1, false));
        instrumentMapping.put(7, new MidiInstrument(0, 0, false));
        // Chromatic Percussion
        instrumentMapping.put(8, new MidiInstrument(11, -1, false));
        instrumentMapping.put(9, new MidiInstrument(11, -1, false));
        instrumentMapping.put(10, new MidiInstrument(11, -1, false));
        instrumentMapping.put(11, new MidiInstrument(11, -1, false));
        instrumentMapping.put(12, new MidiInstrument(11, 0, false));
        instrumentMapping.put(13, new MidiInstrument(9, 0, false));
        instrumentMapping.put(14, new MidiInstrument(7, -1, false));
        instrumentMapping.put(15, new MidiInstrument(7, 0, false));
        // Organ
        instrumentMapping.put(16, new MidiInstrument(1, 1, true));
        instrumentMapping.put(17, new MidiInstrument(1, 1, true));
        instrumentMapping.put(18, new MidiInstrument(0, 0, true));
        instrumentMapping.put(19, new MidiInstrument(0, 0, true));
        instrumentMapping.put(20, new MidiInstrument(0, 0, true));
        instrumentMapping.put(21, new MidiInstrument(0, 0, true));
        instrumentMapping.put(22, new MidiInstrument(0, 0, true));
        instrumentMapping.put(23, new MidiInstrument(0, 0, true));
        // Guitar
        instrumentMapping.put(24, new MidiInstrument(5, 0, false));
        instrumentMapping.put(25, new MidiInstrument(5, 0, false));
        instrumentMapping.put(26, new MidiInstrument(5, 1, false));
        instrumentMapping.put(27, new MidiInstrument(5, 0, false));
        // imap.put(28, new MidiInstrument(-1, 0));
        instrumentMapping.put(29, new MidiInstrument(5, -1, false));
        instrumentMapping.put(30, new MidiInstrument(5, -1, false));
        instrumentMapping.put(31, new MidiInstrument(5, 0, false));
        // Bass
        instrumentMapping.put(32, new MidiInstrument(1, 0, false));
        instrumentMapping.put(33, new MidiInstrument(1, 1, false));
        instrumentMapping.put(34, new MidiInstrument(1, 1, false));
        instrumentMapping.put(35, new MidiInstrument(1, 1, false));
        instrumentMapping.put(36, new MidiInstrument(1, 1, false));
        instrumentMapping.put(37, new MidiInstrument(1, 1, false));
        instrumentMapping.put(38, new MidiInstrument(1, 1, false));
        instrumentMapping.put(39, new MidiInstrument(1, 1, false));
        // Strings
        instrumentMapping.put(40, new MidiInstrument(6, -2, true));
        instrumentMapping.put(41, new MidiInstrument(6, -2, true));
        instrumentMapping.put(42, new MidiInstrument(6, -2, true));
        instrumentMapping.put(43, new MidiInstrument(6, -2, true));
        instrumentMapping.put(44, new MidiInstrument(0, 0, true));
        instrumentMapping.put(45, new MidiInstrument(0, 0, false));
        instrumentMapping.put(46, new MidiInstrument(8, 0, false));
        instrumentMapping.put(47, new MidiInstrument(3, 1, false));
        // Ensemble
        instrumentMapping.put(48, new MidiInstrument(6, -2, true));
        instrumentMapping.put(49, new MidiInstrument(6, -2, true));
        instrumentMapping.put(50, new MidiInstrument(6, -2, true));
        instrumentMapping.put(51, new MidiInstrument(6, -2, true));
        instrumentMapping.put(52, new MidiInstrument(6, -2, true));
        instrumentMapping.put(53, new MidiInstrument(6, -4, true));
        instrumentMapping.put(54, new MidiInstrument(6, -4, true));
        instrumentMapping.put(55, new MidiInstrument(6, 0, false));
        // Brass
        instrumentMapping.put(56, new MidiInstrument(6, -2, true));
        instrumentMapping.put(57, new MidiInstrument(6, -2, true));
        instrumentMapping.put(58, new MidiInstrument(6, -2, true));
        instrumentMapping.put(59, new MidiInstrument(6, -2, true));
        instrumentMapping.put(60, new MidiInstrument(6, -2, true));
        instrumentMapping.put(61, new MidiInstrument(6, -2, true));
        instrumentMapping.put(62, new MidiInstrument(6, -2, true));
        instrumentMapping.put(63, new MidiInstrument(1, 1, true));
        // Reed
        instrumentMapping.put(64, new MidiInstrument(6, -2, true));
        instrumentMapping.put(65, new MidiInstrument(6, -2, true));
        instrumentMapping.put(66, new MidiInstrument(6, -2, true));
        instrumentMapping.put(67, new MidiInstrument(6, -2, true));
        instrumentMapping.put(68, new MidiInstrument(6, -2, true));
        instrumentMapping.put(69, new MidiInstrument(6, -2, true));
        instrumentMapping.put(70, new MidiInstrument(6, -3, true));
        instrumentMapping.put(71, new MidiInstrument(6, -2, true));
        // Pipe
        instrumentMapping.put(72, new MidiInstrument(6, -3, true));
        instrumentMapping.put(73, new MidiInstrument(6, -3, true));
        instrumentMapping.put(74, new MidiInstrument(6, -3, true));
        instrumentMapping.put(75, new MidiInstrument(6, -3, true));
        instrumentMapping.put(76, new MidiInstrument(6, -3, true));
        instrumentMapping.put(77, new MidiInstrument(6, -3, true));
        instrumentMapping.put(78, new MidiInstrument(6, -3, true));
        instrumentMapping.put(79, new MidiInstrument(6, -3, true));
        // Synth Lead
        instrumentMapping.put(80, new MidiInstrument(6, -2, true));
        instrumentMapping.put(81, new MidiInstrument(6, -2, true));
        instrumentMapping.put(82, new MidiInstrument(6, -2, true));
        instrumentMapping.put(83, new MidiInstrument(6, -2, true));
        instrumentMapping.put(84, new MidiInstrument(6, -2, true));
        instrumentMapping.put(85, new MidiInstrument(6, -2, true));
        instrumentMapping.put(86, new MidiInstrument(6, -2, true));
        instrumentMapping.put(87, new MidiInstrument(6, -1, true));
        // Synth Pad
        instrumentMapping.put(88, new MidiInstrument(0, 0, true));
        instrumentMapping.put(89, new MidiInstrument(0, 0, true));
        instrumentMapping.put(90, new MidiInstrument(0, 0, true));
        instrumentMapping.put(91, new MidiInstrument(0, 0, true));
        instrumentMapping.put(92, new MidiInstrument(0, 0, true));
        instrumentMapping.put(93, new MidiInstrument(0, 0, true));
        instrumentMapping.put(94, new MidiInstrument(0, 0, true));
        instrumentMapping.put(95, new MidiInstrument(0, 0, true));
        // Synth Effects
        // imap.put(96, new MidiInstrument(0, 0));
        // imap.put(97, new MidiInstrument(0, 0));
        instrumentMapping.put(98, new MidiInstrument(13, 0, false));
        instrumentMapping.put(99, new MidiInstrument(0, 0, true));
        instrumentMapping.put(100, new MidiInstrument(0, 0, false));
        // imap.put(101, new MidiInstrument(0, 0));
        // imap.put(102, new MidiInstrument(0, 0));
        // imap.put(103, new MidiInstrument(0, 0));
        // Ethnic
        instrumentMapping.put(104, new MidiInstrument(14, 0, false));
        instrumentMapping.put(105, new MidiInstrument(14, 0, false));
        instrumentMapping.put(106, new MidiInstrument(14, 0, false));
        instrumentMapping.put(107, new MidiInstrument(14, 0, false));
        instrumentMapping.put(108, new MidiInstrument(1, 1, false));
        instrumentMapping.put(109, new MidiInstrument(0, 0, true));
        instrumentMapping.put(110, new MidiInstrument(0, 0, true));
        instrumentMapping.put(111, new MidiInstrument(0, 0, true));
        // Percussive
        instrumentMapping.put(112, new MidiInstrument(7, 0, false));
        instrumentMapping.put(113, new MidiInstrument(0, 0, false));
        instrumentMapping.put(114, new MidiInstrument(10, 0, false));
        instrumentMapping.put(115, new MidiInstrument(4, 0, false));
        instrumentMapping.put(116, new MidiInstrument(3, 0, false));
        instrumentMapping.put(117, new MidiInstrument(3, 0, false));
        instrumentMapping.put(118, new MidiInstrument(3, 0, false));
        // Sound Effects
        // imap.put(119, new MidiInstrument(0, 0));
        // imap.put(120, new MidiInstrument(0, 0));
        // imap.put(121, new MidiInstrument(0, 0));
        // imap.put(122, new MidiInstrument(0, 0));
        // imap.put(123, new MidiInstrument(0, 0));
        // imap.put(124, new MidiInstrument(0, 0));
        // imap.put(125, new MidiInstrument(0, 0));
        // imap.put(126, new MidiInstrument(0, 0));
        instrumentMapping.put(127, new MidiInstrument(0, 0, false));


        // ------------------ Percussion  ------------------

        // pmap.put(24, new MidiPercussion(0, 0));
        // pmap.put(25, new MidiPercussion(0, 0));
        // pmap.put(26, new MidiPercussion(0, 0));
        // pmap.put(27, new MidiPercussion(0, 0));
        // pmap.put(28, new MidiPercussion(0, 0));
        // pmap.put(29, new MidiPercussion(0, 0));
        // pmap.put(31, new MidiPercussion(0, 0));
        // pmap.put(32, new MidiPercussion(0, 0));
        // pmap.put(33, new MidiPercussion(0, 0));
        // pmap.put(34, new MidiPercussion(0, 0));

        ((Map<Integer, MidiPercussion>) percussionMapping).put(35, new MidiPercussion(2, 10));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(36, new MidiPercussion(2, 6));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(37, new MidiPercussion(4, 6));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(38, new MidiPercussion(3, 8));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(39, new MidiPercussion(4, 6));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(40, new MidiPercussion(3, 4));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(41, new MidiPercussion(2, 6));

        ((Map<Integer, MidiPercussion>) percussionMapping).put(42, new MidiPercussion(3, 22));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(43, new MidiPercussion(2, 13));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(44, new MidiPercussion(3, 22));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(45, new MidiPercussion(2, 15));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(46, new MidiPercussion(3, 18));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(47, new MidiPercussion(2, 20));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(48, new MidiPercussion(2, 23));

        ((Map<Integer, MidiPercussion>) percussionMapping).put(49, new MidiPercussion(3, 17));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(50, new MidiPercussion(2, 23));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(51, new MidiPercussion(3, 24));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(52, new MidiPercussion(3, 8));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(53, new MidiPercussion(3, 13));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(54, new MidiPercussion(4, 18));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(55, new MidiPercussion(3, 18));

        ((Map<Integer, MidiPercussion>) percussionMapping).put(56, new MidiPercussion(4, 1));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(57, new MidiPercussion(3, 13));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(58, new MidiPercussion(4, 2));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(59, new MidiPercussion(3, 13));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(60, new MidiPercussion(4, 9));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(61, new MidiPercussion(4, 2));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(62, new MidiPercussion(4, 8));

        ((Map<Integer, MidiPercussion>) percussionMapping).put(63, new MidiPercussion(2, 22));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(64, new MidiPercussion(2, 15));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(65, new MidiPercussion(3, 13));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(66, new MidiPercussion(3, 8));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(67, new MidiPercussion(4, 8));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(68, new MidiPercussion(4, 3));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(69, new MidiPercussion(4, 20));

        ((Map<Integer, MidiPercussion>) percussionMapping).put(70, new MidiPercussion(4, 23));
        // pmap.put(71, new MidiPercussion(0, 0));
        // pmap.put(72, new MidiPercussion(0, 0));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(73, new MidiPercussion(4, 17));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(74, new MidiPercussion(4, 11));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(75, new MidiPercussion(4, 18));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(76, new MidiPercussion(4, 9));

        ((Map<Integer, MidiPercussion>) percussionMapping).put(77, new MidiPercussion(4, 5));
        // pmap.put(78, new MidiPercussion(0, 0));
        // pmap.put(79, new MidiPercussion(0, 0));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(80, new MidiPercussion(4, 17));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(81, new MidiPercussion(4, 22));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(82, new MidiPercussion(3, 22));
        // pmap.put(83, new MidiPercussion(0, 0));

        // pmap.put(84, new MidiPercussion(0, 0));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(85, new MidiPercussion(4, 21));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(86, new MidiPercussion(2, 14));
        ((Map<Integer, MidiPercussion>) percussionMapping).put(87, new MidiPercussion(2, 7));
    }

    public static class MidiInstrument {

        public final int mcInstrument;
        public final int octaveModifier;
        public final boolean isLongSound; // TODO better name

        public MidiInstrument(int mcInstrument, int octaveModifier, boolean isLongSound) {
            Preconditions.checkArgument(mcInstrument >= 0);
            Preconditions.checkArgument(mcInstrument <= 15);
            this.isLongSound = isLongSound;
            this.mcInstrument = mcInstrument;
            this.octaveModifier = octaveModifier;
        }

        @Override
        public String toString() {
            return "MidiInstrument{" +
                    "mcInstrument=" + mcInstrument +
                    ", octaveModifier=" + octaveModifier +
                    ", isLongSound=" + isLongSound +
                    '}';
        }
    }

    public static class MidiPercussion {

        public final int mcInstrument;
        public final int midiKey;

        public MidiPercussion(int mcInstrument, int mcKey) {
            Preconditions.checkArgument(mcInstrument >= 0);
            Preconditions.checkArgument(mcInstrument <= 15);
            this.mcInstrument = mcInstrument;
            this.midiKey = mcKey + 33;
        }
    }
}
