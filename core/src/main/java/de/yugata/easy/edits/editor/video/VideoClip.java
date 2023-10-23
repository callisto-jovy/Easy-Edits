package de.yugata.easy.edits.editor.video;

import com.google.gson.JsonObject;

public class VideoClip {

    private final long timeStamp;
    private final long length;

    private final boolean muteAudio;


    public VideoClip(final JsonObject jsonElement) {
        this.timeStamp = jsonElement.get("time_stamp").getAsLong();
        this.muteAudio = jsonElement.get("mute_audio").getAsBoolean();
        this.length = jsonElement.has("clip_length") ? jsonElement.get("clip_length").getAsLong() : -1;
    }


    public long getLength() {
        return length;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean isMuteAudio() {
        return muteAudio;
    }
}
