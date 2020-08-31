/*
 * User: ophir
 * Date: Mar 28, 2009
 * Time: 12:52:22 AM
 */
package com.phyzicsz.rocket.reflection.exception;

public class ReflectionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public ReflectionException(String message) {
		super(message);
	}

	public ReflectionException(String message, Throwable cause) {
		super(message, cause);
	}

    public ReflectionException(Throwable cause) {
        super(cause);
    }
}
