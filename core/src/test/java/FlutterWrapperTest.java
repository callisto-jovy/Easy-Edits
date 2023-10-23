import de.yugata.easy.edits.wrapper.FlutterWrapper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class FlutterWrapperTest {

    public static void main(String[] args) throws IOException {
        final String path = "D:\\Code\\Projects\\Flutter\\video_editor\\easy_edits\\core\\src\\main\\resources\\preview_test.json";
        final String path2 = "D:\\Code\\Projects\\Flutter\\video_editor\\easy_edits\\core\\src\\main\\resources\\edit_preview_test.json";

        // Read json

        final String json = FileUtils.readFileToString(new File(path), Charset.defaultCharset());


     //   FlutterWrapper.previewSegment(json);

        final String json2 = FileUtils.readFileToString(new File(path2), Charset.defaultCharset());

        FlutterWrapper.editPreviews(json2);
    }
}
