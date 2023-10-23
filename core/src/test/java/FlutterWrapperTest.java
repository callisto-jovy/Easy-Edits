import de.yugata.easy.edits.wrapper.FlutterWrapper;

public class FlutterWrapperTest {

    public static void main(String[] args) {
        System.out.println("Testing flags map.");

        FlutterWrapper.getEditingFlags().forEach((String key, String value) -> {
            System.out.println("Key:"  + key);
            System.out.println("Value:" + value);
        });
    }
}
