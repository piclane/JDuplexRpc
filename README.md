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
	Process proc = new ProcessBuilder()
		.command("ssh", "-q", "pi@192.168.1.123", "java -jar ~/hoge_server.jar")
		.redirectOutput(Redirect.PIPE)
		.redirectInput(Redirect.PIPE)
		.redirectError(Redirect.INHERIT)
		.start();

	try(DefaultCommandStream stream = new DefaultCommandStream(proc.getInputStream(), proc.getOutputStream());
		CommandRunner runner = new CommandRunner(stream);) {
		Platform platform = runner.get("platform");
		System.out.println(platform.getHoge());
		platform.setHoge("hoge");
	}
}
```