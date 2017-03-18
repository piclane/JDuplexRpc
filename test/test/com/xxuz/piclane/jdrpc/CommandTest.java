package test.com.xxuz.piclane.jdrpc;

import static org.junit.Assert.*;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xxuz.piclane.jdrpc.CommandRunner;
import com.xxuz.piclane.jdrpc.DefaultCommandStream;
import com.xxuz.piclane.jdrpc.RpcOverride;

/**
 * 
 * 
 * @author yohei_hina
 */
public class CommandTest {
	private static final ExecutorService es = Executors.newCachedThreadPool();

	private CommandRunner runner1;
	private DefaultCommandStream stream1;
	
	private CommandRunner runner2;
	private DefaultCommandStream stream2;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		PipedInputStream is1to2 = new PipedInputStream();
		PipedOutputStream os1to2 = new PipedOutputStream(is1to2);
		
		PipedInputStream is2to1 = new PipedInputStream();
		PipedOutputStream os2to1 = new PipedOutputStream(is2to1);
		
		this.stream1 = new DefaultCommandStream("1", is2to1, os1to2);
		this.runner1 = new CommandRunner("1", stream1);
		
		this.stream2 = new DefaultCommandStream("2", is1to2, os2to1);
		this.runner2 = new CommandRunner("2", stream2);
		
		es.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				Thread.sleep(200);
				runner1.register("api", new TestApiImpl(), TestApi.class);
				return null;
			}
		});
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		runner1.close();
		runner2.close();
		stream1.close();
		stream2.close();
		System.gc();
	}

	@Test
	public void test1() throws Exception {
		TestApi api = runner2.get("api");
		assertEquals(3, api.test1_Sum(1, 2));
	}
	
	@Test(expected=SQLException.class)
	public void test2() throws Exception {
		TestApi api = runner2.get("api");
		api.test2_Exception();
	}
	
	@Test(expected=ClassCastException.class)
	public void test3() throws Exception {
		TestApi api = runner2.get("api");
		api.test3_RuntimeException();
	}
	
	@Test
	public void test4() throws Exception {
		TestApi api = runner2.get("api");
		assertEquals(3, api.test4_Sum(()->1, ()->2));
	}
	
	@Test
	public void test5() throws Exception {
		TestApi api = runner2.get("api");
		Supplier<Integer> s = api.test5_Sum(1, 2);
		assertEquals(3, s.get().intValue());
	}

	@Test
	public void test6() throws Exception {
		TestApi api = runner2.get("api");
		runner2.addRpcOverride(RpcOverride.forMethodParameter(0, TestApi.class, "test6_Sum", Supplier.class, Supplier.class));
		runner2.addRpcOverride(RpcOverride.forMethodParameter(1, TestApi.class, "test6_Sum", Supplier.class, Supplier.class));
		
		assertEquals(3, api.test6_Sum(()->1, ()->2));
	}
	
	@Test
	public void test7() throws Exception {
		TestApi api = runner2.get("api");
		runner2.addRpcOverride(RpcOverride.forMethod(TestApi.class, "test7_Sum", int.class, int.class));
		
		Supplier<Integer> s = api.test7_Sum(1, 2);
		assertEquals(3, s.get().intValue());
	}
	
	@Test
	public void test8() throws Exception {
		runner2.close();
		
		assertTrue(runner1.isClosed());
	}
}
