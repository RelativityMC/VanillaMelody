package com.ishland.vanillamelody.common.playback;

import com.google.common.collect.Sets;
import com.ishland.vanillamelody.common.util.DigestUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class ServerSyncedPlaybackManager {

    static final Set<UUID> PLAYERS_WITH_CLIENT_INSTALLED = Sets.newConcurrentHashSet();

    private static final Int2ReferenceMap<ServerSongPlayer> SONG_PLAYERS = Int2ReferenceMaps.synchronize(new Int2ReferenceOpenHashMap<>(), ServerSyncedPlaybackManager.class);

    static {
        SONG_PLAYERS.put(0, ServerSongPlayer.INSTANCE);
    }

    static {
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ServerPlayNetworking.registerReceiver(handler, Constants.CLIENT_HELLO, (server1, player, handler1, buf, responseSender) -> {
                System.out.println("%s joined with client VanillaMelody installed".formatted(player.getName().asString()));
                PLAYERS_WITH_CLIENT_INSTALLED.add(player.getUuid());
                synchronized (ServerSyncedPlaybackManager.class) {
                    for (Int2ReferenceMap.Entry<ServerSongPlayer> entry : SONG_PLAYERS.int2ReferenceEntrySet()) {
                        entry.getValue().removePlayer(player);
                        entry.getValue().addPlayer(player);
                    }
                }
            });
            ServerPlayNetworking.registerReceiver(handler, Constants.CLIENT_MIDI_FILE_REQUEST, (server1, player, handler1, buf, responseSender) -> {
                final PlayList playList = ServerSongPlayer.playList;

                if (playList != null) __label01:{
                    if (buf.readableBytes() != DigestUtils.SHA256_BYTES) break __label01;
                    byte[] requestedHash = new byte[DigestUtils.SHA256_BYTES];
                    buf.readBytes(requestedHash);

                    for (PlayList.SongInfo song : playList.getSongs()) {
                        if (Arrays.equals(requestedHash, song.sha256())) {
                            final PacketByteBuf responseBuf = new PacketByteBuf(Unpooled.buffer(1 + DigestUtils.SHA256_BYTES + 5 + song.sequenceBytes().length));
                            responseBuf.writeByte(0x01);
                            responseBuf.writeBytes(requestedHash);
                            responseBuf.writeVarInt(song.sequenceBytes().length);
                            responseBuf.writeBytes(song.sequenceBytes());

                            responseSender.sendPacket(Constants.SERVER_MIDI_FILE_RESPONSE, responseBuf);
                            return;
                        }
                    }

                    // failed
                    final PacketByteBuf responseBuf = new PacketByteBuf(Unpooled.buffer(1));
                    responseBuf.writeByte(0x00);

                    responseSender.sendPacket(Constants.SERVER_MIDI_FILE_RESPONSE, responseBuf);
                }
            });
            ServerPlayNetworking.registerReceiver(handler, Constants.CLIENT_PLAYBACK_SEQUENCE_REQUEST, (server1, player, handler1, buf, responseSender) -> {
                final int syncId = buf.readInt();
                final ServerSongPlayer songPlayer = SONG_PLAYERS.get(syncId);
                if (songPlayer != null) {
                    songPlayer.notifySongChange(player);
                }
            });
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sender.sendPacket(Constants.SERVER_HELLO, new PacketByteBuf(Unpooled.buffer(0)));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PLAYERS_WITH_CLIENT_INSTALLED.remove(handler.player.getUuid());
        });
    }

    public static void init() {
    }

}
