package de.yugata.easy.edits.editor;

import com.google.gson.JsonObject;

public class VideoClip {

    private final long timeStamp;
    private final boolean muteAudio;


    public VideoClip(long timeStamp, boolean muteAudio) {
        this.timeStamp = timeStamp;
        this.muteAudio = muteAudio;
    }


    public VideoClip(final JsonObject jsonElement) {
        this.timeStamp = jsonElement.get("time_stamp").getAsLong();
        this.muteAudio = jsonElement.get("mute_audio").getAsBoolean();
    }


    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean isMuteAudio() {
        return muteAudio;
    }
}
