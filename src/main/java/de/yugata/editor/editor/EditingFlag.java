package de.yugata.editor.editor;

public enum EditingFlag {

    WRITE_HDR_OPTIONS("Writes HDR tags to the video options. Should only be used with HDR content.", -1),
    BEST_QUALITY("Will use the best quality preset by FFMPEG, i.e. cf=0", 0),
    SHUFFLE_SEQUENCES("Shuffles the video timestamps, which may lead to more variety in the end product. If you selected your scenes with care and they follow a particular order, this should be disabled.", -1), //TODO: implement
    INTERPOLATE_FRAMES("Will interpolate and blend between the frames, giving the illusion of motion blur. This is not GPU-accelerated and might take a long time.", 120),
    FADE_OUT_VIDEO("Will add a fade to black at the end of the edit.", 4);


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
