package com.ishland.vanillamelody.common.playback.synth;

import com.ishland.vanillamelody.common.playback.data.Note;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public interface NoteReceiver {

    void playNote(Note note, BooleanSupplier isDone);

}
