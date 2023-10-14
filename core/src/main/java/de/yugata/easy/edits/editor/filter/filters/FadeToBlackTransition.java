package de.yugata.easy.edits.editor.filter.filters;

import de.yugata.easy.edits.editor.EditInfo;
import de.yugata.easy.edits.editor.filter.FilterInfo;
import de.yugata.easy.edits.editor.filter.FilterType;
import de.yugata.easy.edits.editor.filter.TransitionVideoFilter;

@FilterInfo(name = "FadeBlack", value = "200", description = "Fades to black after one clip for a certain amount of milliseconds.", filterType = FilterType.TRANSITION)
public class FadeToBlackTransition extends TransitionVideoFilter {


    public FadeToBlackTransition(final EditInfo editInfo) {
        super(editInfo);

        final String filter = String.format("fade=t=in:st=%dus:d=%sms", 0, getValue());
        setFilter(filter);
    }
}
