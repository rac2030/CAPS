/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps.utils;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by rac on 17.02.15.
 */
public class StreamCopyThread extends Thread {

    private static final Logger logger = Logger.getLogger(StreamCopyThread.class);
    private static volatile long connectionCounter = 0;
    private final InputStream in;
    private final OutputStream out;
    private final byte[] buffer;
    private final boolean closeStreams;

    /**
     * Thread used to copy all content from an InputStream to an OutputStream.
     *
     * @param in           InputStream
     * @param out          OutputStream
     * @param bufferSize   Size of the buffer that is used to read from the InputStream
     * @param closeStreams Close involved Streams once this thread finishes normally or because of interruption
     */
    public StreamCopyThread(final InputStream in, final OutputStream out, final int bufferSize, final boolean closeStreams) {
        super("StreamCopyThread-" + ++connectionCounter);

        this.in = in;
        this.out = out;
        this.buffer = new byte[bufferSize];
        this.closeStreams = closeStreams;
    }

    public void run() {
        try {
            interruptibleRun();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.info("ConnectionHandler thread (" + this.getName() + ") has been interrupted");
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        if (!closeStreams) return;
        try {
            in.close();
        } catch (IOException e) {
            logger.debug("IOException while closing InputStream", e);
        }
        try {
            out.close();
        } catch (IOException e) {
            logger.debug("IOException while closing OutputStream", e);
        }
    }

    private void interruptibleRun() throws InterruptedException {
        try {
            while (true) {
                final int bytesRead = in.read(buffer, 0, buffer.length);

                if (bytesRead == -1) {
                    break;
                }

                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            logger.error("IOException during transfer from input to output stream", e);
            shutdown();
        }

    }
}
