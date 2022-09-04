package com.ishland.vanillamelody.common.playback;

import net.minecraft.util.Identifier;

public class Constants {

    public static final String NAMESPACE = "vanillamelody";

    // hello packets have empty contents
    public static final Identifier SERVER_HELLO = new Identifier(NAMESPACE, "server_hello0");
    public static final Identifier CLIENT_HELLO = new Identifier(NAMESPACE, "client_hello0");

    // client midi request content: 32 bytes sha256 of the midi file
    public static final Identifier CLIENT_MIDI_FILE_REQUEST = new Identifier(NAMESPACE, "client_midi_file_request");
    // server midi response content:
    // byte: 0x00 if the midi file is not found, 0x01 if the midi file is found
    // 32 bytes sha256 of the midi file
    // VarInt: if the midi file is found, the length of the midi file in bytes
    // if the midi file is found, the following bytes are the midi file content
    public static final Identifier SERVER_MIDI_FILE_RESPONSE = new Identifier(NAMESPACE, "server_midi_file_response");

    // playback init
    // int32: sync id
    // int16: instrument count
    // for each instrument: int16 key + see MidiInstruments$MidiInstrument
    // int16: percussion count
    // for each percussion: int16 key + see MidiInstruments$MidiPercussion
    public static final Identifier SERVER_PLAYBACK_INIT = new Identifier(NAMESPACE, "server_playback_init_0");

    // sequence change
    // int32: sync id
    // 32 bytes sha256 of the midi file
    // int64: tick position
    // int64: microseconds position
    public static final Identifier SERVER_PLAYBACK_SEQUENCE_CHANGE = new Identifier(NAMESPACE, "server_playback_sequence_change");

    // playback stop
    // int32: sync id
    public static final Identifier SERVER_PLAYBACK_STOP = new Identifier(NAMESPACE, "server_playback_stop");

    // client sequence request
    // int32: sync id
    public static final Identifier CLIENT_PLAYBACK_SEQUENCE_REQUEST = new Identifier(NAMESPACE, "client_playback_sequence_request");

}
