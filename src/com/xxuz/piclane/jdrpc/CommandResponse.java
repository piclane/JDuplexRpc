package com.xxuz.piclane.jdrpc;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * 
 * 
 * @author yohei_hina
 */
public abstract class CommandResponse implements Command {
	/** serialVersionUID */
	private static final long serialVersionUID = 636424130375503817L;
	
	/** メッセージID */
	private final UUID messageId;
	
	/**
	 * コンストラクタ
	 * 
	 * @param messageId メッセージID
	 */
	private CommandResponse(UUID messageId) {
		super();
		this.messageId = messageId;
	}
	
	/**
	 * messageId を取得します
	 *
	 * @return messageId
	 */
	public UUID getMessageId() {
		return messageId;
	}
	
	public static final class Register extends CommandResponse {
		/** serialVersionUID */
		private static final long serialVersionUID = -8440850760342954599L;
		
		/**
		 * コンストラクタ
		 * 
		 * @param messageId メッセージID
		 */
		public Register(UUID messageId) {
			super(messageId);
		}
	}	
	
	/**
	 * インスタンスの登録解除
	 */
	public static final class Deregister extends CommandResponse {
		/** serialVersionUID */
		private static final long serialVersionUID = 8527267899563638378L;

		/**
		 * コンストラクタ
		 * 
		 * @param messageId メッセージID
		 */
		public Deregister(UUID messageId) {
			super(messageId);
		}
	}
	
	public static final class Invoke extends CommandResponse {
		/** serialVersionUID */
		private static final long serialVersionUID = -4478492364586325237L;

		/** {@link #returnValue} の内容 */
		private final ObjectType objectType;
		
		/** 返値、もしくは実行中に発生した例外 */
		private final Object returnValue;
		
		/**
		 * コンストラクタ
		 * 
		 * @param messageId メッセージID
		 * @param exceptionOccurred 例外が発生した場合 true そうでない場合 false
		 * @param returnValue 返値
		 */
		public Invoke(UUID messageId, ObjectType objectType, Object returnValue) {
			super(messageId);
			this.objectType = objectType;
			this.returnValue = returnValue;
		}
		
		/**
		 * 返値を取得します
		 * 
		 * @return 返値
		 * @throws InvocationTargetException 実行中に例外が発生した場合
		 */
		@SuppressWarnings("unchecked")
		public <T> T getReturnValue() throws InvocationTargetException {
			switch(objectType) {
				case Result:
					return (T)returnValue;
				case InvocationException:
					throw new InvocationTargetException((Throwable)returnValue);
				default:
				case InternalError:
					throw new InternalError((Throwable)returnValue);
			}
		}
		
		/**
		 * {@link Invoke#returnValue} の内容
		 */
		public enum ObjectType {
			/** 返値 */
			Result,
			
			/** メソッド実行中の例外 */
			InvocationException,
			
			/** 内部エラー */
			InternalError
		}
	}
	
	public static final class AddRpcOverride extends CommandResponse {
		/** serialVersionUID */
		private static final long serialVersionUID = 381853671057742388L;

		/**
		 * コンストラクタ
		 * 
		 * @param messageId メッセージID
		 */
		public AddRpcOverride(UUID messageId) {
			super(messageId);
		}
	}
	
	public static final class RemoveRpcOverride extends CommandResponse {
		/** serialVersionUID */
		private static final long serialVersionUID = 3143555575717412665L;

		/**
		 * コンストラクタ
		 * 
		 * @param messageId メッセージID
		 */
		public RemoveRpcOverride(UUID messageId) {
			super(messageId);
		}
	}
	
	/**
	 * 終了コマンド
	 */
	public static final class Exit extends CommandResponse {
		/** serialVersionUID */
		private static final long serialVersionUID = -3504172088649747722L;

		/**
		 * コンストラクタ
		 * 
		 * @param messageId メッセージID
		 */
		public Exit(UUID messageId) {
			super(messageId);
		}
	}
}
