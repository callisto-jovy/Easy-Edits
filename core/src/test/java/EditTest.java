import de.yugata.easy.edits.editor.VideoEditor;
import de.yugata.easy.edits.editor.VideoEditorBuilder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class EditTest {


    public static void main(String[] args) throws IOException {
        final String path = "D:\\Code\\Projects\\Java\\Other\\simple_movie_editor\\core\\src\\main\\resources\\test_json.json";
        // Read json

        final String json = FileUtils.readFileToString(new File(path), Charset.defaultCharset());

        final VideoEditor videoEditor = new VideoEditorBuilder()
                .fromJson(json);

       // final List<File> rawSegments = videoEditor.collectSegments();
        final List<File> processedSegments = videoEditor.collectProcessedSegments();
        videoEditor.concatSegments(processedSegments); // Concat processed segments.

    }
}
