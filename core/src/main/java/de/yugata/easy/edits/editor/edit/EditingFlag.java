package de.yugata.easy.edits.editor.edit;

public enum EditingFlag {

    WRITE_HDR_OPTIONS("Writes HDR tags to the video options. Should only be used with HDR content."),
    BEST_QUALITY("Will use the best quality preset by FFMPEG"),
    SHUFFLE_SEQUENCES("Shuffles the video timestamps, which may lead to more variety in the end product. If you selected your scenes with care and they follow a particular order, this should be disabled."),
    PRINT_DEBUG("Prints the ffmpeg debug information. Useful when submitting / tracing bugs"),
    OFFSET_AUDIO_FOR_INTRO("Will offset the audio for the entire intro time if an intro is given."),
    PROCESS_SEGMENTS("WIP");

    private final String description;
    private int setting;

    EditingFlag(final String description) {
        this.description = description;
        this.setting = setting;
    }

    public String getDescription() {
        return description;
    }
}
