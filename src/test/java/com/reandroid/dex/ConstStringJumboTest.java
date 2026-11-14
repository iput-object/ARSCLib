package com.reandroid.dex;

import com.reandroid.TestUtils;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.model.DexClass;
import com.reandroid.dex.model.DexFile;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * A dex with more than 65535 strings: a plain {@code const-string} whose string
 * index ends up above 0xffff must be written as {@code const-string/jumbo}, with
 * branches and (nested, compacted) try blocks following the resized code.
 */
public class ConstStringJumboTest {

    private static final int FIELDS = 70000;

    private static final String METHOD = "" +
            ".class public La/M;\n" +
            ".super Ljava/lang/Object;\n" +
            "\n" +
            ".method public static run(I)Ljava/lang/String;\n" +
            "    .registers 4\n" +
            "    const-string v0, \"aaa\"\n" +
            "    :try_start_0\n" +
            "    if-eqz p0, :cond_0\n" +
            "    CS v0, \"zz_one\"\n" +
            "    :try_start_1\n" +
            "    CS v1, \"zz_two\"\n" +
            "    invoke-virtual {v1}, Ljava/lang/String;->length()I\n" +
            "    :try_end_1\n" +
            "    .catch Ljava/lang/IllegalStateException; {:try_start_1 .. :try_end_1} :catch_1\n" +
            "    CS v0, \"zz_three\"\n" +
            "    :cond_0\n" +
            "    invoke-virtual {v0}, Ljava/lang/String;->length()I\n" +
            "    :try_end_0\n" +
            "    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_0\n" +
            "    :goto_0\n" +
            "    return-object v0\n" +
            "    :catch_0\n" +
            "    move-exception v1\n" +
            "    CS v0, \"zz_four\"\n" +
            "    goto :goto_0\n" +
            "    :catch_1\n" +
            "    move-exception v1\n" +
            "    CS v0, \"zz_five\"\n" +
            "    goto :goto_0\n" +
            ".end method\n";

    @Test
    public void testConstStringWidenedToJumbo() throws IOException {
        // const-string in the source: the library has to widen it
        String widened = buildAndRead("plain", "const-string");
        // const-string/jumbo in the source: the expected layout
        String expected = buildAndRead("jumbo", "const-string/jumbo");

        Assert.assertTrue(widened, widened.contains("const-string/jumbo v0, \"zz_one\""));
        Assert.assertTrue(widened, widened.contains("const-string v0, \"aaa\""));
        Assert.assertEquals(expected, widened);
    }

    private static String buildAndRead(String name, String constString) throws IOException {
        File dir = new File(TestUtils.getTempDir(), "const-string-jumbo-" + name);
        File pkg = new File(dir, "a");
        Assert.assertTrue(pkg.isDirectory() || pkg.mkdirs());
        writeBigClass(new File(pkg, "Big.smali"));
        write(new File(pkg, "M.smali"), METHOD.replace("CS ", constString + " "));

        DexFile dexFile = DexFile.createDefault();
        dexFile.parseSmaliDirectory(dir);
        dexFile.refreshFull();
        byte[] bytes = dexFile.getBytes();
        dexFile.close();

        DexFile read = DexFile.read(bytes);
        DexClass dexClass = read.getDexClass(TypeKey.parse("La/M;"));
        Assert.assertNotNull(dexClass);
        String smali = dexClass.toSmali();
        read.close();
        return smali;
    }

    private static void writeBigClass(File file) throws IOException {
        // field names are strings: pushes the string section past 0xffff,
        // and "zz_*" sort after all of them
        StringBuilder builder = new StringBuilder();
        builder.append(".class public La/Big;\n.super Ljava/lang/Object;\n\n");
        for (int i = 0; i < FIELDS; i++) {
            builder.append(".field public static f");
            builder.append(i);
            builder.append(":I\n");
        }
        write(file, builder.toString());
    }

    private static void write(File file, String text) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(text);
        }
    }
}
