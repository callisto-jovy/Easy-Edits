package de.yugata.editor.editor.filter.filters;


import de.yugata.editor.editor.EditInfo;
import de.yugata.editor.editor.filter.FilterInfo;
import de.yugata.editor.editor.filter.SimpleVideoFilter;

@FilterInfo(name = "Interpolate", description = "Uses interpolation to create an motion blur effect & increase the framerate by predicting what frames between might look like.", value = "60")
public class InterpolateFilter extends SimpleVideoFilter {


    public InterpolateFilter(EditInfo editInfo) {
        super(editInfo);
        final double fps = Double.parseDouble(getValue());
        final int factor = (int) (fps / editInfo.getFrameRate());

        final String videoFilter = String.format("minterpolate=fps=%f,tblend=all_mode=average,setpts=%d*PTS", fps, factor);
        setFilter(videoFilter);
    }
}
