package de.yugata.editor.editor.filter.filters;

import de.yugata.editor.editor.EditInfo;
import de.yugata.editor.editor.EditingFlag;
import de.yugata.editor.editor.filter.FilterInfo;
import de.yugata.editor.editor.filter.FilterType;
import de.yugata.editor.editor.filter.TransitionVideoFilter;

@FilterInfo(name = "FadeBlack", value = "200", description = "Fades to black after one clip for a certain amount of milliseconds.", filterType = FilterType.TRANSITION)
public class FadeToBlackTransition extends TransitionVideoFilter {


    public FadeToBlackTransition(final EditInfo editInfo) {
        super(editInfo);

        final String filter = String.format("fade=t=in:st=%dus:d=%sms", 0, getValue());
        setFilter(filter);
    }
}
