package de.yugata.editor.util;

public class AudioUtil {

    public static long calculateLengthInMilliseconds(final int frames, final float frameRate) {
        return  (long) (frames / frameRate * 1000);
    }
}