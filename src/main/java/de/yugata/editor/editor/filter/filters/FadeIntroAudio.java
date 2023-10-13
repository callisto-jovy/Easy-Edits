package de.yugata.editor.editor.filter.filters;


import de.yugata.editor.editor.EditInfo;
import de.yugata.editor.editor.filter.FilterInfo;
import de.yugata.editor.editor.filter.FilterType;
import de.yugata.editor.editor.filter.SimpleAudioFilter;

@FilterInfo(name = "FadeIntroAudio", description = "Fade in the audio of the edit in the intro", value = "-1", filterType = FilterType.AUDIO)

public class FadeIntroAudio extends SimpleAudioFilter {
    public FadeIntroAudio(EditInfo editInfo) {
        super(editInfo);

        final long fadeInLength = editInfo.getIntroEnd() - editInfo.getIntroStart();
        this.setFilter(String.format("afade=t=in:d=%dus", fadeInLength));
    }
}
