package de.yugata.easy.edits.editor.filter.filters;


import de.yugata.easy.edits.editor.EditInfo;
import de.yugata.easy.edits.editor.filter.FilterInfo;
import de.yugata.easy.edits.editor.filter.SimpleVideoFilter;

@FilterInfo(name = "ZoomIn", description = "WIP", value = "200")
public class ZoomInFilter extends SimpleVideoFilter {
    public ZoomInFilter(EditInfo editInfo) {
        super(editInfo);
        setFilter("zoompan=z='min(pzoom+0.00213,2.13)':x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2):d=1:fps=60");
    }
}
