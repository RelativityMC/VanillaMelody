package com.ishland.vanillamelody.common.playback;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ishland.vanillamelody.common.Config;
import com.ishland.vanillamelody.common.playback.data.MidiInstruments;
import com.ishland.vanillamelody.common.playback.data.Note;
import com.ishland.vanillamelody.common.playback.synth.MinecraftMidiSynthesizer;
import com.ishland.vanillamelody.common.playback.synth.NoteReceiver;
import com.ishland.vanillamelody.common.util.DigestUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

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
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public class ServerSongPlayer implements NoteReceiver {

    public static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(
            1,
            new ThreadFactoryBuilder().setNameFormat("VanillaMelody Server Scheduler").setDaemon(true).build()
    );

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    public static final ServerSongPlayer INSTANCE = new ServerSongPlayer();

    static volatile PlayList playList = null;

    public static Stream<String> playlistSuggestion() {
        final PlayList list = playList;
        return list != null ? list.getSongs().stream().map(PlayList.SongInfo::pathWithoutInvalidChars) : Stream.empty();
    }

    public static void reload() {
        playList = PlayList.scan(FabricLoader.getInstance().getConfigDir()
                .resolve("vanillamelody").resolve("songs").toFile());
        System.out.println("Found %d midi songs".formatted(playList.getSongs().size()));
    }

    static {
        reload();
    }

    private final int syncId = ID_COUNTER.getAndIncrement();
    private final CopyOnWriteArraySet<ServerPlayerEntity> players = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<ServerPlayerEntity> playersWithClientMod = new CopyOnWriteArraySet<>();

    private final MinecraftMidiSynthesizer synthesizer = new MinecraftMidiSynthesizer(this);

    private final AtomicInteger index = new AtomicInteger(0);
    private volatile int playlistHash = System.identityHashCode(playList);
    private volatile PlayList.SongInfo playing = null;

    private volatile Sequencer sequencer;

    {
        try {
            reopenSequencer();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }

        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                tick();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 0, 20, TimeUnit.MILLISECONDS);
    }

    private Sequencer reopenSequencer() throws MidiUnavailableException {
        synchronized (this) {
            Sequencer sequencer = this.sequencer;
            if (sequencer != null) sequencer.close();
            sequencer = this.sequencer = MidiSystem.getSequencer(false);
            sequencer.getTransmitter().setReceiver(synthesizer);
            sequencer.addMetaEventListener(this::onMetaMessage);
            sequencer.open();
            return sequencer;
        }
    }

    public void addPlayer(ServerPlayerEntity player) {
        if (ServerSyncedPlaybackManager.PLAYERS_WITH_CLIENT_INSTALLED.contains(player.getUuid())) {
            playersWithClientMod.add(player);
            sendInitialData(player);
            notifySongChange(player);
        }
        players.add(player);
        sendSongChange(player);
    }

    public boolean removePlayer(ServerPlayerEntity player) {
        if (playersWithClientMod.remove(player)) {
            notifySequenceStop(player);
        }
        return players.remove(player);
    }

    public void nextSong() {
        synchronized (this) {
            final Sequencer sequencer = this.sequencer;
            if (sequencer == null) return;
            sequencer.setTickPosition(sequencer.getTickLength() - 1);
            sequencer.stop();
        }
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

        Sequencer sequencer = this.sequencer;
        if (sequencer != null && !sequencer.isRunning()) {
            if (playList.getSongs().isEmpty()) return;

            synchronized (this) {
                sequencer.stop();
                this.synthesizer.reset(true);
                try {
                    final PlayList.SongInfo songInfo = playList.getSongs().get(index.getAndIncrement() % playList.getSongs().size());
                    this.playing = songInfo;
//                    sequencer.setSequence((Sequence) null);
                    sequencer = reopenSequencer();
                    this.synthesizer.reset(true);
                    sequencer.setSequence(songInfo.sequence());
                    notifySongChange();
                    sequencer.start();
                    for (ServerPlayerEntity player : this.players) {
                        sendSongChange(player);
                    }
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
                    e.printStackTrace();
                }
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

    private void notifySongChange() {
        final PacketByteBuf buf = createSequenceChangeBuf();
        if (buf == null) return;
        for (ServerPlayerEntity player : this.playersWithClientMod) {
            buf.retain();
            ServerPlayNetworking.send(player, PacketConstants.SERVER_PLAYBACK_SEQUENCE_CHANGE, buf);
        }
        buf.release();
    }

    void sendInitialData(ServerPlayerEntity player) {
        final PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(syncId);
        MidiInstruments.writeInstruments(synthesizer.getInstrumentBank(), buf);
        MidiInstruments.writePercussions(synthesizer.getPercussionBank(), buf);
        ServerPlayNetworking.send(player, PacketConstants.SERVER_PLAYBACK_INIT, buf);
    }

    void notifySongChange(ServerPlayerEntity player) {
        final PacketByteBuf buf = createSequenceChangeBuf();
        if (buf == null) return;
        ServerPlayNetworking.send(player, PacketConstants.SERVER_PLAYBACK_SEQUENCE_CHANGE, buf);
    }

    @Nullable
    private PacketByteBuf createSequenceChangeBuf() {
        final PlayList.SongInfo info = this.playing;
        final Sequencer sequencer = this.sequencer;
        if (info == null || sequencer == null) return null;
        final PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(4 + DigestUtils.SHA256_BYTES + 8));
        buf.writeInt(this.syncId);
        buf.writeBytes(info.sha256());
        buf.writeLong(sequencer.getTickPosition());
        buf.writeLong(sequencer.getMicrosecondPosition());
        return buf;
    }

    private void notifySequenceStop(ServerPlayerEntity player) {
        final PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(this.syncId);
        ServerPlayNetworking.send(player, PacketConstants.SERVER_PLAYBACK_STOP, buf);
    }

    @Override
    public void playNote(Note note, BooleanSupplier isDone) {
        final Identifier sound = new Identifier(note.instrument());
        for (ServerPlayerEntity player : players) {
            if (!Config.ENABLE_SERVERSIDE_PLAYBACK || playersWithClientMod.contains(player)) continue;

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
