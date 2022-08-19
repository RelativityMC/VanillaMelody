package com.ishland.vanillamelody.common.playback;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.SharedConstants;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

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
                        songs.add(new SongInfo(
                                MidiSystem.getSequence(file),
                                MidiSystem.getMidiFileFormat(file),
                                directory.toPath().relativize(file.toPath()).toString()
                        ));
                    } catch (InvalidMidiDataException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return new PlayList(songs);
    }


    record SongInfo(Sequence sequence, MidiFileFormat fileFormat, String relativeFilePath, String pathWithoutInvalidChars) {

        public SongInfo(Sequence sequence, MidiFileFormat fileFormat, String relativeFilePath) {
            this(sequence, fileFormat, relativeFilePath, SharedConstants.stripInvalidChars(relativeFilePath));
        }

    }

}
