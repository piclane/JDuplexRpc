package com.xxuz.piclane.jdrpc;

import java.io.Serializable;
import java.util.UUID;

/**
 * 
 * 
 * @author yohei_hina
 */
public interface Command extends Serializable {
	/**
	 * messageId を取得します
	 *
	 * @return messageId
	 */
	public UUID getMessageId();
}
