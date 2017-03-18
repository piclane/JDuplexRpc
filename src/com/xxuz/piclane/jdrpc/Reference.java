package com.xxuz.piclane.jdrpc;

import java.io.Serializable;
import java.util.UUID;

/**
 * 
 * 
 * @author yohei_hina
 */
class Reference implements Serializable {
	/** serialVersionUID */
	private static final long serialVersionUID = 1923209548497957045L;

	/** インスタンスID */
	private final UUID instanceId;

	/**
	 * コンストラクタ
	 * 
	 * @param instanceId インスタンスID
	 */
	public Reference(UUID instanceId) {
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
