import de.yugata.easy.edits.editor.FlutterWrapper;
import de.yugata.easy.edits.editor.filter.FilterManager;
import de.yugata.easy.edits.editor.filter.FilterWrapper;

public class FilterTest {


    public static void main(String[] args) {

        for (FilterWrapper filter : FlutterWrapper.getFilters()) {
            System.out.println(filter.getName());
            System.out.println(filter.getValues());
            System.out.println(filter.getDescription());
        }

    }
}
