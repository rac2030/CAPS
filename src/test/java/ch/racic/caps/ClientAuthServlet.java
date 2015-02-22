


/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class ClientAuthServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        X509Certificate[] certs = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");

        PrintWriter writer = resp.getWriter();


        if (certs == null) {
            writer.println("Cert: 0");
        } else {
            writer.println("Cert: " + certs.length);
            for (X509Certificate c : certs) {
                writer.println("<cert>" + certs[0].getSubjectDN() + "</cert>");
            }
        }

        writer.println();
        writer.println("<method>" + req.getMethod() + "</method>");
        writer.println("<headers>");
        writer.println("Request Headers:");
        Enumeration headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String hdr = (String) headerNames.nextElement();
            Enumeration headers = req.getHeaders(hdr);
            while (headers.hasMoreElements()) {
                String val = (String) headers.nextElement();
                writer.println(hdr + ": " + val);
            }
        }
        writer.println("</headers>");


        writer.close();
    }
}
