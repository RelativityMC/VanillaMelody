package com.ishland.vanillamelody.common.playback.synth;

import com.google.common.collect.Sets;
import com.ishland.vanillamelody.common.playback.data.MidiInstruments;
import com.ishland.vanillamelody.common.playback.data.Note;
import com.ishland.vanillamelody.common.playback.data.SoftTuning;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceFunction;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class MinecraftMidiSynthesizer implements Receiver {

    public static final boolean DEBUG = false;

    private static int restrict7Bit(int value) {
        if (value < 0) return 0;
        return Math.min(value, 127);
    }

    public static final Int2ReferenceFunction<SoftTuning> NEW_TUNING_BANK = unused -> new SoftTuning();

    private static int getPatchIndex(int bank, int program) {
        return ((bank & 0b1111_1111_1111_1111) << 8) | (program & 0b1111_1111);
    }

    private final NoteReceiver noteReceiver;
    private volatile Int2ObjectOpenHashMap<MidiInstruments.MidiInstrument> instrumentBank = MidiInstruments.instrumentMapping;
    private volatile Int2ObjectOpenHashMap<MidiInstruments.MidiPercussion> percussionBank = MidiInstruments.percussionMapping;

    private final Int2ReferenceOpenHashMap<SoftTuning> tuningBank = new Int2ReferenceOpenHashMap<>();

    private static final int RPN_NULL_VALUE = (127 << 7) + 127;
    private final int[] rpn_control_recv = new int[16];
    private final int[] nrpn_control_recv = new int[16];
    private final Int2ObjectOpenHashMap<int[]> co_midi_nrpn_nrpn_i = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<double[]> co_midi_nrpn_nrpn = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<int[]> co_midi_rpn_rpn_i = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<double[]> co_midi_rpn_rpn = new Int2ObjectOpenHashMap<>();
    private final int[] tuning_program_recv = new int[16];
    private final int[] tuning_bank_recv = new int[16];

    private final int[] channelBanks = new int[16];
    private final SoftTuning[] channelTunings = new SoftTuning[16];
    private final int[] channelProgramsNum = new int[16];
    private final short[] channelPitchBends = new short[16];
    private final byte[][] channelPolyPressures = new byte[16][128];
    private final byte[] channelPressures = new byte[16];
    private final byte[] channelVolumes = new byte[16];
    private final byte[] channelExpression = new byte[16];
    private final byte[] channelPan = new byte[16];
    @SuppressWarnings("unchecked")
    private final Set<SimpleNote>[] runningNotes = new Set[16];
    @SuppressWarnings("unchecked")
    private final Set<SimpleNote>[] pendingOffNotes = new Set[16];

    private final boolean[] holdPedal = new boolean[16];

    private int generalMidiMode = 0;
    private boolean isCh10Percussion = false;

    private long tickCount = 0L;

    {
        reset(false);
    }

    public MinecraftMidiSynthesizer(NoteReceiver noteReceiver) {
        this.noteReceiver = noteReceiver;
    }

    public void reset(boolean full) {
        if (full) {
            this.generalMidiMode = 0;
            this.isCh10Percussion = false;
        }

        if (DEBUG) {
            System.out.println("Resetting MIDI synthesizer: GM mode %d".formatted(this.generalMidiMode));
        }

        if (generalMidiMode == 2) {
            this.isCh10Percussion = true;
        } else {
            this.isCh10Percussion = false;
        }

//        Arrays.fill(channelPrograms, MidiInstruments.instrumentMapping.get(0));
        tuningBank.clear();
        Arrays.fill(nrpn_control_recv, RPN_NULL_VALUE);
        Arrays.fill(rpn_control_recv, RPN_NULL_VALUE);
        co_midi_nrpn_nrpn.clear();
        co_midi_nrpn_nrpn_i.clear();
        co_midi_rpn_rpn.clear();
        co_midi_rpn_rpn_i.clear();
        Arrays.fill(tuning_bank_recv, 0);
        Arrays.fill(tuning_program_recv, 0);
        Arrays.fill(channelBanks, 0);
        Arrays.fill(channelTunings, new SoftTuning());
        Arrays.fill(channelProgramsNum, 0);
        Arrays.fill(channelPitchBends, (short) 0);
        for (byte[] bytes : channelPolyPressures) {
            Arrays.fill(bytes, (byte) 127);
        }
        Arrays.fill(channelPressures, (byte) 127);
        Arrays.fill(channelExpression, (byte) 127);
        Arrays.fill(channelPan, (byte) 64);
        Arrays.fill(runningNotes, Sets.newConcurrentHashSet());
        Arrays.fill(pendingOffNotes, Sets.newConcurrentHashSet());
        resetControllers();
    }

    private void resetControllers() {
        Arrays.fill(holdPedal, false);
    }

    @Override
    public void send(MidiMessage midiMessage, long l) {
        try {
            if (midiMessage instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) midiMessage.clone();
                if (shortMessage.getCommand() == ShortMessage.NOTE_ON && shortMessage.getData2() == 0) {
                    try {
                        shortMessage.setMessage(ShortMessage.NOTE_OFF, shortMessage.getData1(), 64);
                    } catch (InvalidMidiDataException e) {
                        e.printStackTrace();
                    }
                }
                switch (shortMessage.getCommand()) {
                    case ShortMessage.NOTE_ON:
                        noteOn(shortMessage);
                        break;
                    case ShortMessage.NOTE_OFF:
                        noteOff(shortMessage);
                        break;
                    case ShortMessage.PROGRAM_CHANGE:
                        programChange(shortMessage);
                        break;
                    case ShortMessage.PITCH_BEND:
                        pitchBend(shortMessage);
                        break;
                    case ShortMessage.POLY_PRESSURE:
                        polyPressure(shortMessage);
                        break;
                    case ShortMessage.CHANNEL_PRESSURE:
                        channelPressure(shortMessage);
                        break;
                    case ShortMessage.SYSTEM_RESET:
                        reset(false);
                        break;
                    case ShortMessage.CONTROL_CHANGE:
                        controlChange(shortMessage);
                        break;
                }

            } else //noinspection StatementWithEmptyBody
                if (midiMessage instanceof SysexMessage sysexMessage) {
                    sysexMessage(sysexMessage);
                } else System.err.println("Invalid message: " + midiMessage);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void sysexMessage(SysexMessage sysexMessage) {
        final byte[] originalData = sysexMessage.getData();
        final byte[] data = new byte[originalData.length + 1];
        data[0] = (byte) sysexMessage.getStatus();
        System.arraycopy(originalData, 0, data, 1, originalData.length);

        if ((data[1] & 0xFF) == 0x7E) { // Non-Realtime
            int deviceID = data[2] & 0xFF;
            if (deviceID == 0x7F || deviceID == 0x00) {
                int subid1 = data[3] & 0xFF;
                switch (subid1) {
                    case 0x08:  // MIDI Tuning Standard
                    {
                        int subid2 = data[4] & 0xFF;
                        switch (subid2) {
                            case 0x01:  // BULK TUNING DUMP
                            {
                                // http://www.midi.org/about-midi/tuning.shtml
                                SoftTuning tuning = tuningBank.computeIfAbsent(getPatchIndex(0, data[5] & 0xFF), NEW_TUNING_BANK);
                                tuning.load(data);
                                break;
                            }
                            case 0x04:  // KEY-BASED TUNING DUMP
                            case 0x05:  // SCALE/OCTAVE TUNING DUMP, 1 byte format
                            case 0x06:  // SCALE/OCTAVE TUNING DUMP, 2 byte format
                            case 0x07:  // SINGLE NOTE TUNING CHANGE (NON-REAL-TIME)
                                // (BANK)
                            {
                                // http://www.midi.org/about-midi/tuning_extens.shtml
                                SoftTuning tuning = tuningBank.computeIfAbsent(getPatchIndex(data[5] & 0xFF, data[6] & 0xFF), NEW_TUNING_BANK);
                                tuning.load(data);
                                break;
                            }
                            case 0x08:  // scale/octave tuning 1-byte form (Non
                                // Real-Time)
                            case 0x09:  // scale/octave tuning 2-byte form (Non
                                // Real-Time)
                            {
                                // http://www.midi.org/about-midi/tuning-scale.shtml
                                SoftTuning tuning = new SoftTuning(data);
                                int channelmask = (data[5] & 0xFF) * 16384
                                        + (data[6] & 0xFF) * 128 + (data[7] & 0xFF);
                                for (int i = 0; i < channelTunings.length; i++)
                                    if ((channelmask & (1 << i)) != 0)
                                        channelTunings[i] = tuning;
                                break;
                            }
                            default:
                                break;
                        }
                        break;
                    }
                    case 0x09:  // General Midi Message
                    {
                        int subid2 = data[4] & 0xFF;
                        switch (subid2) {
                            case 0x01:  // General Midi 1 On
                                generalMidiMode = 1;
                                reset(false);
                                break;
                            case 0x02:  // General Midi Off
                                generalMidiMode = 0;
                                reset(false);
                                break;
                            case 0x03:  // General MidI Level 2 On
                                generalMidiMode = 2;
                                reset(false);
                                break;
                            default:
                                break;
                        }
                    }
                    default:
                        if (DEBUG) {
                            System.out.println("Unhandled non-realtime sysex message: " + Arrays.toString(data));
                        }
                        break;
                }
                return;
            }
        }

        if ((data[1] & 0xFF) == 0x7F) { // Real-time
            int deviceID = data[2] & 0xFF;
            if (deviceID == 0x7F || deviceID == 0x00) {
                int subid1 = data[3] & 0xFF;
                switch (subid1) {
                    case 0x08:  // MIDI Tuning Standard
                    {
                        int subid2 = data[4] & 0xFF;
                        switch (subid2) {
                            case 0x02:  // SINGLE NOTE TUNING CHANGE (REAL-TIME)
                            {
                                // http://www.midi.org/about-midi/tuning.shtml
                                SoftTuning tuning = tuningBank.computeIfAbsent(getPatchIndex(0, data[5] & 0xFF), NEW_TUNING_BANK);
                                tuning.load(data);
                                for (Set<SimpleNote> simpleNotes : runningNotes) {
                                    for (SimpleNote simpleNote : simpleNotes) {
                                        if (simpleNote.tuning == tuning) {
                                            simpleNote.pitchOffset = (float) ((tuning.getTuning(simpleNote.note) / 100.0) - simpleNote.note);
                                        }
                                    }
                                }

                                break;
                            }
                            case 0x07:  // SINGLE NOTE TUNING CHANGE (REAL-TIME)
                                // (BANK)
                            {
                                // http://www.midi.org/about-midi/tuning_extens.shtml
                                SoftTuning tuning = tuningBank.computeIfAbsent(getPatchIndex(data[5] & 0xFF, data[6] & 0xFF), NEW_TUNING_BANK);
                                tuning.load(data);
                                for (Set<SimpleNote> simpleNotes : runningNotes) {
                                    for (SimpleNote simpleNote : simpleNotes) {
                                        if (simpleNote.tuning == tuning) {
                                            simpleNote.pitchOffset = (float) ((tuning.getTuning(simpleNote.note) / 100.0) - simpleNote.note);
                                        }
                                    }
                                }
                                break;
                            }
                            case 0x08:  // scale/octave tuning 1-byte form
                                //(Real-Time)
                            case 0x09:  // scale/octave tuning 2-byte form
                                // (Real-Time)
                            {
                                // http://www.midi.org/about-midi/tuning-scale.shtml
                                SoftTuning tuning = new SoftTuning(data);
                                int channelmask = (data[5] & 0xFF) * 16384
                                        + (data[6] & 0xFF) * 128 + (data[7] & 0xFF);
                                for (int i = 0; i < channelTunings.length; i++)
                                    if ((channelmask & (1 << i)) != 0)
                                        channelTunings[i] = tuning;
                                for (Set<SimpleNote> simpleNotes : runningNotes) {
                                    for (SimpleNote simpleNote : simpleNotes) {
                                        simpleNote.pitchOffset = (float) ((tuning.getTuning(simpleNote.note) / 100.0) - simpleNote.note);
                                    }
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        break;
                    }
                    default:
                        if (DEBUG) {
                            System.out.println("Unhandled realtime sysex message: " + Arrays.toString(data));
                        }
                        break;
                }
            }
        }

        if (DEBUG) {
            System.out.println("Unhandled sysex message: " + Arrays.toString(data));
        }
    }

    private void controlChange(ShortMessage shortMessage) {
        final int controller = shortMessage.getData1();
        final int value = shortMessage.getData2();
        final int channel = shortMessage.getChannel();
        controlChange(channel, controller, value);
    }

    private void controlChange(int channel, int controller, int value) {
        switch (controller) {
            case 1: // Bank Select
                channelBanks[channel] = value;
                break;
            case 2: // Modulation Wheel
                break; // TODO
            case 7: // channel volume
                channelVolumes[channel] = (byte) restrict7Bit(value);
                break;
            case 10: // Pan
                channelPan[channel] = (byte) restrict7Bit(value);
                break;
            case 11: // Expression
                channelExpression[channel] = (byte) restrict7Bit(value);
                break;
            case 6:
            case 38:
            case 96:
            case 97:
                int val = 0;
                if (nrpn_control_recv[channel] != RPN_NULL_VALUE) {
                    int[] val_i = co_midi_nrpn_nrpn_i.get(nrpn_control_recv[channel]);
                    if (val_i != null)
                        val = val_i[0];
                }
                if (rpn_control_recv[channel] != RPN_NULL_VALUE) {
                    int[] val_i = co_midi_rpn_rpn_i.get(rpn_control_recv[channel]);
                    if (val_i != null)
                        val = val_i[0];
                }

                if (controller == 6)
                    val = (val & 127) + (value << 7);
                else if (controller == 38)
                    val = (val & (127 << 7)) + value;
                else if (controller == 96 || controller == 97) {
                    int step = 1;
                    if (rpn_control_recv[channel] == 2 || rpn_control_recv[channel] == 3 || rpn_control_recv[channel] == 4)
                        step = 128;
                    if (controller == 96)
                        val += step;
                    if (controller == 97)
                        val -= step;
                }

                if (nrpn_control_recv[channel] != RPN_NULL_VALUE)
                    nrpnChange(channel, nrpn_control_recv[channel], val);
                if (rpn_control_recv[channel] != RPN_NULL_VALUE)
                    rpnChange(channel, rpn_control_recv[channel], val);

                break;
            case 64: // Hold Pedal
                holdPedal[channel] = value >= 64;
                handleHoldPedal(channel);
                break;
            case 98:
                nrpn_control_recv[channel] = (nrpn_control_recv[channel] & (127 << 7)) + value;
                rpn_control_recv[channel] = RPN_NULL_VALUE;
                break;
            case 99:
                nrpn_control_recv[channel] = (nrpn_control_recv[channel] & 127) + (value << 7);
                rpn_control_recv[channel] = RPN_NULL_VALUE;
                break;
            case 100:
                rpn_control_recv[channel] = (rpn_control_recv[channel] & (127 << 7)) + value;
                nrpn_control_recv[channel] = RPN_NULL_VALUE;
                break;
            case 101:
                rpn_control_recv[channel] = (rpn_control_recv[channel] & 127) + (value << 7);
                nrpn_control_recv[channel] = RPN_NULL_VALUE;
                break;
            case 123: // All Notes Off
            case 124:
            case 125:
            case 126:
            case 127:
                allNotesOff(channel);
                break;
            case 121:
                holdPedal[channel] = false;
                handleHoldPedal(channel);
                break;
            default:
                if (DEBUG) {
                    System.out.println("[%2d] Unhandled control change: control %d data %d".formatted(channel, controller, value));
                }
                break;
        }
    }

    private void nrpnChange(int channel, int controller, int value) {
        if (generalMidiMode == 0) {
            if (controller == (0x01 << 7) + (0x08)) // Vibrato Rate
                controlChange(channel, 76, value >> 7);
            if (controller == (0x01 << 7) + (0x09)) // Vibrato Depth
                controlChange(channel, 77, value >> 7);
            if (controller == (0x01 << 7) + (0x0A)) // Vibrato Delay
                controlChange(channel, 78, value >> 7);
            if (controller == (0x01 << 7) + (0x20)) // Brightness
                controlChange(channel, 74, value >> 7);
            if (controller == (0x01 << 7) + (0x21)) // Filter Resonance
                controlChange(channel, 71, value >> 7);
            if (controller == (0x01 << 7) + (0x63)) // Attack Time
                controlChange(channel, 73, value >> 7);
            if (controller == (0x01 << 7) + (0x64)) // Decay Time
                controlChange(channel, 75, value >> 7);
            if (controller == (0x01 << 7) + (0x66)) // Release Time
                controlChange(channel, 72, value >> 7);

            // TODO
//            if (controller >> 7 == 0x18) // Pitch coarse
//                controlChangePerNote(controller % 128, 120, value >> 7);
//            if (controller >> 7 == 0x1A) // Volume
//                controlChangePerNote(controller % 128, 7, value >> 7);
//            if (controller >> 7 == 0x1C) // Panpot
//                controlChangePerNote(controller % 128, 10, value >> 7);
//            if (controller >> 7 == 0x1D) // Reverb
//                controlChangePerNote(controller % 128, 91, value >> 7);
//            if (controller >> 7 == 0x1E) // Chorus
//                controlChangePerNote(controller % 128, 93, value >> 7);
        }

        int[] val_i = co_midi_nrpn_nrpn_i.get(controller);
        double[] val_d = co_midi_nrpn_nrpn.get(controller);
        if (val_i == null) {
            val_i = new int[1];
            co_midi_nrpn_nrpn_i.put(controller, val_i);
        }
        if (val_d == null) {
            val_d = new double[1];
            co_midi_nrpn_nrpn.put(controller, val_d);
        }

    }

    private void rpnChange(int channel, int controller, int value) {
        if (controller == 3) {
            tuning_program_recv[channel] = (value >> 7) & 127;
            tuningChange(channel, tuning_bank_recv[channel], tuning_program_recv[channel]);
        }
        if (controller == 4) {
            tuning_bank_recv[channel] = (value >> 7) & 127;
        }

        int[] val_i = co_midi_rpn_rpn_i.get(controller);
        double[] val_d = co_midi_rpn_rpn.get(controller);
        if (val_i == null) {
            val_i = new int[1];
            co_midi_rpn_rpn_i.put(controller, val_i);
        }
        if (val_d == null) {
            val_d = new double[1];
            co_midi_rpn_rpn.put(controller, val_d);
        }
        val_i[0] = value;
        val_d[0] = val_i[0] * (1.0 / 16384.0);
    }

    private void tuningChange(int channel, int bank, int program) {
        channelTunings[channel] = tuningBank.computeIfAbsent(getPatchIndex(bank, program), NEW_TUNING_BANK);
    }

    private void allNotesOff(int channel) {
//        if (holdPedal[channel]) {
//            for (SimpleNote note : runningNotes[channel])
//                pendingOffNotes[channel].add(note);
//        } else {
//        }
        runningNotes[channel].clear();
    }

    private void handleHoldPedal(int channel) {
        if (DEBUG) {
            System.out.println("[%2d] Hold Pedal: %s".formatted(channel, holdPedal[channel]));
        }
        if (!holdPedal[channel]) {
            for (Iterator<SimpleNote> iterator = pendingOffNotes[channel].iterator(); iterator.hasNext(); ) {
                SimpleNote note = iterator.next();
                runningNotes[channel].remove(note);
                iterator.remove();
            }
        }
    }

    private void noteOff(ShortMessage shortMessage) {
        final Optional<SimpleNote> found = runningNotes[shortMessage.getChannel()].stream().filter(simpleNote -> simpleNote.note == shortMessage.getData1()).findFirst();
        if (!found.isPresent()) return;
        if (holdPedal[shortMessage.getChannel()])
            pendingOffNotes[shortMessage.getChannel()].add(found.get());
        else
            runningNotes[shortMessage.getChannel()].remove(found.get());
    }

    public void channelPressure(ShortMessage shortMessage) {
        if (shortMessage.getData1() == 0) return;
        channelPressures[shortMessage.getChannel()] = (byte) shortMessage.getData1();
    }

    public void polyPressure(ShortMessage shortMessage) {
        channelPolyPressures[shortMessage.getChannel()][shortMessage.getData1()] = (byte) shortMessage.getData2();
    }

    public void pitchBend(ShortMessage shortMessage) {
        channelPitchBends[shortMessage.getChannel()] = (short) ((shortMessage.getData1() + shortMessage.getData2() * 128) - 8192);
    }

    public void programChange(ShortMessage shortMessage) {
        if (DEBUG) {
            System.out.println("[%2d] Program change: %d".formatted(shortMessage.getChannel(), shortMessage.getData1()));
        }
//        channelPrograms[shortMessage.getChannel()] = MidiInstruments.instrumentMapping.get(shortMessage.getData1());
        channelProgramsNum[shortMessage.getChannel()] = shortMessage.getData1();
        Arrays.fill(channelPolyPressures[shortMessage.getChannel()], (byte) 127);
        runningNotes[shortMessage.getChannel()].clear();
    }

    public void noteOn(ShortMessage shortMessage) {
//        if (shortMessage.getChannel() != 0 && shortMessage.getChannel() != 9) return;
        final Note note;
        if (shortMessage.getChannel() == 9 || (isCh10Percussion && shortMessage.getChannel() == 10)) {
            final MidiInstruments.MidiPercussion percussion = percussionBank.get(shortMessage.getData1());
            if (percussion == null) return;
            note = new Note(
                    (byte) percussion.mcInstrument,
                    (short) percussion.midiKey,
                    getNoteVolume(shortMessage.getData2(), shortMessage.getChannel(), shortMessage.getData1()),
                    channelPan[shortMessage.getChannel()] - 64,
                    (short) 0);
        } else {
            final MidiInstruments.MidiInstrument channelProgram = instrumentBank.get(channelProgramsNum[shortMessage.getChannel()]);
            if (channelProgram == null) return;
            final short key = (short) (shortMessage.getData1() + (channelProgram.octaveModifier * 12));
            final SoftTuning tuning = channelTunings[shortMessage.getChannel()];
            final SimpleNote simpleNote = new SimpleNote(shortMessage.getData1(), shortMessage.getData2(), tuning);
            note = new Note(
                    (byte) channelProgram.mcInstrument,
                    key,
                    getNoteVolume(shortMessage.getData2(), shortMessage.getChannel(), shortMessage.getData1()),
                    channelPan[shortMessage.getChannel()] - 64,
                    (short) ((channelPitchBends[shortMessage.getChannel()] / 4096.0 + simpleNote.pitchOffset) * 100));
//            System.out.println(channelProgramsNum[shortMessage.getChannel()]);
//            System.out.println(note);
            if (channelProgram.isLongSound) {
                runningNotes[shortMessage.getChannel()].remove(simpleNote);
                runningNotes[shortMessage.getChannel()].add(simpleNote);
            }
        }
        playNote(note);
    }

    private float getNoteVolume(int noteVelocity, int channel, int noteKey) {
        return (float) (
                (noteVelocity / 127.0) *
                        (channelPolyPressures[channel][noteKey] / 127.0) *
                        (channelPressures[channel] / 127.0) *
                        (channelVolumes[channel] / 127.0) *
                        (channelExpression[channel] / 127.0)
        );
    }

    private void playNote(Note note) {
        noteReceiver.playNote(note);
    }

    private float keyVelocityModifier(short key) {
        return 1;
    }

    @Override
    public void close() {

    }

    public void tick() {
        final long currentTick = tickCount++;
        for (int channel = 0, runningNotesLength = runningNotes.length; channel < runningNotesLength; channel++) {
            Set<SimpleNote> channelRunningNotes = runningNotes[channel];
            for (SimpleNote note : channelRunningNotes) {
                final MidiInstruments.MidiInstrument channelProgram = instrumentBank.get(channelProgramsNum[channel]);
                if (channelProgram == null) return;
                final short key = (short) (note.note + (channelProgram.octaveModifier * 12));
                final Note resultNote = new Note(
                        (byte) channelProgram.mcInstrument,
                        key,
                        (float) (getNoteVolume(note.velocity, channel, note.note) * 0.08),
                        channelPan[channel] - 64,
                        (short) ((channelPitchBends[channel] / 4096.0 + note.pitchOffset) * 100));
                if (currentTick % Math.max(1, Math.round(1 / resultNote.rawPitch())) == 0)
                    playNote(resultNote);
            }

        }

    }

    private class SimpleNote {

        public final int note;
        public final int velocity;
        public final SoftTuning tuning;
        public float pitchOffset;

        private SimpleNote(int note, int velocity, SoftTuning tuning) {
            this.note = note;
            this.velocity = velocity;
            this.tuning = tuning;
            this.pitchOffset = (float) ((tuning.getTuning(note) / 100.0) - note);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleNote that = (SimpleNote) o;
            return note == that.note;
        }

        @Override
        public int hashCode() {
            return note;
        }

        @Override
        public String toString() {
            return "SimpleNote{" +
                    "note=" + note +
                    ", velocity=" + velocity +
                    ", pitchOffset=" + pitchOffset +
                    '}';
        }
    }
}
