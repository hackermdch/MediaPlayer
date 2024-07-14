package net.hacker.mediaplayer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.util.Arrays;

public class NativeFileDialog {
    public static File openFileDialog(String title, String path, String filterDescription, String... filters) {
        String result;
        if (System.getProperty("os.name").startsWith("Windows")) {
            path = path.replace("/", "\\");
        } else {
            path = path.replace("\\", "/");
        }
        if (filters.length != 0) {
            try (var stack = MemoryStack.stackPush()) {
                var pointerBuffer = stack.mallocPointer(filters.length);
                Arrays.stream(filters).forEach(it -> pointerBuffer.put(stack.UTF8(it)));
                pointerBuffer.flip();
                result = TinyFileDialogs.tinyfd_openFileDialog(title, path, pointerBuffer, filterDescription, false);
            }
        } else {
            result = TinyFileDialogs.tinyfd_openFileDialog(title, path, null, filterDescription, false);
        }
        return result == null ? null : new File(result);
    }
}