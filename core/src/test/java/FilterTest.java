import de.yugata.easy.edits.editor.EditInfo;
import de.yugata.easy.edits.editor.EditInfoBuilder;
import de.yugata.easy.edits.editor.FlutterWrapper;
import de.yugata.easy.edits.editor.filter.Filter;
import de.yugata.easy.edits.editor.filter.FilterManager;
import de.yugata.easy.edits.editor.filter.FilterType;
import de.yugata.easy.edits.editor.filter.FilterWrapper;
import org.bytedeco.javacv.FFmpegFrameFilter;

import java.util.List;
import java.util.stream.Collectors;

public class FilterTest {


    public static void main(String[] args) {

        final List<FlutterWrapper.FlutterFilterWrapper> videoFilters = FilterManager.FILTER_MANAGER.getAvailableFilters().stream().filter(flutterFilterWrapper -> flutterFilterWrapper.getFilterType() == FilterType.VIDEO).collect(Collectors.toList());

        final List<FilterWrapper> wrappers = videoFilters
                .stream()
                .map(flutterFilterWrapper -> new FilterWrapper(flutterFilterWrapper.getName(), flutterFilterWrapper.getValues()))
                .collect(Collectors.toList());

        final EditInfo testInfo = new EditInfoBuilder()
                .setEditTime(10000000)
                .createEditInfo();

        FilterManager.FILTER_MANAGER.populateFilters(wrappers, testInfo);
        combineFilters();

    }

    private static void combineFilters() {
        final StringBuilder combinedFilters = new StringBuilder();

        final List<Filter> videoFilters = FilterManager.FILTER_MANAGER.getVideoFilters();

        if (videoFilters.isEmpty())
            return;

        // Add in to first filter...
        combinedFilters.append("[in]");

        for (int i = 0; i < videoFilters.size(); i++) {
            final Filter videoFilter = videoFilters.get(i);

            if (i > 0) {
                combinedFilters
                        .append("[f")
                        .append((i - 1))
                        .append("]");
            }

            combinedFilters
                    .append(videoFilter.getFilter());

            if (i < videoFilters.size() - 1) {
                combinedFilters
                        .append("[f")
                        .append(i)
                        .append("]")
                        .append("; ");
                // --> [f i]; so that in the next iteration this will be the input.
            }
        }

        combinedFilters.append("[out]");

        System.out.println(combinedFilters);

        final FFmpegFrameFilter filterTest = new FFmpegFrameFilter(combinedFilters.toString(), 1920, 1080);
        try {
            filterTest.start();
        } catch (FFmpegFrameFilter.Exception e) {
            throw new RuntimeException(e);
        }

    }
}
