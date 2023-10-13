package de.yugata.editor.editor.filter.filters;

import de.yugata.editor.editor.EditInfo;
import de.yugata.editor.editor.filter.FilterInfo;
import de.yugata.editor.editor.filter.SimpleVideoFilter;
import de.yugata.editor.util.FFmpegUtil;

@FilterInfo(name = "FadeOutVideo", value = "4", description = "Fades the video to black at the end for a certain amount of time.")
public class FadeOutVideo extends SimpleVideoFilter {


    public FadeOutVideo(final EditInfo editInfo) {
        super(editInfo);

        final int fadeOutLength = Integer.parseInt(getValue());
        final int fadeOutStart = (int) ((editInfo.getEditTime() / 1000000L) - fadeOutLength);

        final String fadeFilter = String.format("fade=t=out:st=%d:d=%d, drawtext=fontcolor=white:enable='between(t,%d,%d):fontsize=128:fontfile='%s':text=yugata:x=(w-text_w)/2:y=(h-text_h)/2",
                fadeOutStart - 1, // st=%d
                fadeOutLength, // d = %d
                fadeOutStart, // between %d (0)
                fadeOutStart + fadeOutLength, // between %d (1)
                FFmpegUtil.getFontFile() //fontfile=%s

        ); // d=%d
        setFilter(fadeFilter);
    }
}
