package com.ishland.vanillamelody.client.playback;

import com.ishland.vanillamelody.common.playback.NoteUtil;
import com.ishland.vanillamelody.common.playback.data.Note;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.function.BooleanSupplier;

public class SynthSoundInstance extends PositionedSoundInstance implements TickableSoundInstance {

    public static SynthSoundInstance create(Note note, BooleanSupplier isDone, float yaw) {
        final Identifier sound = new Identifier(NoteUtil.getSoundNameByInstrument(note.mcInstrument()));
        final Vec3d pos = NoteUtil.stereoPan(Vec3d.ZERO, yaw, (float) (note.panning() / 16.0));
        float volume = note.volume();
        return new SynthSoundInstance(
                sound,
                SoundCategory.RECORDS,
                volume,
                note.rawPitch(),
                false,
                0,
                SoundInstance.AttenuationType.LINEAR,
                pos.x,
                pos.y,
                pos.z,
                true,
                isDone
        );
    }

    private final BooleanSupplier isDone;
    private long playableUntil = System.currentTimeMillis() + 400L;

    private SynthSoundInstance(Identifier id, SoundCategory category, float volume, float pitch, boolean repeat, int repeatDelay, AttenuationType attenuationType, double x, double y, double z, boolean relative, BooleanSupplier isDone) {
        super(id, category, volume, pitch, Random.create(), repeat, repeatDelay, attenuationType, x, y, z, relative);
        this.isDone = isDone;
    }

    @Override
    public boolean isDone() {
        return System.currentTimeMillis() > playableUntil && this.isDone != null && this.isDone.getAsBoolean();
    }

    @Override
    public void tick() {

    }
}
