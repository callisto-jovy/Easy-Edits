module Easy.Edits.core {

    exports de.yugata.easy.edits.editor;
    exports de.yugata.easy.edits.audio;
    exports de.yugata.easy.edits.editor.filter;

    requires java.logging;
    requires java.desktop;
    requires org.apache.commons.io;
    requires com.google.gson;
    requires org.bytedeco.javacv;
    requires org.bytedeco.ffmpeg;
}