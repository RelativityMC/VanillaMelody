package com.ishland.vanillamelody.common.playback;

import com.ishland.vanillamelody.common.util.DigestUtils;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.SharedConstants;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Objects;

public class PlayList {

    private final ReferenceArrayList<SongInfo> songs;

    private PlayList(ReferenceArrayList<SongInfo> songs) {
        this.songs = songs;
    }

    public ReferenceArrayList<SongInfo> getSongs() {
        return songs;
    }

    public static PlayList scan(File directory) {
        LinkedList<File> pendingScans = new LinkedList<>();
        ReferenceArrayList<SongInfo> songs = new ReferenceArrayList<>();

        directory.mkdirs();
        pendingScans.add(directory);
        File dir;
        while ((dir = pendingScans.poll()) != null) {
            final File[] files = dir.listFiles();
            if (files == null) continue;
            for (File file : Arrays.stream(files)
                    .sorted(Comparator.comparing(File::getName))
                    .toList()) {
                if (file.isDirectory()) {
                    pendingScans.add(file);
                } else if (file.getName().endsWith(".mid")) {
                    try {
                        byte[] fileContent;
                        try (final var input = new FileInputStream(file)) {
                            fileContent = input.readAllBytes();
                        }
                        songs.add(new SongInfo(
                                fileContent,
                                directory.toPath().relativize(file.toPath()).toString()
                        ));
                    } catch (InvalidMidiDataException | IOException e) {
                        System.out.println("Failed to load midi file: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
        return new PlayList(songs);
    }


    public record SongInfo(byte[] sequenceBytes, Sequence sequence, MidiFileFormat fileFormat, String relativeFilePath, String pathWithoutInvalidChars, byte[] sha256) {

        public SongInfo(byte[] sequenceBytes, String relativeFilePath) throws InvalidMidiDataException, IOException {
            this(sequenceBytes, Objects.requireNonNull(MidiSystem.getSequence(new ByteArrayInputStream(sequenceBytes))),
                    Objects.requireNonNull(MidiSystem.getMidiFileFormat(new ByteArrayInputStream(sequenceBytes))),
                    relativeFilePath, SharedConstants.stripInvalidChars(relativeFilePath),
                    DigestUtils.sha256(sequenceBytes));
        }

    }

}
