package com.ishland.vanillamelody.client.playback;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ishland.vanillamelody.common.playback.PacketConstants;
import com.ishland.vanillamelody.common.playback.PlayList;
import com.ishland.vanillamelody.common.playback.data.MidiInstruments;
import com.ishland.vanillamelody.common.util.DigestUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceFunction;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import javax.sound.midi.InvalidMidiDataException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ClientSyncedPlaybackManager {

    private static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(
            1,
            new ThreadFactoryBuilder().setNameFormat("VanillaMelody Client Scheduler").setDaemon(true).build()
    );

    private static final Path CACHE_DIR = FabricLoader.getInstance().getGameDir()
            .resolve("cache").resolve("vanillamelody").resolve("synced_midis");

    static {
        try {
            Files.createDirectories(CACHE_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ConcurrentHashMap<String, PlayList.SongInfo> CACHE = new ConcurrentHashMap<>();

    static {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            ClientPlayNetworking.registerReceiver(PacketConstants.SERVER_HELLO, (client1, handler1, buf, responseSender) -> {
                responseSender.sendPacket(PacketConstants.CLIENT_HELLO, new PacketByteBuf(Unpooled.buffer(0)));
            });
            ClientPlayNetworking.registerReceiver(PacketConstants.SERVER_MIDI_FILE_RESPONSE, (client1, handler1, buf, responseSender) -> {
                final byte status = buf.readByte();
                if (status == 0x01) { // success
                    final byte[] requestedHash = new byte[DigestUtils.SHA256_BYTES];
                    buf.readBytes(requestedHash);
                    final int length = buf.readVarInt();
                    final byte[] sequenceBytes = new byte[length];
                    buf.readBytes(sequenceBytes);
                    putInCache(requestedHash, sequenceBytes);
                }
            });
        });
    }

    private static void putInCache(byte[] sha256, byte[] sequenceBytes) {
        try {
            final PlayList.SongInfo song = new PlayList.SongInfo(sequenceBytes, DigestUtils.bytesToHex(sha256));
            Preconditions.checkArgument(Arrays.equals(sha256, song.sha256()), "hash mismatch");
            CACHE.put(DigestUtils.bytesToHex(song.sha256()), song);
            try {
                Files.write(CACHE_DIR.resolve(DigestUtils.bytesToHex(song.sha256())), sequenceBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (InvalidMidiDataException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PlayList.SongInfo get(byte[] sha256, boolean request) {
        String sha256String = DigestUtils.bytesToHex(sha256);
        final PlayList.SongInfo cached = CACHE.get(sha256String);
        if (cached != null) return cached;

        // in-memory cache check failed, check disk
        try {
            if (Files.exists(CACHE_DIR.resolve(sha256String))) {
                final byte[] sequenceBytes = Files.readAllBytes(CACHE_DIR.resolve(sha256String));
                if (Arrays.equals(DigestUtils.sha256(sequenceBytes), sha256)) {
                    putInCache(sha256, sequenceBytes);
                    return CACHE.get(sha256String);
                } else {
                    Files.delete(CACHE_DIR.resolve(sha256String));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // disk cache check failed
        final ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (request && networkHandler != null) {
            // request from server
            final PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(DigestUtils.SHA256_BYTES));
            buf.writeBytes(sha256);
            networkHandler.sendPacket(ClientPlayNetworking.createC2SPacket(PacketConstants.CLIENT_MIDI_FILE_REQUEST, buf));
        }
        return null;
    }

    public static void init() {
    }

    // below are non-threadsafe code, only run on scheduler thread
    private static final Int2ReferenceOpenHashMap<ClientSongPlayer> SONG_PLAYERS = new Int2ReferenceOpenHashMap<>();
    private static final Int2ReferenceFunction<ClientSongPlayer> NEW_SONG_PLAYER = ignored -> new ClientSongPlayer();

    static {
        EXECUTOR.scheduleAtFixedRate(() -> {
            final ObjectIterator<Int2ReferenceMap.Entry<ClientSongPlayer>> iterator = SONG_PLAYERS.int2ReferenceEntrySet().fastIterator();
            while (iterator.hasNext()) {
                final var entry = iterator.next();
                final int syncId = entry.getIntKey();
                try {
                    entry.getValue().tick(syncId);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }, 20, 20, TimeUnit.MILLISECONDS);

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            ClientPlayNetworking.registerReceiver(PacketConstants.SERVER_PLAYBACK_INIT, (client1, handler1, buf, responseSender) -> {
                final int syncId = buf.readInt();
                final Int2ObjectOpenHashMap<MidiInstruments.MidiInstrument> instruments = MidiInstruments.readInstruments(buf);
                final Int2ObjectOpenHashMap<MidiInstruments.MidiPercussion> percussions = MidiInstruments.readPercussions(buf);
                EXECUTOR.execute(() -> {
                    SONG_PLAYERS.computeIfAbsent(syncId, NEW_SONG_PLAYER).init(instruments, percussions);
                });
            });
            ClientPlayNetworking.registerReceiver(PacketConstants.SERVER_PLAYBACK_SEQUENCE_CHANGE, (client1, handler1, buf, responseSender) -> {
                final int syncId = buf.readInt();
                final byte[] sha256 = new byte[DigestUtils.SHA256_BYTES];
                buf.readBytes(sha256);
                final long tickPosition = buf.readLong();
                final long microsecondsPosition = buf.readLong();
                EXECUTOR.execute(() -> {
                    SONG_PLAYERS.computeIfAbsent(syncId, NEW_SONG_PLAYER).sequenceChange(sha256, tickPosition, microsecondsPosition);
                });
            });
            ClientPlayNetworking.registerReceiver(PacketConstants.SERVER_PLAYBACK_STOP, (client1, handler1, buf, responseSender) -> {
                final int syncId = buf.readInt();
                EXECUTOR.execute(() -> {
                    final ClientSongPlayer player = SONG_PLAYERS.get(syncId);
                    if (player != null) {
                        player.close();
                        SONG_PLAYERS.remove(syncId);
                    }
                });
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            EXECUTOR.execute(() -> {
                SONG_PLAYERS.values().forEach(ClientSongPlayer::close);
                SONG_PLAYERS.clear();
            });
        });
    }

}
