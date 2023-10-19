import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.ffmpeg.*;
import de.yugata.easy.edits.editor.EditInfo;
import de.yugata.easy.edits.editor.EditInfoBuilder;
import de.yugata.easy.edits.editor.FlutterWrapper;
import de.yugata.easy.edits.editor.filter.FilterManager;
import de.yugata.easy.edits.editor.filter.FilterType;
import de.yugata.easy.edits.editor.filter.FilterWrapper;
import de.yugata.easy.edits.util.FFmpegUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilterStichTest {


    public static void main(String[] args) {
        /* Filter loading boilerplate */

        final List<FlutterWrapper.FlutterFilterWrapper> videoFilters = FilterManager.FILTER_MANAGER.getAvailableFilters()
                .stream()
                .filter(wrapper -> wrapper.getFilterType() == FilterType.VIDEO)
                .collect(Collectors.toList());

        final List<FilterWrapper> wrappers = videoFilters
                .stream()
                .map(wrapper -> new FilterWrapper(wrapper.getName(), wrapper.getValues()))
                .collect(Collectors.toList());

        final EditInfo testInfo = new EditInfoBuilder()
                .setEditTime(5000000)
                .createEditInfo();


        FilterManager.FILTER_MANAGER.populateFilters(wrappers, testInfo);


        final int segments = 4;

        final String resourcePath = "D:\\Edits\\Edits\\Whiplash let it happen\\editor_out\\segment %d.mp4";
        final String output = "out.mp4";

        final FFmpeg fFmpeg = FFmpeg.atPath(new File("D:\\Programs\\ffmpeg\\bin").toPath());

        for (int i = 0; i < segments; i++) {
            fFmpeg.addInput(UrlInput.fromUrl(String.format(resourcePath, i)));
        }

        // Filters are combined into one string. NOTE:: Complex filters are not included here!
        final String complexFilters = FFmpegUtil.chainComplexFilters(segments);


        final String complexConcat = String.format("concat=n=%d:v=1:a=0 [v]", segments);

        fFmpeg.setComplexFilter(complexFilters.concat(complexConcat));
        System.out.println("complexFilters.concat(complexConcat) = " + complexFilters.concat(complexConcat));

        fFmpeg.addArguments("-map", "[v]");
        fFmpeg.addArguments("-map", String.format("%d:a", segments));



        //TODO: Do this programmatically
        // TODO: Dynamic ffmpeg downloading
        final FFmpegResult result = fFmpeg
                .addInput(UrlInput.fromUrl("D:\\Edits\\Music\\Let it happen\\let_it_happen.wav"))
                .addOutput(UrlOutput.toUrl(output))
                //     .setFilter(StreamType.VIDEO, videoFilter)
                .setLogLevel(LogLevel.INFO)
                .addArguments("-c:v", "libx265")
                .setOutputListener(
                        System.out::println
                )
                .addArgument("-shortest")
                .setOverwriteOutput(true)
                .execute();

        System.out.println(result.getVideoSize());
    }
}
