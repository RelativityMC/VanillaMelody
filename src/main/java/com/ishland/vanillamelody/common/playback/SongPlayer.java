package com.ishland.vanillamelody.common.playback;

import com.ishland.vanillamelody.common.playback.data.Note;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.io.File;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SongPlayer {

    private static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(1);

    public static final SongPlayer INSTANCE = new SongPlayer();

    private final CopyOnWriteArraySet<ServerPlayerEntity> players = new CopyOnWriteArraySet<>();

    private final MinecraftMidiSynthesizer synthesizer = new MinecraftMidiSynthesizer(this);

    private final Sequencer sequencer;

    {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.getTransmitter().setReceiver(synthesizer);
            sequencer.open();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }

        EXECUTOR.scheduleAtFixedRate(synthesizer::tick, 0, 20, TimeUnit.MILLISECONDS);
    }

    public void addPlayer(ServerPlayerEntity player) {
        players.add(player);
    }

    public void removePlayer(ServerPlayerEntity player) {
        players.remove(player);
    }

    public void tick() {
    }

    public void playNote(Note note) {
        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(
                    new Identifier(note.instrument()),
                    SoundCategory.RECORDS,
                    player.getPos(),
                    note.volume(),
                    note.pitch()
            ));
        }

    }

    public void playDefault() {
        try {
            final Sequence sequence = MidiSystem.getSequence(new File("default.mid"));
            this.sequencer.stop();
            this.sequencer.setSequence(sequence);
            this.sequencer.start();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
