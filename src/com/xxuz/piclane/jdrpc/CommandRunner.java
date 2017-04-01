package com.xxuz.piclane.jdrpc;

import java.lang.annotation.Annotation;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.xxuz.piclane.jdrpc.CommandResponse.Invoke.ObjectType;
import com.xxuz.piclane.jdrpc.InstanceContainer.Dynamic;

/**
 * 
 * 
 * @author yohei_hina
 */
public class CommandRunner implements AutoCloseable {
	/** 匿名インスタンス名の接頭辞 */
	protected static final String ANONYMOUS_INSTANCE_HEADER = "anonymous-";

	/** {@link ExecutorService} */
	private final ExecutorService es;
	
	/** 名前 */
	private final String name;
	
	/** 登録されたインスタンス */
	protected final ConcurrentHashMap<UUID, InstanceContainer> instances;
	
	/** 登録された名前付きインスタンス */
	protected final ConcurrentHashMap<String, InstanceContainer> namedInstances;
	
	/** {@link CommandStream} */
	protected final CommandStream stream;
	
	/** プロクシクラスの参照キュー */
	private final ReferenceQueue<Object> refQueue;
	
	/** {@link RpcOverride} の集合 */
	private final Set<RpcOverride> overrides;
	
	/** 終了されている場合 true */
	private volatile boolean isClosed = false;
	
	/**
	 * コンストラクタ
	 * 
	 * @param stream {@link CommandStream}
	 */
	public CommandRunner(CommandStream stream) {
		this(null, stream);
	}
	
	/**
	 * コンストラクタ
	 * 
	 * @param name 名前
	 * @param stream {@link CommandStream}
	 */
	public CommandRunner(String name, CommandStream stream) {
		this.es = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
		this.name = name != null && !name.isEmpty() ? name + "-" : "";
		this.instances = new ConcurrentHashMap<>();
		this.namedInstances = new ConcurrentHashMap<>();
		this.stream = stream;
		this.refQueue = new ReferenceQueue<Object>();
		this.overrides = new HashSet<>();
		
		es.submit(new CommandPump());
		es.submit(new ProxyFinalizer());
	}
	
	/**
	 * 名前付きインスタンスを取得します。<br>
	 * このメソッドは名前付きインスタンスが登録されるまでブロックします
	 * 
	 * @param name インスタンス名
	 * @return インスタンス
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public <T> T get(String name) throws InterruptedException {
		validate();
		InstanceContainer named = getInstanceContainer(name);
		return named.getInstance();
	}
	
	/**
	 * インスタンスを取得します。<br>
	 * このメソッドは名前付きインスタンスが登録されるまでブロックします
	 * 
	 * @param instanceId インスタンスID
	 * @return インスタンス
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public <T> T get(UUID instanceId) throws InterruptedException {
		validate();
		InstanceContainer named = getInstanceContainer(instanceId);
		return named.getInstance();
	}
	
	/**
	 * 名前付きインスタンスを取得します。<br>
	 * このメソッドは名前付きインスタンスが登録されていない場合は待機せず <code>null</code> を返します
	 * 
	 * @param name インスタンス名
	 * @return 登録されている場合はインスタンス、登録されていない場合は <code>null</code>
	 */
	public <T> T tryGet(String name) {
		validate();
		InstanceContainer named = tryGetInstanceContainer(name);
		if(named != null) {
			return named.getInstance();
		} else {
			return null;
		}
	}
	
	/**
	 * インスタンスを取得します。<br>
	 * このメソッドはインスタンスが登録されていない場合は待機せず <code>null</code> を返します
	 * 
	 * @param instanceId インスタンスID
	 * @return 登録されている場合はインスタンス、登録されていない場合は <code>null</code>
	 */
	public <T> T tryGet(UUID instanceId) {
		validate();
		InstanceContainer named = instances.get(instanceId);
		if(named != null) {
			return named.getInstance();
		} else {
			return null;
		}
	}
	
	/**
	 * リモートにインスタンスを登録します
	 * 
	 * @param name インスタンス名
	 * @param object 登録するインスタンス
	 * @return インスタンスID
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public UUID register(String name, Object object) throws InterruptedException {
		return register(name, object, object.getClass().getInterfaces());
	}
	
	/**
	 * リモートにインスタンスを登録します
	 * 
	 * @param name インスタンス名
	 * @param object 登録するインスタンス
	 * @param interfaces インスタンスのインターフェイス
	 * @return インスタンスID
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public UUID register(String name, Object object, Class<?>... interfaces) throws InterruptedException {
		validate();
		UUID instanceId = UUID.randomUUID();
		name = putInstance(name, instanceId, interfaces, object);
		stream.call(new CommandRequest.Register(name, instanceId, interfaces));
		return instanceId;
	}
	
	/**
	 * リモートに登録したインスタンスを登録解除します
	 * 
	 * @param name インスタンス名
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public void deregister(String name) throws InterruptedException {
		validate();
		removeInstance(name);
	}
	
	/**
	 * リモートに登録したインスタンスを登録解除します
	 * 
	 * @param instanceId インスタンスID
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public void deregister(UUID instanceId) throws InterruptedException {
		validate();
		removeInstance(instanceId);
	}
	
	public Object invokeStatic(Method method, Object... args) throws InvocationTargetException, InterruptedException {
		return invoke((UUID)null, method, args);
	}
	
	public Object invoke(String name, Method method, Object... args) throws InvocationTargetException, InterruptedException {
		InstanceContainer named = getInstanceContainer(name);
		if(named == null) {
			throw new NullPointerException();
		}
		
		return invoke(named.getId(), method, args);
	}

	public Object invoke(UUID instanceId, Method method, Object... args) throws InvocationTargetException, InterruptedException {
		validate();
		if(instanceId == null && (method.getModifiers() & Modifier.STATIC) == 0) {
			throw new IllegalArgumentException(method + " is not static method");
		}
		
		// 引数のリファレンス化
		int paramCount = method.getParameterCount();
		Object[] reqArgs = new Object[paramCount];
		Class<?>[] paramClasses = method.getParameterTypes();
		Annotation[][] paramAnnos = method.getParameterAnnotations();
		for(int i=0; i<paramCount; i++) {
			Object arg = reqArgs[i] = args[i];
			Class<?> paramCls = paramClasses[i];
			if(arg != null &&
			   hasAnnotation(paramAnnos[i], RpcParam.class) || 
			   overrides.contains(RpcOverride.forMethodParameter(i, method))) {
				if(paramCls.isArray()) {
					reqArgs[i] = new ReferenceArray(i, arg);
				} else {
					if(!paramCls.isInterface()) {
						throw new IllegalArgumentException();
					}
					reqArgs[i] = new Reference(register(null, arg, paramCls));
				}
			}
		}
		
		// 実行
		CommandRequest req = new CommandRequest.Invoke(
			instanceId,
			method.getDeclaringClass(),
			method.getName(),
			method.getParameterTypes(),
			reqArgs);
		CommandResponse.Invoke resp = 
			(CommandResponse.Invoke)stream.call(req);
		Object result = resp.getReturnValue();
		
		// 参照渡しされた引数を戻す
		for(ReferenceArray refParam: resp.getReferenceParams()) {
			refParam.copyInto(args[refParam.getParameterIndex()]);
		}
		
		// 返値のリファレンス解除
		if(result instanceof Reference) {
			Reference ref = (Reference)result;
			if(ref != null) {
				result = tryGet(ref.getInstanceId());
			}
		}
		
		return result;
	}
	
	/**
	 * 終了しているかどうかを取得します
	 * 
	 * @return 終了している場合 true そうでない場合 false
	 */
	public boolean isClosed() {
		return isClosed;
	}
	
	/**
	 * @throws InterruptedException 
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws InterruptedException {
		if(isClosed) {
			return;
		}
		isClosed = true;
		
		stream.call(new CommandRequest.Exit());
		
		es.shutdownNow();
		es.awaitTermination(10L, TimeUnit.SECONDS);
	}
	
	private void closeWithoutCall() throws InterruptedException {
		if(isClosed) {
			return;
		}
		isClosed = true;
		
		es.shutdownNow();
		es.awaitTermination(10L, TimeUnit.SECONDS);
	}
	
	private void validate() {
		if(isClosed) {
			throw new IllegalStateException("CommandRunner has been closed.");
		}
	}
	
	/**
	 * {@link RpcOverride} を追加します
	 * 
	 * @param override 追加する {@link RpcOverride}
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public void addRpcOverride(RpcOverride override) throws InterruptedException {
		stream.call(new CommandRequest.AddRpcOverride(override));
		overrides.add(override);
	}
	
	/**
	 * {@link RpcOverride} を削除します
	 * 
	 * @param override 削除する {@link RpcOverride}
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	public void removeRpcOverride(RpcOverride override) throws InterruptedException {
		stream.call(new CommandRequest.RemoveRpcOverride(override));
		overrides.remove(override);
	}
	
	protected final InstanceContainer getInstanceContainer(String name) throws InterruptedException {
		InstanceContainer named = new InstanceContainer();
		InstanceContainer namedPrev = namedInstances.putIfAbsent(name, named);
		if(namedPrev != null) {
			named = namedPrev;
		}
		if(!named.isSettled()) {
			synchronized (named) {
				if(!named.isSettled()) {
					named.wait();
				}
			}
		}
		return named;
	}
	
	protected final InstanceContainer getInstanceContainer(UUID instanceId) throws InterruptedException {
		InstanceContainer named = new InstanceContainer();
		InstanceContainer namedPrev = instances.putIfAbsent(instanceId, named);
		if(namedPrev != null) {
			named = namedPrev;
		}
		if(!named.isSettled()) {
			synchronized (named) {
				if(!named.isSettled()) {
					named.wait();
				}
			}
		}
		return named;
	}

	protected final InstanceContainer tryGetInstanceContainer(String name) {
		return namedInstances.get(name);
	}
	
	protected final InstanceContainer tryGetInstanceContainer(UUID instanceId) {
		return instances.get(instanceId);
	}
	
	/**
	 * インスタンスを登録解除します
	 * 
	 * @param name インスタンス名
	 * @throws InterruptedException 割込例外が発生した場合
	 */
	protected final void removeInstance(String name) throws InterruptedException {
		InstanceContainer named = namedInstances.get(name);
		if(named == null) {
			return;
		}
		
		UUID instanceId = named.getId();
		if(instanceId == null) {
			return;
		}
		
		removeInstance(instanceId);
	}
	
	protected final void removeInstance(UUID instanceId) throws InterruptedException {
		stream.call(new CommandRequest.Deregister(instanceId));
		
		InstanceContainer container = instances.remove(instanceId);
		if(container != null) {
			namedInstances.remove(container.getName());
		}
	}
	
	/**
	 * インスタンスを登録します
	 * 
	 * @param name インスタンス名、もしくは <code>null</code>
	 * @param instanceId インスタンスID
	 * @param object 登録するインスタンス
	 * @return インスタンス名
	 */
	protected final String putInstance(String name, UUID instanceId, Class<?>[] interfaces, Object object) {
		if(name == null) {
			name = ANONYMOUS_INSTANCE_HEADER + instanceId;
		}
		
		InstanceContainer containerNew = new InstanceContainer(name, instanceId, interfaces, object);
		InstanceContainer containerPrev;
		containerPrev = namedInstances.putIfAbsent(name, containerNew);
		if(containerPrev != null) {
			synchronized (containerPrev) {
				containerPrev.set(name, instanceId, interfaces, object);
				containerPrev.notifyAll();
			}
		}
		containerPrev = instances.putIfAbsent(instanceId, containerNew);
		if(containerPrev != null) {
			synchronized (containerPrev) {
				containerPrev.set(name, instanceId, interfaces, object);
				containerPrev.notifyAll();
			}
		}
		return name;
	}
	
	/**
	 * コマンドの内容によって
	 * 
	 * @param req
	 * @return
	 */
	protected CommandResponse process(CommandRequest req) {
		if(req instanceof CommandRequest.Register) {
			return processRegister((CommandRequest.Register)req);
		} else if(req instanceof CommandRequest.Deregister) {
			return processDeregister((CommandRequest.Deregister)req);
		} else if(req instanceof CommandRequest.Invoke) {
			return processInvoke((CommandRequest.Invoke)req);
		} else if(req instanceof CommandRequest.AddRpcOverride) {
			return processAddRpcOverride((CommandRequest.AddRpcOverride)req);
		} else if(req instanceof CommandRequest.RemoveRpcOverride) {
			return processRemoveRpcOverride((CommandRequest.RemoveRpcOverride)req);
		} else if(req instanceof CommandRequest.Exit) {
			return processExit((CommandRequest.Exit)req);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	protected CommandResponse.Register processRegister(CommandRequest.Register req) {
		Class<?>[] interfaces = req.getInterfaces();
		UUID instanceId = req.getInstanceId();
		String name = req.getName();
		Object object = 
			name.startsWith(ANONYMOUS_INSTANCE_HEADER) ?
				new LazyProxy() :
				newProxy(interfaces, instanceId);
				
		// インスタンスを登録
		putInstance(name, instanceId, interfaces, object);
		
		return new CommandResponse.Register(req.getMessageId());
	}
	
	protected CommandResponse.Deregister processDeregister(CommandRequest.Deregister req) {
		InstanceContainer container = instances.remove(req.getInstanceId());
		if(container != null) {
			namedInstances.remove(container.getName());
		}
		
		return new CommandResponse.Deregister(req.getMessageId());
	}
	
	protected CommandResponse.Invoke processInvoke(CommandRequest.Invoke req) {
		UUID messageId = req.getMessageId();
		UUID instanceId = req.getInstanceId();
		
		try {
			Object[] args = req.getArguments();
			Class<?> cls = req.getDeclaringClass();
			Method method = cls.getDeclaredMethod(req.getMethodName(), req.getParameterTypes());
			
			// 引数のリファレンス解除
			int paramCount = method.getParameterCount();
			int refParamCount = 0;
			Object[] reqArgs = new Object[paramCount];
			for(int i=0; i<paramCount; i++) {
				Object arg = args[i];
				if(arg instanceof Reference) {
					Reference ref = (Reference)arg;
					reqArgs[i] = tryGet(ref.getInstanceId());
				} else if(arg instanceof ReferenceArray) {
					ReferenceArray ref = (ReferenceArray)arg;
					reqArgs[i] = ref.getArray();
					refParamCount++;
				} else {
					reqArgs[i] = arg;
				}
			}
			
			// インスタンス取得
			Object object = null;
			if((method.getModifiers() & Modifier.STATIC) == 0) {
				object = tryGet(instanceId);
				if(object == null) {
					throw new NullPointerException();
				}
			}

			// 実行
			Object result = method.invoke(object, reqArgs);
			
			// 参照渡し引数のリファレンス化
			ReferenceArray[] refParams = new ReferenceArray[refParamCount];
			for(int i=0, ri=0; i<paramCount; i++) {
				Object arg = args[i];
				if(arg instanceof ReferenceArray) {
					ReferenceArray ref = (ReferenceArray)arg;
					refParams[ri++] = new ReferenceArray(ref.getParameterIndex(), reqArgs[i]);
				}
			}
			
			// 返値のリファレンス化
			if(result != null &&
			   method.isAnnotationPresent(RpcResult.class) || 
			   overrides.contains(RpcOverride.forMethod(method))) {
				result = new Reference(register(null, result, method.getReturnType()));
			}
			
			return new CommandResponse.Invoke(messageId, ObjectType.Result, result, refParams);
		} catch (InvocationTargetException e) {
			return new CommandResponse.Invoke(messageId, ObjectType.InvocationException, e.getTargetException());
		} catch (Exception e) {
			return new CommandResponse.Invoke(messageId, ObjectType.InternalError, e);
		}
	}
	
	protected CommandResponse.AddRpcOverride processAddRpcOverride(CommandRequest.AddRpcOverride req) {
		overrides.add(req.getOverride());
		return new CommandResponse.AddRpcOverride(req.getMessageId());
	}
	
	protected CommandResponse.RemoveRpcOverride processRemoveRpcOverride(CommandRequest.RemoveRpcOverride req) {
		overrides.remove(req.getOverride());
		return new CommandResponse.RemoveRpcOverride(req.getMessageId());
	}
	
	protected CommandResponse.Exit processExit(CommandRequest.Exit req) {
		return new CommandResponse.Exit(req.getMessageId());
	}
	
	private boolean hasAnnotation(Annotation[] annos, Class<? extends Annotation> annotationClass) {
		for(Annotation anno: annos) {
			if(annotationClass.isInstance(anno)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * リモートから来たコマンドを実行してレスポンスを返すスレッド
	 */
	private class CommandPump implements Callable<Void> {
		/**
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Void call() throws Exception {
			Thread.currentThread().setName(name + "CommandRunner-CommandPump");
			
			while(true) {
				CommandRequest req = stream.take();
				CommandResponse resp = process(req);
				stream.put(resp);
				
				// 終了要求コマンドで新規送出停止
				if(resp instanceof CommandResponse.Exit) {
					CommandRunner.this.closeWithoutCall();
					return null;
				}
			}
		}
	}
	
	/**
	 * プロクシが GC される事を検知して登録解除するスレッド
	 */
	private class ProxyFinalizer implements Callable<Void> {
		/**
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Void call() throws Exception {
			Thread.currentThread().setName(name + "CommandRunner-ProxyFinalizer");
			
			try {
				while(true) {
					java.lang.ref.Reference<?> r = refQueue.remove();
					if(r instanceof WeakProxyReference) {
						WeakProxyReference wpr = (WeakProxyReference)r;
						removeInstance(wpr.getInstanceId());
					}
				}
			} catch (InterruptedException e) {
				return null;
			}
		}
	}
	
	private class LazyProxy implements Dynamic {
		/** プロクシインスタンスへの弱参照 */
		private WeakProxyReference proxyRef = null;

		/**
		 * @see com.xxuz.piclane.jdrpc.InstanceContainer.Dynamic#get(com.xxuz.piclane.jdrpc.InstanceContainer)
		 */
		@Override
		public Object get(InstanceContainer container) {
			Object proxy;
			WeakProxyReference proxyRef = this.proxyRef;
			if(proxyRef == null) {
				Class<?>[] interfaces = container.getInterfaces();
				UUID instanceId = container.getId();
				proxy = newProxy(interfaces, instanceId);
				proxyRef = this.proxyRef = new WeakProxyReference(instanceId, proxy, refQueue);
			} else {
				proxy = proxyRef.get();
			}
			return proxy;
		}
	}
	
	/**
	 * {@link LazyProxy} がプロクシクラスを保持する為の弱参照
	 */
	private static class WeakProxyReference extends WeakReference<Object> {
		/** インスタンスID */
		private final UUID instanceId;
		
		/**
		 * コンストラクタ
		 * 
		 * @param referent 新しい弱参照が参照するオブジェクト
		 * @param q 参照が登録されるキュー。登録が必要ない場合は <code>null</code>
		 */
		public WeakProxyReference(UUID instanceId, Object referent, ReferenceQueue<? super Object> q) {
			super(referent, q);
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
	
	private Object newProxy(Class<?>[] interfaces, UUID instanceId) {
		return Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(), 
			interfaces,
			new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					try {
						// リモートでメソッド実行
						return CommandRunner.this.invoke(instanceId, method, args);
					} catch (InvocationTargetException e) {
						Class<?> declaringClass = method.getDeclaringClass();
						
						Throwable t = e.getTargetException();
						StackTraceElement[] ts1 = t.getStackTrace();
						StackTraceElement[] ts2 = Thread.currentThread().getStackTrace();
						StackTraceElement[] ts = new StackTraceElement[ts1.length + ts2.length - 1];
						System.arraycopy(ts1, 0, ts, 0, ts1.length);
						ts[ts1.length + 0] = new StackTraceElement(".", "........................ RPC ..........................", null, 0);
						ts[ts1.length + 1] = new StackTraceElement(declaringClass.getName(), method.getName(), null, -1);
						System.arraycopy(ts2, 3, ts, ts1.length + 2, ts2.length - 3);
						t.setStackTrace(ts);
						throw t;
					}
				};
			});
	}
}
