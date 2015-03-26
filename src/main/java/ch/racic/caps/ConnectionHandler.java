/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps;

import ch.racic.caps.utils.StreamCopyThread;
import ch.racic.caps.utils.html.HTMLElement;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rac on 16.02.15. Inspiration and lots of code taken from Grinder Project HTTPProxyTCPProxyEngine class.
 */
public class ConnectionHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ConnectionHandler.class);
    private static volatile long connectionCounter = 0;
    private final Socket clientSocket;
    private final ICapsConfiguration configuration;
    private final Pattern httpConnectPattern;
    private final Pattern httpsConnectPattern;
    private SSLSocket clientSslSocket;
    private Socket targetSocket;
    private SSLSocket targetSslSocket;
    private Socket proxySocket;
    private StreamCopyThread client2targetCopy, target2clientCopy;
    private int bufferSize;

    /**
     * Handles a single incoming connection. It determines if HTTP or a SSL CONNECT command has been sent, creates the
     * desired target socket and kicks off the stream copy. In case of errors it will send an appropriate HTML error
     * response to the client socket.
     *
     * @param clientSocket
     * @param configuration
     */
    public ConnectionHandler(final Socket clientSocket, final ICapsConfiguration configuration) {
        this.clientSocket = clientSocket;
        this.configuration = configuration;
        // Patterns taken from Grinder Project after studying how it handles the CONNECT command
        // More information about CONNECT can be found in the RFC => http://tools.ietf.org/html/rfc2817#section-5.2
        httpConnectPattern = Pattern.compile("^([A-Z]+)[ \\t]+http://([^/:]+):?(\\d*)/.*\r\n\r\n", Pattern.DOTALL);
        httpsConnectPattern = Pattern.compile("^CONNECT[ \\t]+([^:]+):(\\d+).*\r\n\r\n", Pattern.DOTALL);

        bufferSize = 40960;

    }

    public void run() {
        try {
            interruptibleRun();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.info("ConnectionHandler thread has been interrupted");
        } finally {
            shutdown();
        }
    }

    /**
     * Interruptible method which is used to catch the Interrupt in the run method and handle it properly with a
     * shutdown.
     *
     * @throws InterruptedException
     */
    private void interruptibleRun() throws InterruptedException {
        final byte[] buffer = new byte[bufferSize];

        try {


            final BufferedInputStream clientIn = new BufferedInputStream(clientSocket.getInputStream(), buffer.length);
            clientIn.mark(buffer.length);
            int time = 0;
            while (true) {
                while (time < configuration.getConnectionTimeout() && clientIn.available() == 0) {
                    Thread.currentThread().sleep(1);
                    time += 1;
                }
                final boolean timeout = clientIn.available() == 0;

                // Rewind our buffered stream: easier than maintaining a cursor.
                clientIn.reset();

                final int bytesRead;

                if (clientIn.available() > 0) {
                    bytesRead = clientIn.read(buffer);
                } else {
                    bytesRead = 0;
                }

                final String bufferAsString =
                        new String(buffer, 0, bytesRead, "US-ASCII");

                if (timeout) {
                    // Time out without matching a handler.
                    final HTMLElement message = new HTMLElement();

                    message.addElement("p").addText(
                            "Failed to determine proxy destination.");

                    if (bufferAsString.length() > 0) {
                        final HTMLElement paragraph1 = message.addElement("p");
                        paragraph1.addText(
                                "Do not type TCPProxy address into your browser. ");
                        paragraph1.addText("The browser proxy settings should be set " +
                                "to the TCPProxy address (");
                        paragraph1.addElement("code").addText(clientSocket.getInetAddress().getCanonicalHostName() + ":" + configuration.getProxyListenerPort());
                        paragraph1.addText("), and you should type the address of the " +
                                "target server into the browser.");
                        message.addElement("p").addText(
                                "Text of received message follows:");
                        message.addElement("p").addElement("pre")
                                .addElement("blockquote").addText(bufferAsString);
                    } else {
                        message.addElement("p").addText(
                                "Client opened connection but sent no bytes.");
                    }

                    sendHTTPErrorResponse(message, "400 Bad Request",
                            clientSocket.getOutputStream());

                    shutdown();

                    break;
                }

                final Matcher httpConnectMatcher =
                        httpConnectPattern.matcher(bufferAsString);

                final Matcher httpsConnectMatcher =
                        httpsConnectPattern.matcher(bufferAsString);

                if (httpConnectMatcher.find()) {
                    // HTTP proxy request.
                    logger.debug("http match Buffer: " + bufferAsString);

                    // Reset stream to beginning of request.
                    clientIn.reset();

                    // handle HTTP connect
                    final String targetHost = httpConnectMatcher.group(2);
                    final String targetPortStr = httpConnectMatcher.group(3);
                    // if no port is in request, we use default http port
                    final int targetPort = (targetPortStr != null && !targetPortStr.equals("")) ? Integer.parseInt(targetPortStr) : 80;
                    targetSocket = new Socket(targetHost, targetPort);
                    // Start copy threads
                    client2targetCopy = new StreamCopyThread(clientIn, targetSocket.getOutputStream(), bufferSize, true);
                    target2clientCopy = new StreamCopyThread(targetSocket.getInputStream(), clientSocket.getOutputStream(), bufferSize, true);
                    client2targetCopy.start();
                    target2clientCopy.start();
                    client2targetCopy.join();
                    target2clientCopy.join();
                    break;
                } else if (httpsConnectMatcher.find()) {
                    logger.debug("https match Buffer: " + bufferAsString);

                    // handle HTTPS connect
                    final String targetHost = httpsConnectMatcher.group(1);
                    final String targetPortStr = httpsConnectMatcher.group(2);
                    // if no port is in request, we use default https port
                    final int targetPort = (targetPortStr != null && !targetPortStr.equals("")) ? Integer.parseInt(targetPortStr) : 443;
                    // Setup proxy socket if needed
                    if (configuration.getTargetProxy() != null) {
                        // Set targetSocket over an proxy socket
                        proxySocket = new Socket(configuration.getTargetProxy());
                        targetSslSocket = (SSLSocket) configuration.getTargetSslContext().getSocketFactory().createSocket(proxySocket, targetHost, targetPort, true /** autoclose **/);
                    } else {
                        // set targetSocket directly
                        final SSLContext targetSslContext = configuration.getTargetSslContext();
                        final SSLSocketFactory targetSslSocketFactory = targetSslContext.getSocketFactory();
                        targetSslSocket = (SSLSocket) targetSslSocketFactory.createSocket(targetHost, targetPort);
                    }

                    // Send a 200 response to send to client. Client
                    // will now start sending SSL data to localSocket.
                    final StringBuilder response = new StringBuilder();
                    response.append("HTTP/1.0 200 OK\r\n");
                    response.append("Proxy-agent: " + CAPServer.PROJECT_NAME + "-v." + CAPServer.PROJECT_VERSION);
                    response.append("\r\n");
                    response.append("\r\n");
                    clientSocket.getOutputStream().write(response.toString().getBytes());
                    clientSocket.getOutputStream().flush();
                    // Upgrade client socket to SSL
                    clientSslSocket = (SSLSocket) configuration.getProxySslContext().getSocketFactory().createSocket(clientSocket, clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort(), false);
                    // do handshake with client
                    clientSslSocket.setUseClientMode(false);
                    clientSslSocket.startHandshake();


                    // Start copy threads
                    client2targetCopy = new StreamCopyThread(clientSslSocket.getInputStream(), targetSslSocket.getOutputStream(), bufferSize, true);
                    target2clientCopy = new StreamCopyThread(targetSslSocket.getInputStream(), clientSslSocket.getOutputStream(), bufferSize, true);
                    client2targetCopy.start();
                    target2clientCopy.start();
                    client2targetCopy.join();
                    target2clientCopy.join();
                    break;
                }
                if (bytesRead == buffer.length) {
                    while (clientIn.available() > 0) {
                        // Drain.
                        clientIn.read(buffer);
                    }

                    final HTMLElement message = new HTMLElement();
                    message.addElement("p").addText(
                            "Buffer overflow - failed to match HTTP message after " +
                                    buffer.length + " bytes");

                    sendHTTPErrorResponse(message, "400 Bad Request",
                            clientSocket.getOutputStream());

                    break;
                }
            }
        } catch (IOException e) {
            shutdown();
            logger.error("IOException in ConnectionHandler", e);
        } catch (InterruptedException e) {
            shutdown();
            // handle interrupt
            Thread.currentThread().interrupt();
            throw e;
        } catch (CertificateException e) {
            shutdown();
            logger.error("CertificateException in ConnectionHandler.targetSocket", e);
        } catch (NoSuchAlgorithmException e) {
            shutdown();
            logger.error("NoSuchAlgorithmException in ConnectionHandler.targetSocket", e);
        } catch (UnrecoverableKeyException e) {
            shutdown();
            logger.error("UnrecoverableKeyException in ConnectionHandler.targetSocket", e);
        } catch (KeyStoreException e) {
            shutdown();
            logger.error("KeyStoreException in ConnectionHandler.targetSocket", e);
        } catch (ConfigurationException e) {
            shutdown();
            logger.error("ConfigurationException in ConnectionHandler.targetSocket", e);
        } catch (KeyManagementException e) {
            shutdown();
            logger.error("KeyManagementException in ConnectionHandler.targetSocket", e);
        }

    }

    /**
     * Sends a properly formatted HTML error response to the output stream.
     *
     * @param message
     * @param status
     * @param outputStream
     * @throws IOException
     */
    private void sendHTTPErrorResponse(final HTMLElement message,
                                       final String status,
                                       final OutputStream outputStream)
            throws IOException {
        logger.warn("Sent HTTP Error response to browser: " + message.toText());
        final HTTPResponse response = new HTTPResponse();
        response.setStatus(status);
        response.setMessage(status, message);

        outputStream.write(response.toString().getBytes("US-ASCII"));
    }


    /**
     * Interrupt all stream copy threads and close all opened sockets
     */
    private void shutdown() {
        if (client2targetCopy != null)
            client2targetCopy.interrupt();
        if (target2clientCopy != null)
            target2clientCopy.interrupt();
        if (clientSocket != null && clientSocket.isConnected()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Exception during closing of the client socket", e);
            }
        }

        if (clientSslSocket != null && clientSslSocket.isConnected()) {
            try {
                clientSslSocket.close();
            } catch (IOException e) {
                logger.error("Exception during closing of the client SSL socket", e);
            }
        }

        if (targetSocket != null && targetSocket.isConnected()) {
            try {
                targetSocket.close();
            } catch (IOException e) {
                logger.error("Exception during closing of the target socket", e);
            }
        }

        if (targetSslSocket != null && targetSslSocket.isConnected()) {
            try {
                targetSslSocket.close();
            } catch (IOException e) {
                logger.error("Exception during closing of the target SSL socket", e);
            }
        }

        if (proxySocket != null && proxySocket.isConnected()) {
            try {
                proxySocket.close();
            } catch (IOException e) {
                logger.error("Exception during closing of the proxy socket", e);
            }
        }


    }
}
