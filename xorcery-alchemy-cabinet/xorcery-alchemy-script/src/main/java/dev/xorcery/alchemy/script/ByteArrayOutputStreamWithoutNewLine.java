package dev.xorcery.alchemy.script;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public class ByteArrayOutputStreamWithoutNewLine
    extends ByteArrayOutputStream
{
    @Override
    public String toString(Charset charset) {
        return new String(buf, 0, count-1, charset);
    }
}
