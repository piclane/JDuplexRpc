# Duplex RPC

Modern full-duplex RPC for Java

# Usage

## server

```java
public static void main(String[] args) throws Exception {
	try(DefaultCommandStream stream = new DefaultCommandStream(System.in, System.out);
		CommandRunner runner = new CommandRunner(stream);) {
		runner.register("platform", new PlatformImpl(), Platform.class);
		stream.join();
	}
}
```

## client

```java
public static void main(String[] args) throws Exception {
	try(DefaultCommandStream stream = new DefaultCommandStream(System.in, System.out);
		CommandRunner runner = new CommandRunner(stream);) {
		Platform platform = runner.get("platform");
		System.out.println(platform.getHoge());
		platform.setHoge("hoge");
	}
}
```