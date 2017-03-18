package com.xxuz.piclane.jdrpc;

import java.util.concurrent.Future;

/**
 * 
 * 
 * @author piclane
 */
public interface CommandStream extends AutoCloseable {
	
	/**
	 * ストリームが終了するまで待機します
	 * 
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public void join() throws InterruptedException;
	
	/**
	 * リモートから受け付けた要求コマンドを取得します
	 * 
	 * @return {@link CommandResponse}
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public CommandRequest take() throws InterruptedException;
	
	/**
	 * リモートに応答コマンドを送信します
	 * 
	 * @param resp {@link CommandResponse}
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public void put(CommandResponse resp) throws InterruptedException;
	
	/**
	 * リモートにコマンドを実行させます<br>
	 * このメソッドはコマンドのレスポンスが返えるまでブロックします
	 * 
	 * @param cmd {@link CommandRequest}
	 * @return {@link CommandResponse}
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public CommandResponse call(CommandRequest cmd) throws InterruptedException;
	
	/**
	 * リモートにコマンドを実行させます<br>
	 * このメソッドはコマンドのレスポンスが返えるまでブロックしません
	 * 
	 * @param cmd {@link CommandRequest}
	 * @return {@link CommandResponse} を取得する非同期計算の結果
	 */
	public Future<CommandResponse> callAsync(CommandRequest cmd);
	
	/**
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws InterruptedException;
}
