package com.ishland.vanillamelody.client.playback;

import com.ishland.vanillamelody.common.Constants;
import com.ishland.vanillamelody.common.playback.PacketConstants;
import com.ishland.vanillamelody.common.playback.NoteUtil;
import com.ishland.vanillamelody.common.playback.PlayList;
import com.ishland.vanillamelody.common.playback.data.MidiInstruments;
import com.ishland.vanillamelody.common.playback.data.Note;
import com.ishland.vanillamelody.common.playback.synth.MinecraftMidiSynthesizer;
import com.ishland.vanillamelody.common.playback.synth.NoteReceiver;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.util.function.BooleanSupplier;

public class ClientSongPlayer implements NoteReceiver {

    private final MinecraftMidiSynthesizer synthesizer = new MinecraftMidiSynthesizer(this);

    private volatile PlayList.SongInfo playing = null;
    private volatile Sequencer sequencer;

    private byte[] pendingHash = null;

    {
        try {
            reopenSequencer();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private Sequencer reopenSequencer() throws MidiUnavailableException {
        synchronized (this) {
            Sequencer sequencer = this.sequencer;
            if (sequencer != null) sequencer.close();
            sequencer = this.sequencer = MidiSystem.getSequencer(false);
            sequencer.getTransmitter().setReceiver(synthesizer);
            sequencer.open();
            return sequencer;
        }
    }

    public void init(Int2ObjectOpenHashMap<MidiInstruments.MidiInstrument> instruments, Int2ObjectOpenHashMap<MidiInstruments.MidiPercussion> percussions) {
        this.synthesizer.setInstrumentBank(instruments);
        this.synthesizer.setPercussionBank(percussions);
    }

    public void sequenceChange(byte[] sha256, long tickPosition, long microsecondsPosition) {
        final PlayList.SongInfo songInfo = ClientSyncedPlaybackManager.get(sha256, true);
        if (songInfo == null) {
            pendingHash = sha256;
            return;
        }
        final PlayList.SongInfo currentlyPlaying = playing;
        Sequencer sequencer = this.sequencer;
        if (sequencer == null) return;
        if (currentlyPlaying != null && currentlyPlaying == songInfo) {
            if (Math.abs(sequencer.getMicrosecondPosition() - microsecondsPosition) < 10_000_000) return;
            sequencer.setTickPosition(tickPosition);
            return;
        }

        try {
            sequencer.stop();
            sequencer.setSequence((Sequence) null);
            this.synthesizer.reset(true);
            this.playing = songInfo;
            sequencer = reopenSequencer();
            this.synthesizer.reset(true);
            sequencer.setSequence(songInfo.sequence());
            sequencer.setTickPosition(tickPosition);
            sequencer.start();
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            e.printStackTrace();
        }

    }

    public void tick(int syncId) {
        synthesizer.tick();
        if (pendingHash != null) {
            final PlayList.SongInfo songInfo = ClientSyncedPlaybackManager.get(pendingHash, false);
            if (songInfo != null) {
                final PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeInt(syncId);
                ClientPlayNetworking.send(PacketConstants.CLIENT_PLAYBACK_SEQUENCE_REQUEST, buf);
                pendingHash = null;
            }
        }
    }

    public void close() {
        final Sequencer sequencer = this.sequencer;
        if (sequencer != null) {
            sequencer.stop();
            sequencer.close();
        }
    }

    @Override
    public void playNote(Note note, BooleanSupplier isDone) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        if (note.rawPitch() < 0.05f) return;
        final SynthSoundInstance sound = SynthSoundInstance.create(note, isDone, player.getYaw());
        if (Constants.isRSLSInstalled) {
            MinecraftClient.getInstance().getSoundManager().play(sound);
        } else {
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().getSoundManager().play(sound));
        }
    }
}
