package de.yugata.editor.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Event;

import java.util.function.Consumer;

public class SWTUtil {

    public static void color(final Event event, final int color, final Consumer<Void> function) {
        final GC gc = event.gc;
        final Color previousColor = gc.getForeground();
        gc.setForeground(getColor(event, color));
        function.accept(null);
        gc.setForeground(previousColor);
    }

    public static Color getColor(final Event event, final int colorCode) {
        return event.display.getSystemColor(colorCode);
    }


}
