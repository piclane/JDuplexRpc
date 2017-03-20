package com.xxuz.piclane.jdrpc;

import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * 
 * 
 * @author piclane
 */
class ReferenceArray implements Serializable {
	/** serialVersionUID */
	private static final long serialVersionUID = -2062283685564104315L;
	
	/** an empty array */
	public static final ReferenceArray[] EMPTY_ARRAY = new ReferenceArray[0];
	
	/** an index of the parameter */
	private final int parameterIndex;
	
	/** an array object */
	private final Object array;

	/**
	 * Constructor
	 * 
	 * @param parameterIndex an index of the parameter
	 * @param array an array object
	 */
	public ReferenceArray(int parameterIndex, Object array) {
		super();
		this.parameterIndex = parameterIndex;
		this.array = array;
	}

	/**
	 * Returns an index of the parameter
	 *
	 * @return parameterIndex an index of the parameter
	 */
	public int getParameterIndex() {
		return parameterIndex;
	}

	/**
	 * Returns an array object
	 *
	 * @return array
	 */
	public Object getArray() {
		return array;
	}
	
    /**
     * Returns the length of the specified array object, as an {@code int}.
     *
     * @param array the array
     * @return the length of the array
     */
	public int length() {
		return Array.getLength(array);
	}
	
	/**
	 * Copy into another array object
	 * 
	 * @param other another array object
	 */
	public void copyInto(Object other) {
		if(other == null) {
			throw new NullPointerException();
		}
		if(!other.getClass().isArray()) {
			throw new IllegalArgumentException();
		}
		System.arraycopy(array, 0, other, 0, length());
	}
}
