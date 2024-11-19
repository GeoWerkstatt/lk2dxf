package ch.geowerkstatt.lk2dxf;

import java.io.IOException;
import java.io.Writer;

public final class MultiWriter extends Writer {
    private final Writer[] writers;

    public MultiWriter(Writer... writers) {
        this.writers = writers;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (Writer writer : writers) {
            writer.write(cbuf, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        for (Writer writer : writers) {
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        for (Writer writer : writers) {
            writer.close();
        }
    }
}
