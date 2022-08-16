package com.ishland.vanillamelody.common.playback;

import com.google.common.collect.Sets;
import com.ishland.vanillamelody.common.playback.data.MidiInstruments;
import com.ishland.vanillamelody.common.playback.data.Note;

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
import java.util.UUID;

class MinecraftMidiSynthesizer implements Receiver {

    private final SongPlayer songPlayer;
    private final MidiInstruments.MidiInstrument[] channelPrograms = new MidiInstruments.MidiInstrument[16];
    private final int[] channelProgramsNum = new int[16];
    private final short[] channelPitchBends = new short[16];
    private final byte[][] channelPolyPressures = new byte[16][128];
    private final byte[] channelPressures = new byte[16];
    @SuppressWarnings("unchecked")
    private final Set<SimpleNote>[] runningNotes = new Set[16];
    @SuppressWarnings("unchecked")
    private final Set<SimpleNote>[] pendingOffNotes = new Set[16];

    private final boolean[] holdPedal = new boolean[16];

    {
        reset();
    }

    public MinecraftMidiSynthesizer(SongPlayer songPlayer) {
        this.songPlayer = songPlayer;
    }

    private void reset() {
        Arrays.fill(channelPrograms, MidiInstruments.instrumentMapping.get(0));
        Arrays.fill(channelProgramsNum, 0);
        Arrays.fill(channelPitchBends, (short) 0);
        for (byte[] bytes : channelPolyPressures) {
            Arrays.fill(bytes, (byte) 127);
        }
        Arrays.fill(channelPressures, (byte) 127);
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
                        reset();
                        break;
                    case ShortMessage.CONTROL_CHANGE:
                        controlChange(shortMessage);
                        break;
                }

            } else //noinspection StatementWithEmptyBody
                if (midiMessage instanceof SysexMessage) {

                } else System.err.println("Invalid message: " + midiMessage);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void controlChange(ShortMessage shortMessage) {
        switch (shortMessage.getData1()) {
            case 64: // Hold Pedal
                holdPedal[shortMessage.getChannel()] = shortMessage.getData2() >= 64;
                handleHoldPedal(shortMessage);
                break;
            case 123: // All Notes Off
            case 124:
            case 125:
            case 126:
            case 127:
                if (holdPedal[shortMessage.getChannel()]) {
                    for (SimpleNote note : runningNotes[shortMessage.getChannel()])
                        pendingOffNotes[shortMessage.getChannel()].add(note);
                } else {
                    runningNotes[shortMessage.getChannel()].clear();
                }
                break;
            case 121:
                holdPedal[shortMessage.getChannel()] = false;
                handleHoldPedal(shortMessage);
                break;
        }
    }

    private void handleHoldPedal(ShortMessage shortMessage) {
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
        channelPrograms[shortMessage.getChannel()] = MidiInstruments.instrumentMapping.get(shortMessage.getData1());
        channelProgramsNum[shortMessage.getChannel()] = shortMessage.getData1();
        Arrays.fill(channelPolyPressures[shortMessage.getChannel()], (byte) 127);
        runningNotes[shortMessage.getChannel()].clear();
    }

    public void noteOn(ShortMessage shortMessage) {
        final Note note;
        if (shortMessage.getChannel() != 9) {
            final MidiInstruments.MidiInstrument channelProgram = channelPrograms[shortMessage.getChannel()];
            if (channelProgram == null) return;
            final short key = (short) (shortMessage.getData1() + (channelProgram.octaveModifier * 12));
            note = new Note(
                    (byte) channelProgram.mcInstrument,
                    key,
                    (float) ((shortMessage.getData2() / 127.0) * (channelPolyPressures[shortMessage.getChannel()][shortMessage.getData1()] / 127.0) * (channelPressures[shortMessage.getChannel()] / 127.0)) * keyVelocityModifier(key),
                    100,
                    (short) (channelPitchBends[shortMessage.getChannel()] / 4096.0 * 100));
//            System.out.println(channelProgramsNum[shortMessage.getChannel()]);
//            System.out.println(note);
            if (channelProgram.isLongSound) {
                runningNotes[shortMessage.getChannel()].remove(new SimpleNote(shortMessage.getData1(), shortMessage.getData2()));
                runningNotes[shortMessage.getChannel()].add(new SimpleNote(shortMessage.getData1(), shortMessage.getData2()));
            }
        } else {
            final MidiInstruments.MidiPercussion percussion = MidiInstruments.percussionMapping.get(shortMessage.getData1());
            if (percussion == null) return;
            note = new Note(
                    (byte) percussion.mcInstrument,
                    (short) percussion.midiKey,
                    (float) ((shortMessage.getData2() / 127.0) * (channelPolyPressures[shortMessage.getChannel()][shortMessage.getData1()] / 127.0) * (channelPressures[shortMessage.getChannel()] / 127.0)),
                    100,
                    (short) 0);
        }
        playNote(note);
    }

    private void playNote(Note note) {
        songPlayer.playNote(note);
    }

    private float keyVelocityModifier(short key) {
        return 0.6f + key * 0.01f;
    }

    @Override
    public void close() {

    }

    public void tick() {
        for (int channel = 0, runningNotesLength = runningNotes.length; channel < runningNotesLength; channel++) {
            Set<SimpleNote> channelRunningNotes = runningNotes[channel];
            for (SimpleNote note : channelRunningNotes) {
                final MidiInstruments.MidiInstrument channelProgram = channelPrograms[channel];
                if (channelProgram == null) return;
                final short key = (short) (note.note + (channelProgram.octaveModifier * 12));
                playNote(
                        new Note(
                                (byte) channelProgram.mcInstrument,
                                key,
                                (float) (((note.velocity / 127.0) * (channelPolyPressures[channel][note.note] / 127.0) * (channelPressures[channel] / 127.0)) * 0.1 * keyVelocityModifier(key)),
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
