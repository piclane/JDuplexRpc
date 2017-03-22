package com.xxuz.piclane.jdrpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * コマンドのストリームを表現します
 * 
 * @author yohei_hina
 */
public class DefaultCommandStream implements CommandStream {
	/** {@link ExecutorService} */
	private final ExecutorService es;
	
	/** 名前 */
	private final String name;
	
	/** 入力ストリーム */
	private final InputStream is;
	
	/** 出力ストリーム */
	private final OutputStream os;
	
	/** 入力コマンドキュー */
	private final BlockingQueue<CommandRequest> incomingCommands;
	
	/** 出力コマンドキュー */
	private final BlockingQueue<Command> outgoingCommands;
	
	/**
	 * Key:   メッセージID
	 * Value: {@link ResponseContainer}
	 */
	private final ConcurrentHashMap<UUID, ResponseContainer> responseContainers;
	
	/**
	 * コンストラクタ
	 * 
	 * @param is 入力ストリーム
	 * @param os 出力ストリーム
	 * @throws IOException 入出力例外が発生した場合
	 */
	public DefaultCommandStream(InputStream is, OutputStream os) throws IOException {
		this(null, is, os);
	}
	
	/**
	 * コンストラクタ
	 * 
	 * @param name 名前
	 * @param is 入力ストリーム
	 * @param os 出力ストリーム
	 * @throws IOException 入出力例外が発生した場合
	 */
	public DefaultCommandStream(String name, InputStream is, OutputStream os) throws IOException {
		this.es = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
		this.name = name != null && !name.isEmpty() ? name + "-" : "";
		this.is = is;
		this.os = os;
		this.incomingCommands = new LinkedBlockingQueue<>();
		this.outgoingCommands = new LinkedBlockingQueue<>();
		this.responseContainers = new ConcurrentHashMap<>();

		es.submit(new InputPump());
		es.submit(new OutputPump());
	}

	/**
	 * @see com.xxuz.piclane.jdrpc.CommandStream#join()
	 */
	public void join() throws InterruptedException {
		es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	}
	
	/**
	 * @see com.xxuz.piclane.jdrpc.CommandStream#take()
	 */
	@Override
	public CommandRequest take() throws InterruptedException {
		return incomingCommands.take();
	}
	
	/**
	 * @see com.xxuz.piclane.jdrpc.CommandStream#put(com.xxuz.piclane.jdrpc.CommandResponse)
	 */
	@Override
	public void put(CommandResponse resp) throws InterruptedException {
		outgoingCommands.put(resp);
	}
	
	/**
	 * @see com.xxuz.piclane.jdrpc.CommandStream#call(com.xxuz.piclane.jdrpc.CommandRequest)
	 */
	public CommandResponse call(CommandRequest cmd) throws InterruptedException {
		ResponseContainer rc = new ResponseContainer();
		responseContainers.put(cmd.getMessageId(), rc);
		outgoingCommands.add(cmd);
		synchronized (rc) {
			rc.wait();
			if(rc.response != null) {
				return rc.response;
			} else {
				throw new IllegalStateException();
			}
		}
	}
	
	/**
	 * @see com.xxuz.piclane.jdrpc.CommandStream#callAsync(com.xxuz.piclane.jdrpc.CommandRequest)
	 */
	public Future<CommandResponse> callAsync(CommandRequest cmd) {
		ResponseContainer rc = new ResponseContainer();
		responseContainers.put(cmd.getMessageId(), rc);
		outgoingCommands.add(cmd);
		return es.submit(new Callable<CommandResponse>() {
			@Override
			public CommandResponse call() throws Exception {
				synchronized (rc) {
					rc.wait();
					if(rc.response != null) {
						return rc.response;
					} else {
						throw new IllegalStateException();
					}
				}
			}
		});
	}
	
	/**
	 * @see com.xxuz.piclane.jdrpc.CommandStream#close()
	 */
	@Override
	public void close() throws InterruptedException {
		es.shutdownNow();
		es.awaitTermination(10L, TimeUnit.SECONDS);
	}
	
	/**
	 * {@link CommandResponse} の受け渡し時の同期を補助するクラス
	 */
	private static class ResponseContainer {
		/** {@link CommandResponse} */
		public CommandResponse response = null;
	}
	
	/**
	 * 入力ストリームからコマンドを取得していくスレッド
	 */
	private class InputPump implements Callable<Void> {
		/**
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Void call() throws Exception {
			Thread.currentThread().setName(name + "CommandStream-InputPump");
			
			try(ObjectInputStream ois = new ObjectInputStream(is)) {
				while(true) {
					Command cmd = (Command)ois.readUnshared();
					if(cmd instanceof CommandResponse) {
						ResponseContainer rc = responseContainers.get(cmd.getMessageId());
						if(rc != null) {
							synchronized (rc) {
								rc.response = (CommandResponse)cmd;
								rc.notifyAll();
							}
						}
					} else if(cmd instanceof CommandRequest.Exit) { // 終了要求コマンドで新規受付停止
						incomingCommands.add((CommandRequest)cmd);
						return null;
					} else if(cmd instanceof CommandRequest) {
						incomingCommands.add((CommandRequest)cmd);
					}
				}
			}
		}
	}
	
	/**
	 * コマンドキューを出力ストリームに出力していくスレッド
	 */
	private class OutputPump implements Callable<Void> {
		/**
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Void call() throws Exception {
			Thread.currentThread().setName(name + "CommandStream-OutputPump");
			
			try(ObjectOutputStream oos = new ObjectOutputStream(os)) {
				while(true) {
					Command cmd = outgoingCommands.take();
					oos.writeUnshared(cmd);
					oos.reset();
					oos.flush();
					
					// 終了要求コマンドで新規送出停止
					if(cmd instanceof CommandResponse.Exit) {
						close();
						return null;
					}
				}
			}
		}
	}
}
