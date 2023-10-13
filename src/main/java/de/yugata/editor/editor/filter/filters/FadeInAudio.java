package de.yugata.editor.editor.filter.filters;

import de.yugata.editor.editor.EditInfo;
import de.yugata.editor.editor.filter.FilterInfo;
import de.yugata.editor.editor.filter.FilterType;
import de.yugata.editor.editor.filter.SimpleAudioFilter;

@FilterInfo(name = "FadeInAudio", description = "Fade in the audio of the edit for a certain amount of time.", value = "4", filterType = FilterType.AUDIO)
public class FadeInAudio extends SimpleAudioFilter {
    public FadeInAudio(EditInfo editInfo) {
        super(editInfo);

        final int fadeInLength = Integer.parseInt(getValue());
        this.setFilter(String.format("afade=t=in:d=%d", fadeInLength));
    }
}
