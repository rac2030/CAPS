/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps.exceptions;

/**
 * Created by rac on 21.02.15.
 */
public class NotStartedException extends RuntimeException {

    /**
     * Used if server could not have been started with the given timeouts.
     *
     * @param msg Message with reason
     */
    public NotStartedException(String msg) {
        super(msg);
    }
}
