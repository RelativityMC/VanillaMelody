package com.ishland.vanillamelody.common.playback.synth;

import com.google.common.collect.Sets;
import com.ishland.vanillamelody.common.playback.data.MidiInstruments;
import com.ishland.vanillamelody.common.playback.data.Note;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class MinecraftMidiSynthesizer implements Receiver {

    public static final boolean DEBUG = false;

    private static int restrict7Bit(int value) {
        if (value < 0) return 0;
        return Math.min(value, 127);
    }

    private final NoteReceiver noteReceiver;
    private final int[] channelProgramsNum = new int[16];
    private volatile Int2ObjectOpenHashMap<MidiInstruments.MidiInstrument> instrumentBank = MidiInstruments.instrumentMapping;
    private volatile Int2ObjectOpenHashMap<MidiInstruments.MidiPercussion> percussionBank = MidiInstruments.percussionMapping;

    private final short[] channelPitchBends = new short[16];
    private final byte[][] channelPolyPressures = new byte[16][128];
    private final byte[] channelPressures = new byte[16];
    private final byte[] channelVolumes = new byte[16];
    private final byte[] channelExpression = new byte[16];
    @SuppressWarnings("unchecked")
    private final Set<SimpleNote>[] runningNotes = new Set[16];
    @SuppressWarnings("unchecked")
    private final Set<SimpleNote>[] pendingOffNotes = new Set[16];

    private final boolean[] holdPedal = new boolean[16];

    private int generalMidiMode = 0;
    private boolean isCh10Percussion = false;


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
        Arrays.fill(channelProgramsNum, 0);
        Arrays.fill(channelPitchBends, (short) 0);
        for (byte[] bytes : channelPolyPressures) {
            Arrays.fill(bytes, (byte) 127);
        }
        Arrays.fill(channelPressures, (byte) 127);
        Arrays.fill(channelExpression, (byte) 127);
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
//                        reset();
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
        final byte[] data = sysexMessage.getData();
        if ((data[1] & 0xFF) == 0x7E) { // Non-Realtime
            int deviceID = data[2] & 0xFF;
            if (deviceID == 0x7F || deviceID == 0x00) {
                int subid1 = data[3] & 0xFF;
                switch (subid1) {
                    case 0x09:  // General Midi Message
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
                    default:
                        if (DEBUG) {
                            System.out.println("Unhandled non-realtime sysex message: " + Arrays.toString(data));
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
        switch (shortMessage.getData1()) {
            case 1: // Bank Select
                break; // TODO
            case 2: // Modulation Wheel
                break; // TODO
            case 7: // channel volume
                channelVolumes[shortMessage.getChannel()] = (byte) restrict7Bit(shortMessage.getData2());
                break;
            case 11: // Expression
                channelExpression[shortMessage.getChannel()] = (byte) restrict7Bit(shortMessage.getData2());
                break;
            case 64: // Hold Pedal
                holdPedal[shortMessage.getChannel()] = shortMessage.getData2() >= 64;
                handleHoldPedal(shortMessage);
                break;
            case 123: // All Notes Off
            case 124:
            case 125:
            case 126:
            case 127:
                allNotesOff(shortMessage);
                break;
            case 121:
                holdPedal[shortMessage.getChannel()] = false;
                handleHoldPedal(shortMessage);
                break;
            default:
                if (DEBUG) {
                    System.out.println("[%2d] Unhandled control change: control %d data %d".formatted(shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2()));
                }
                break;
        }
    }

    private void allNotesOff(ShortMessage shortMessage) {
        if (holdPedal[shortMessage.getChannel()]) {
            for (SimpleNote note : runningNotes[shortMessage.getChannel()])
                pendingOffNotes[shortMessage.getChannel()].add(note);
        } else {
            runningNotes[shortMessage.getChannel()].clear();
        }
    }

    private void handleHoldPedal(ShortMessage shortMessage) {
        if (DEBUG) {
            System.out.println("[%2d] Hold Pedal: %s".formatted(shortMessage.getChannel(), holdPedal[shortMessage.getChannel()]));
        }
        if (!holdPedal[shortMessage.getChannel()]) {
            for (Iterator<SimpleNote> iterator = pendingOffNotes[shortMessage.getChannel()].iterator(); iterator.hasNext(); ) {
                SimpleNote note = iterator.next();
                runningNotes[shortMessage.getChannel()].remove(note);
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
//        if (shortMessage.getChannel() != 12 && shortMessage.getChannel() != 9) return;
        final Note note;
        if (shortMessage.getChannel() == 9 || (isCh10Percussion && shortMessage.getChannel() == 10)) {
            final MidiInstruments.MidiPercussion percussion = percussionBank.get(shortMessage.getData1());
            if (percussion == null) return;
            note = new Note(
                    (byte) percussion.mcInstrument,
                    (short) percussion.midiKey,
                    getNoteVolume(shortMessage.getData2(), shortMessage.getChannel(), shortMessage.getData1()),
                    100,
                    (short) 0);
        } else {
            final MidiInstruments.MidiInstrument channelProgram = instrumentBank.get(channelProgramsNum[shortMessage.getChannel()]);
            if (channelProgram == null) return;
            final short key = (short) (shortMessage.getData1() + (channelProgram.octaveModifier * 12));
            note = new Note(
                    (byte) channelProgram.mcInstrument,
                    key,
                    getNoteVolume(shortMessage.getData2(), shortMessage.getChannel(), shortMessage.getData1()),
                    100,
                    (short) (channelPitchBends[shortMessage.getChannel()] / 4096.0 * 100));
//            System.out.println(channelProgramsNum[shortMessage.getChannel()]);
//            System.out.println(note);
            if (channelProgram.isLongSound) {
                runningNotes[shortMessage.getChannel()].remove(new SimpleNote(shortMessage.getData1(), shortMessage.getData2()));
                runningNotes[shortMessage.getChannel()].add(new SimpleNote(shortMessage.getData1(), shortMessage.getData2()));
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
        for (int channel = 0, runningNotesLength = runningNotes.length; channel < runningNotesLength; channel++) {
            Set<SimpleNote> channelRunningNotes = runningNotes[channel];
            for (SimpleNote note : channelRunningNotes) {
                final MidiInstruments.MidiInstrument channelProgram = instrumentBank.get(channelProgramsNum[channel]);
                if (channelProgram == null) return;
                final short key = (short) (note.note + (channelProgram.octaveModifier * 12));
                playNote(
                        new Note(
                                (byte) channelProgram.mcInstrument,
                                key,
                                (float) (getNoteVolume(note.velocity, channel, note.note) * 0.1),
                                100,
                                (short) (channelPitchBends[channel] / 4096.0 * 100))
                );
            }

        }

    }

    private class SimpleNote {

        public final int note;
        public final int velocity;

        private SimpleNote(int note, int velocity) {
            this.note = note;
            this.velocity = velocity;
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
            return Objects.hash(note);
        }

        @Override
        public String toString() {
            return "SimpleNote{" +
                    "note=" + note +
                    ", velocity=" + velocity +
                    '}';
        }
    }
}
