package net.hacker.mediaplayer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.nio.ByteBuffer;

public class NativeFileDialog {
    public static File openFileDialog(String title, String path, String filterDescription, String... filters) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 准备过滤器
            PointerBuffer filterPatterns = null;
            if (filters != null && filters.length > 0) {
                filterPatterns = stack.mallocPointer(filters.length);
                for (String filter : filters) {
                    ByteBuffer filterBuffer = stack.UTF8(filter);
                    filterPatterns.put(filterBuffer);
                }
                filterPatterns.flip();
            }

            // 调用原生文件对话框
            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    title,
                    path,
                    filterPatterns,
                    filterDescription,
                    false
            );

            // 处理结果
            if (result != null) {
                return new File(result);
            }
            return null;
        } catch (Exception e) {
            MediaPlayer.LOGGER.error("Failed to open native file dialog", e);
            return null;
        }
    }
}