package com.sobey.jcg.sobeyhive.wsinvoker;

import java.lang.reflect.Method;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.SoapCaller;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.OutputObject;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.ABindingType;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAddressing;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;



/**
 * 接口代理调用的工厂类
 * @author wangxi
 * @version 0.1
 * 调用步骤 
 * 1. 根据发布的方法名生成内部或外部接口, 目前版本需要人为指定接口的参数
 * public interface InterFaceName{
 *	 public String DCMGetAttributesofEntity(DCMContentIdentifier conId);
 * }
 * 2.找到WSDL地址, 生成代理
 * String wsdl = "sddfsdfsdf.wsdl";
 * InterFaceName it = (InterFaceName)WebServiceProxyFactory.getInstance() 
 *						.getWebServiceProxy(wsdl, InterFaceName.class);
 * 3.调用
 * String ret = it.DCMGetAttributesofEntity(new DCMContentIdentifier());
 * 
 * 例:查找SystemConstant并更新
 * 		interface TestServiceCtrl{
 * 			public static String wsdl = "http://172.16.1.6:9080/SobeyMAMWEB/services/DCMSystemConstantControl?wsdl";
 * 			public static String wsdl2 = "http://172.16.1.6:9080/SobeyMAMWEB/services/DCMSystem?wsdl";
 *	 		public DCMSystemConstant getSystemConstantById(Long ActorId);
 *			public OtherInWsdl2 otherMethod(Params p);
 *		 }
 * 
 *		TestServiceCtrl dctr = (TestServiceCtrl)WebServiceProxyFactory.getInstance()
 *										.getWebServiceProxy(TestServiceCtrl.wsdl, TestServiceCtrl.class);
 *		DCMSystemConstant con = dctr.getSystemConstantById(new Long(1021));
 *		System.out.println(con.getPropertyName());
 *		System.out.println(con.getPropertyXml());
 *		
 *		con.setPropertyValue("KEYFRAME");
 *		dctr.updateSystemConstant(con);
 * 
 * 要查看WSDL中的方法请用工具MethodList查看
 * */
public class WebServiceProxyFactory
{
	private Enhancer enhancer;
	private String wsdld;
	
	private String user = null;
	private String pass = null;
	
	private WSAddressing wsaddressing;
		
	private WebServiceProxyFactory()
	{	
	}
	
	private void initial(String wsdl, Class clazz)
	{		
		enhancer = new Enhancer();
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(new MethodInterceptorImpl());
		wsdld = wsdl;
	}
	
	public static WebServiceProxyFactory getInstance()
	{
		return new WebServiceProxyFactory();
	}
	
	public WebServiceProxyFactory setWSAddressing(WSAddressing wsaddressing)
	{
		this.wsaddressing = wsaddressing;
		return this;
	}
	
	public Object getWebServiceProxy(String wsdl, Class clazz)
	{
		this.initial(wsdl, clazz);
		return enhancer.create();
	}
	
	private class MethodInterceptorImpl implements MethodInterceptor
	{
		public Object intercept(Object obj, Method method, Object[] args,
				MethodProxy proxy) throws Throwable
		{
			if(wsdld == null)
			{
				throw new Exception("WSDL为空");
			}
			if(!wsdld.toLowerCase().endsWith("?wsdl"))
			{
				wsdld += "?wsdl";
			}
			String methodName = method.getName();
			Class exceptReturnType = method.getReturnType();
			
			Object o = SoapCaller.getInstance(wsdld, ABindingType.SOAP11)
								 .setOperatioName(methodName)
								 .setParams(args)
								 .setExceptReturnType(exceptReturnType)
								 .setWSAddressing(wsaddressing)
								 .invoke();
			
			if(o instanceof OutputObject){
				OutputObject out = (OutputObject)o;
				return out.getBodyObject();
			}else{
				return o;
			}
		}
	}
}


