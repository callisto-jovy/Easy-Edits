package de.yugata.editor.editor.filter.filters;

import de.yugata.editor.editor.EditInfo;
import de.yugata.editor.editor.filter.FilterInfo;
import de.yugata.editor.editor.filter.FilterType;
import de.yugata.editor.editor.filter.SimpleAudioFilter;

@FilterInfo(name = "FadeOutAudio", description = "Fade out the audio of the edit for a certain amount of time.", value = "4", filterType = FilterType.AUDIO)
public class FadeOutAudio extends SimpleAudioFilter {

    public FadeOutAudio(EditInfo inputVideo) {
        super(inputVideo);

        final int fadeOutLength = Integer.parseInt(getValue());
        final int fadeOutStart = (int) ((inputVideo.getEditTime() / 1000000L) - fadeOutLength);
        this.setFilter(String.format("afade=t=out:st=%d:d=%d", fadeOutStart - 1, fadeOutLength));
    }
}
