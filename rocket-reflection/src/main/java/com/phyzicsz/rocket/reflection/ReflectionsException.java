/*
 * User: ophir
 * Date: Mar 28, 2009
 * Time: 12:52:22 AM
 */
package com.phyzicsz.rocket.reflection;

public class ReflectionsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public ReflectionsException(String message) {
		super(message);
	}

	public ReflectionsException(String message, Throwable cause) {
		super(message, cause);
	}

    public ReflectionsException(Throwable cause) {
        super(cause);
    }
}
