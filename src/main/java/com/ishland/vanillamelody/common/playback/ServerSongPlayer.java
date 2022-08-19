package com.ishland.vanillamelody.common.playback;

import com.ishland.vanillamelody.common.playback.data.Note;
import com.ishland.vanillamelody.common.playback.synth.MinecraftMidiSynthesizer;
import com.ishland.vanillamelody.common.playback.synth.NoteReceiver;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerSongPlayer implements NoteReceiver {

    private static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(1);

    public static final ServerSongPlayer INSTANCE = new ServerSongPlayer();

    private final CopyOnWriteArraySet<ServerPlayerEntity> players = new CopyOnWriteArraySet<>();

    private final MinecraftMidiSynthesizer synthesizer = new MinecraftMidiSynthesizer(this);

    private final PlayList playList = PlayList.scan(FabricLoader.getInstance().getConfigDir()
            .resolve("vanillamelody").resolve("songs").toFile());
    private final AtomicInteger index = new AtomicInteger(0);
    private volatile PlayList.SongInfo playing = null;

    private final Sequencer sequencer;

    {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.getTransmitter().setReceiver(synthesizer);
            sequencer.open();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }

        EXECUTOR.scheduleAtFixedRate(this::tick, 0, 20, TimeUnit.MILLISECONDS);
    }

    public void addPlayer(ServerPlayerEntity player) {
        players.add(player);
        sendSongChange(player);
    }

    public void removePlayer(ServerPlayerEntity player) {
        players.remove(player);
    }

    public void tick() {
        synthesizer.tick();
        if (!sequencer.isRunning()) {
            if (playList.getSongs().isEmpty()) return;

            this.sequencer.stop();
            this.synthesizer.reset(true);
            try {
                final PlayList.SongInfo songInfo = playList.getSongs().get(index.getAndIncrement() % playList.getSongs().size());
                this.playing = songInfo;
                sequencer.setSequence(songInfo.sequence());
                sequencer.start();
                for (ServerPlayerEntity player : this.players) {
                    sendSongChange(player);
                }
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendSongChange(ServerPlayerEntity player) {
        final PlayList.SongInfo info = this.playing;
        if (info == null) return;
        player.sendMessage(
                new LiteralText("Now playing: %s".formatted(info.fileFormat().properties().getOrDefault("title", info.relativeFilePath())))
                        .setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
                true
        );
    }

    @Override
    public void playNote(Note note) {
        final Identifier sound = new Identifier(note.instrument());
        for (ServerPlayerEntity player : players) {
            float volume = note.volume();
            while (volume > 0.0f) {
                player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(
                        sound,
                        SoundCategory.RECORDS,
                        player.getPos(),
                        Math.min(volume, 0.9f),
                        note.pitch()
                ));
                volume -= 0.9f;
            }
        }
    }
}
