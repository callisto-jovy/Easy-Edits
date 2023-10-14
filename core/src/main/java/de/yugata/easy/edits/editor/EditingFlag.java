package de.yugata.easy.edits.editor;

public enum EditingFlag {

    WRITE_HDR_OPTIONS("Writes HDR tags to the video options. Should only be used with HDR content.", -1),
    BEST_QUALITY("Will use the best quality preset by FFMPEG, i.e. cf=0", 12),
    SHUFFLE_SEQUENCES("Shuffles the video timestamps, which may lead to more variety in the end product. If you selected your scenes with care and they follow a particular order, this should be disabled.", -1),
    PRINT_DEBUG("Prints the ffmpeg debug information. Useful when submitting / tracing bugs", -1),
    OFFSET_AUDIO_FOR_INTRO("Will offset the audio for the entire intro time if an intro is given.", -1);

    private final String description;
    private int setting;

    EditingFlag(final String description, int setting) {
        this.description = description;
        this.setting = setting;
    }

    public void setSetting(int setting) {
        this.setting = setting;
    }

    public int getSetting() {
        return setting;
    }

    public String getDescription() {
        return description;
    }
}
