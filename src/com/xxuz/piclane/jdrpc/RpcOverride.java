package com.xxuz.piclane.jdrpc;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * 
 * 
 * @author piclane
 */
public class RpcOverride implements Serializable {
	/** serialVersionUID */
	private static final long serialVersionUID = 5779459387921437835L;

	/**
	 * メソッドに対しての {@link RpcOverride} を生成します
	 * 
	 * @param method 対象メソッド
	 * @return メソッドに対して生成された {@link RpcOverride}
	 */
	public static RpcOverride forMethod(java.lang.reflect.Method method) {
		return new RpcOverride(method, -1);
	}
	
	/**
	 * メソッドに対しての {@link RpcOverride} を生成します
	 * 
	 * @param cls 対象メソッドが存在するインターフェイス
	 * @param methodName メソッド名
	 * @param parameterTypes メソッドの引数タイプ
	 * @return メソッドに対して生成された {@link RpcOverride}
	 * @throws NoSuchMethodException 指定されたメソッドが存在しなかった場合
	 * @throws SecurityException セキュリティー違反が発生した場合
	 */
	public static RpcOverride forMethod(Class<?> cls, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
		return new RpcOverride(cls.getMethod(methodName, parameterTypes), -1);
	}
	
	/**
	 * メソッド引数に対しての {@link RpcOverride} を生成します
	 * 
	 * @param parameterIndex メソッド引数のインデックス
	 * @param method 対象メソッド
	 * @return メソッド引数に対して生成された {@link RpcOverride}
	 */
	public static RpcOverride forMethodParameter(int parameterIndex, java.lang.reflect.Method method) {
		return new RpcOverride(method, parameterIndex);
	}
	
	/**
	 * メソッド引数に対しての {@link RpcOverride} を生成します
	 * 
	 * @param parameterIndex メソッド引数のインデックス
	 * @param cls 対象メソッドが存在するインターフェイス
	 * @param methodName メソッド名
	 * @param parameterTypes メソッドの引数タイプ
	 * @return メソッド引数に対して生成された {@link RpcOverride}
	 * @throws NoSuchMethodException 指定されたメソッドが存在しなかった場合
	 * @throws SecurityException セキュリティー違反が発生した場合
	 */
	public static RpcOverride forMethodParameter(int parameterIndex, Class<?> cls, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
		return new RpcOverride(cls.getMethod(methodName, parameterTypes), parameterIndex);
	}
	
	/** 宣言されているクラス */
	private final Class<?> declaringClass;

	/** メソッド名 */
	private final String methodName;
	
	/** メソッドの引数 */
	private final Class<?>[] parameterTypes;
	
	/** パラメーターのインデックス */
	private final int parameterIndex;
	
	/**
	 * コンストラクタ
	 * 
	 * @param method 対象メソッド
	 * @param parameterIndex パラメーターのインデックス
	 */
	private RpcOverride(java.lang.reflect.Method method, int parameterIndex) {
		super();
		
		Class<?> declaringClass = method.getDeclaringClass();
		if((method.getModifiers() & Modifier.STATIC) == 0 && !declaringClass.isInterface()) {
			throw new IllegalArgumentException("インターフェイスのメソッドのみオーバーライド可能です");
		}
		if(parameterIndex < 0) {
			parameterIndex = -1;
		}
		if(parameterIndex >= method.getParameterCount()) {
			throw new IllegalArgumentException("引数インデックスが大きすぎます");
		}
		
		this.declaringClass = declaringClass;
		this.methodName = method.getName();
		this.parameterTypes = method.getParameterTypes();
		this.parameterIndex = parameterIndex;
	}

	/**
	 * method を取得します
	 *
	 * @return method 対象メソッド
	 */
	public java.lang.reflect.Method getMethod() {
		try {
			return declaringClass.getMethod(methodName, parameterTypes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * parameterIndex を取得します
	 *
	 * @return parameterIndex
	 */
	public int getParameterIndex() {
		return parameterIndex;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + parameterIndex;
		result = prime * result + Arrays.hashCode(parameterTypes);
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RpcOverride other = (RpcOverride) obj;
		if (declaringClass == null) {
			if (other.declaringClass != null)
				return false;
		} else if (!declaringClass.equals(other.declaringClass))
			return false;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (parameterIndex != other.parameterIndex)
			return false;
		if (!Arrays.equals(parameterTypes, other.parameterTypes))
			return false;
		return true;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Method method = getMethod();
		int parameterIndex = getParameterIndex();
		StringBuilder buf = new StringBuilder();
		if(parameterIndex >= 0) {
			buf.append("parameterIndex=");
			buf.append(parameterIndex);
			buf.append(" ");
		}
		buf.append(method);
		return buf.toString();
	}
}
