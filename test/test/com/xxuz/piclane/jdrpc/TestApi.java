package test.com.xxuz.piclane.jdrpc;

import java.sql.SQLException;
import java.util.function.Supplier;

import com.xxuz.piclane.jdrpc.RpcParam;
import com.xxuz.piclane.jdrpc.RpcResult;

/**
 * 
 * 
 * @author yohei_hina
 */
public interface TestApi {
	public int test1_Sum(int a, int b);

	public void test2_Exception() throws SQLException;
	
	public void test3_RuntimeException();
	
	public int test4_Sum(@RpcParam Supplier<Integer> a, @RpcParam Supplier<Integer> b);
	
	@RpcResult
	public Supplier<Integer> test5_Sum(int a, int b);

	public int test6_Sum(Supplier<Integer> a, Supplier<Integer> b);
	
	public Supplier<Integer> test7_Sum(int a, int b);
	
	public void test8_RefCall(@RpcParam byte[] a, @RpcParam int[] b, @RpcParam long[] c, @RpcParam Object[] d);
}
