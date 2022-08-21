package com.ishland.vanillamelody.common.playback;

import com.ishland.vanillamelody.common.playback.data.Note;
import com.ishland.vanillamelody.common.playback.synth.MinecraftMidiSynthesizer;
import com.ishland.vanillamelody.common.playback.synth.NoteReceiver;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ServerSongPlayer implements NoteReceiver {

    private static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(1);

    public static final ServerSongPlayer INSTANCE = new ServerSongPlayer();

    private static volatile PlayList playList = null;

    public static Stream<String> playlistSuggestion() {
        return playList.getSongs().stream().map(PlayList.SongInfo::pathWithoutInvalidChars);
    }

    public static void reload() {
        playList = PlayList.scan(FabricLoader.getInstance().getConfigDir()
                .resolve("vanillamelody").resolve("songs").toFile());
        System.out.println("Found %d midi songs".formatted(playList.getSongs().size()));
    }

    static {
        reload();
    }

    private final CopyOnWriteArraySet<ServerPlayerEntity> players = new CopyOnWriteArraySet<>();

    private final MinecraftMidiSynthesizer synthesizer = new MinecraftMidiSynthesizer(this);

    private final AtomicInteger index = new AtomicInteger(0);
    private volatile int playlistHash = System.identityHashCode(playList);
    private volatile PlayList.SongInfo playing = null;

    private final Sequencer sequencer;

    {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.getTransmitter().setReceiver(synthesizer);
            sequencer.addMetaEventListener(this::onMetaMessage);
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

    public void nextSong() {
        this.sequencer.setTickPosition(this.sequencer.getTickLength() - 1);
        this.sequencer.stop();
    }

    public void setSong(String path) {
        ReferenceArrayList<PlayList.SongInfo> songs = playList.getSongs();
        for (int i = 0, songsSize = songs.size(); i < songsSize; i++) {
            PlayList.SongInfo song = songs.get(i);
            if (song.pathWithoutInvalidChars().equals(path)) {
                this.index.set(i);
                nextSong();
                return;
            }
        }
    }

    public void tick() {
        synthesizer.tick();

        if (System.identityHashCode(playList) != playlistHash) {
            playlistHash = System.identityHashCode(playList);
            index.set(0);
        }

        if (!sequencer.isRunning()) {
            if (playList.getSongs().isEmpty()) return;

            this.sequencer.stop();
            this.synthesizer.reset(true);
            try {
                final PlayList.SongInfo songInfo = playList.getSongs().get(index.getAndIncrement() % playList.getSongs().size());
                this.playing = songInfo;
                sequencer.setSequence((Sequence) null);
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
                false
        );
    }

    @Override
    public void playNote(Note note) {
        final Identifier sound = new Identifier(note.instrument());
        for (ServerPlayerEntity player : players) {
            final Vec3d pos = NoteUtil.stereoPan(player.getPos(), player.getYaw(), (float) (note.panning() / 16.0));
            float volume = note.volume();
            while (volume > 0.0f) {
                player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(
                        sound,
                        SoundCategory.RECORDS,
                        pos,
                        Math.min(volume, 0.9f),
                        note.pitch()
                ));
                volume -= 0.9f;
            }
        }
    }

    public void onMetaMessage(MetaMessage metaMessage) {
        switch (metaMessage.getType()) {
            case 0x01: // Text
                if (MinecraftMidiSynthesizer.DEBUG) {
                    System.out.println("Text: " + new String(metaMessage.getData()));
                }
                break;
            case 0x05: // Lyrics
                for (ServerPlayerEntity player : players) {
                    player.sendMessage(new LiteralText(new String(metaMessage.getData())), true);
                }
                break;
            case 0x06: // Marker
                if (MinecraftMidiSynthesizer.DEBUG) {
                    System.out.println("Marker: " + new String(metaMessage.getData()));
                }
                break;
        }
    }
}
