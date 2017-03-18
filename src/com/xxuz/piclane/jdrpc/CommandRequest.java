package com.xxuz.piclane.jdrpc;

import java.util.UUID;

/**
 * 
 * 
 * @author yohei_hina
 */
public abstract class CommandRequest implements Command {
	/** serialVersionUID */
	private static final long serialVersionUID = 8296575831448773457L;

	/** メッセージID */
	private final UUID messageId = UUID.randomUUID();
	
	/**
	 * コンストラクタ
	 */
	private CommandRequest() {
		super();
	}
	
	/**
	 * messageId を取得します
	 *
	 * @return messageId
	 */
	public UUID getMessageId() {
		return messageId;
	}

	public static final class Register extends CommandRequest {
		/** serialVersionUID */
		private static final long serialVersionUID = -5090390491907413681L;
		
		/** インスタンス名 */
		private final String name;
		
		/** インスタンスID */
		private final UUID instanceId;
		
		/** インターフェイスを示すクラス */
		private final Class<?>[] interfaces;
		
		/**
		 * コンストラクタ
		 * 
		 * @param name インスタンス名
		 * @param instanceId インスタンスID
		 * @param interfaceCls インターフェイスを示すクラス
		 */
		public Register(String name, UUID instanceId, Class<?>[] interfaceCls) {
			super();
			this.name = name;
			this.instanceId = instanceId;
			this.interfaces = interfaceCls;
		}
		
		/**
		 * name を取得します
		 *
		 * @return name
		 */
		public String getName() {
			return name;
		}

		/**
		 * instanceId を取得します
		 *
		 * @return instanceId
		 */
		public UUID getInstanceId() {
			return instanceId;
		}
		
		/**
		 * interfaceCls を取得します
		 *
		 * @return interfaceCls
		 */
		public Class<?>[] getInterfaces() {
			return interfaces;
		}
	}
	
	/**
	 * インスタンスの登録解除
	 */
	public static final class Deregister extends CommandRequest {
		/** serialVersionUID */
		private static final long serialVersionUID = -6284033457521019510L;

		/** インスタンスID */
		private final UUID instanceId;

		/**
		 * コンストラクタ
		 * 
		 * @param instanceId インスタンスID
		 */
		public Deregister(UUID instanceId) {
			super();
			this.instanceId = instanceId;
		}

		/**
		 * instanceId を取得します
		 *
		 * @return instanceId
		 */
		public UUID getInstanceId() {
			return instanceId;
		}
	}
	

	@SuppressWarnings("rawtypes")
	public static final class Invoke extends CommandRequest {
		/** serialVersionUID */
		private static final long serialVersionUID = 8622801715449784305L;

		private final UUID instanceId;
		
		private final Class<?> declaringClass;
		
		private final String methodName;
		
		private final Class[] parameterTypes;
		
		private final Object[] arguments;

		/**
		 * @param instanceId
		 * @param methodName
		 * @param parameterTypes
		 * @param arguments
		 */
		public Invoke(UUID instanceId, Class<?> declaringClass, String methodName, Class[] parameterTypes, Object[] arguments) {
			super();
			this.instanceId = instanceId;
			this.declaringClass = declaringClass;
			this.methodName = methodName;
			this.parameterTypes = parameterTypes;
			this.arguments = arguments;
		}

		/**
		 * instanceId を取得します
		 *
		 * @return instanceId
		 */
		public UUID getInstanceId() {
			return instanceId;
		}
		
		/**
		 * declaringClass を取得します
		 *
		 * @return declaringClass
		 */
		public Class<?> getDeclaringClass() {
			return declaringClass;
		}

		/**
		 * methodName を取得します
		 *
		 * @return methodName
		 */
		public String getMethodName() {
			return methodName;
		}

		/**
		 * parameterTypes を取得します
		 *
		 * @return parameterTypes
		 */
		public Class[] getParameterTypes() {
			return parameterTypes;
		}

		/**
		 * arguments を取得します
		 *
		 * @return arguments
		 */
		public Object[] getArguments() {
			return arguments;
		}
	}
	
	public static final class AddRpcOverride extends CommandRequest {
		/** serialVersionUID */
		private static final long serialVersionUID = 6772861725483500111L;
		
		/** {@link RpcOverride} */
		private final RpcOverride override;

		/**
		 * コンストラクタ
		 * 
		 * @param override {@link RpcOverride}
		 */
		public AddRpcOverride(RpcOverride override) {
			this.override = override;
		}

		/**
		 * override を取得します
		 *
		 * @return override
		 */
		public RpcOverride getOverride() {
			return override;
		}
	}
	
	public static final class RemoveRpcOverride extends CommandRequest {
		/** serialVersionUID */
		private static final long serialVersionUID = 7597877737057048439L;
		
		/** {@link RpcOverride} */
		private final RpcOverride override;

		/**
		 * コンストラクタ
		 * 
		 * @param override {@link RpcOverride}
		 */
		public RemoveRpcOverride(RpcOverride override) {
			this.override = override;
		}

		/**
		 * override を取得します
		 *
		 * @return override
		 */
		public RpcOverride getOverride() {
			return override;
		}
	}
	
	/**
	 * 終了コマンド
	 */
	public static final class Exit extends CommandRequest {
		/** serialVersionUID */
		private static final long serialVersionUID = -3485339817046783333L;
	}
}
