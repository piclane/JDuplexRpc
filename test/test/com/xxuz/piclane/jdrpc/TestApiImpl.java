package test.com.xxuz.piclane.jdrpc;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * 
 * 
 * @author yohei_hina
 */
public class TestApiImpl implements TestApi {

	/**
	 * @see test.com.xxuz.piclane.jdrpc.TestApi#test1_Sum(int, int)
	 */
	@Override
	public int test1_Sum(int a, int b) {
		return a + b;
	}
	
	/**
	 * @see test.com.xxuz.piclane.jdrpc.TestApi#test2_Exception()
	 */
	@Override
	public void test2_Exception() throws SQLException {
		throw new SQLException("test");
	}

	/**
	 * @see test.com.xxuz.piclane.jdrpc.TestApi#test3_RuntimeException()
	 */
	@Override
	public void test3_RuntimeException() {
		throw new ClassCastException("test");
	}
	
	/**
	 * @see test.com.xxuz.piclane.jdrpc.TestApi#test4_Sum(java.util.function.Supplier, java.util.function.Supplier)
	 */
	@Override
	public int test4_Sum(Supplier<Integer> a, Supplier<Integer> b) {
		return a.get() + b.get();
	}
	
	/**
	 * @see test.com.xxuz.piclane.jdrpc.TestApi#test5_Sum(int, int)
	 */
	@Override
	public Supplier<Integer> test5_Sum(int a, int b) {
		return () -> a + b;
	}
	
	/**
	 * @see test.com.xxuz.piclane.jdrpc.TestApi#test6_Sum(java.util.function.Supplier, java.util.function.Supplier)
	 */
	@Override
	public int test6_Sum(Supplier<Integer> a, Supplier<Integer> b) {
		int ar = a.get();
		int br = b.get();
		return ar + br;
	}
	
	/**
	 * @see test.com.xxuz.piclane.jdrpc.TestApi#test7_Sum(int, int)
	 */
	@Override
	public Supplier<Integer> test7_Sum(int a, int b) {
		return () -> a + b;
	}
}
