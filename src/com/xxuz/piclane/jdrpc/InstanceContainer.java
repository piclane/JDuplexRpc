package com.xxuz.piclane.jdrpc;

import java.util.UUID;

/**
 * インスタンスのコンテナ
 */
final class InstanceContainer {
	/** インスタンスが確定している場合は true そうで無い場合は false */
	private boolean settled;
	
	/** インスタンス名 */
	private String name;
	
	/** インスタンスID */
	private UUID id;
	
	/** インターフェイス */
	private Class<?>[] interfaces;
	
	/** インスタンス */
	private Object instance;
	
	/**
	 * コンストラクタ
	 */
	public InstanceContainer() {
		this.settled = false;
		this.name = null;
		this.id = null;
		this.instance = null;
	}
	
	/**
	 * コンストラクタ
	 * 
	 * @param インスタンス名
	 * @param instanceId インスタンスID
	 * @param interfaces インターフェイス
	 * @param instance インスタンス
	 */
	public InstanceContainer(String name, UUID instanceId, Class<?>[] interfaces, Object instance) {
		this.settled = true;
		this.name = name;
		this.id = instanceId;
		this.interfaces = interfaces;
		this.instance = instance;
	}

	/**
	 * 値を設定します
	 * 
	 * @param インスタンス名
	 * @param instanceId インスタンスID
	 * @param interfaces インターフェイス
	 * @param instance インスタンス
	 */
	public synchronized void set(String name, UUID instanceId, Class<?>[] interfaces, Object instance) {
		this.settled = true;
		this.name = name;
		this.id = instanceId;
		this.interfaces = interfaces;
		this.instance = instance;
	}
	
	/**
	 * settled を取得します
	 *
	 * @return settled
	 */
	public boolean isSettled() {
		return settled;
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
	 * id を取得します
	 *
	 * @return id
	 */
	public UUID getId() {
		return id;
	}
	
	/**
	 * interfaces を取得します
	 *
	 * @return interfaces
	 */
	public Class<?>[] getInterfaces() {
		return interfaces;
	}

	/**
	 * interfaces を設定します
	 * 
	 * @param interfaces interfaces
	 */
	public void setInterfaces(Class<?>[] interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * instance を取得します
	 *
	 * @return instance
	 */
	@SuppressWarnings("unchecked")
	public <T> T getInstance() {
		if(instance instanceof Dynamic) {
			return (T)((Dynamic)instance).get(this);
		} else {
			return (T)instance;
		}
	}
	
	public interface Dynamic {
		Object get(InstanceContainer container);
	}
}