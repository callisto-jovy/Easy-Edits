package de.yugata.editor.editor;

public enum EditingFlag {

    WRITE_HDR_OPTIONS("Writes HDR tags to the video options. Should only be used with HDR content."),
    BEST_QUALITY("Will use the best quality preset by FFMPEG, i.e. cf=0"),
    SHUFFLE_SEQUENCES("Shuffles the video timestamps, which may lead to more variety in the end product. If you selected your scenes with care and they follow a particular order, this should be disabled."), //TODO: implement
    INTERPOLATE_FRAMES("Will interpolate and blend between the frames, giving the illusion of motion blur. This is not GPU-accelerated and might take a long time."),
    FADE_OUT_VIDEO("Will add a fade to black at the end of the edit.");


    private final String description;

    EditingFlag(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
