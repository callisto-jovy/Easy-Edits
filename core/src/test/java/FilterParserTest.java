import com.github.kokorin.jaffree.ffmpeg.FilterChain;
import com.github.kokorin.jaffree.ffmpeg.GenericFilter;
import de.yugata.easy.edits.util.FFmpegUtil;

public class FilterParserTest {


    public static void main(String[] args) {
        final String filter = "fade=t=out:st=$fadeStart$:d=$fadeDuration$, drawtext=text='yugata':enable='between(t,$fadeStart$,$fadeEnd$)':x=(w-text_w)/2:y=(h-text_h)/2:fontsize=128:fontfile='$font$':fontcolor=white";
        System.out.println("filter = " + filter);
        System.out.println();
        System.out.println();
        final FilterChain genericFilter = FFmpegUtil.parseFilterChain(filter);

        System.out.println(genericFilter.getValue());
    }
}
