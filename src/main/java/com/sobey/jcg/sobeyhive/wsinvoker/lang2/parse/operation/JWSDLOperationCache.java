package com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.operation;

import java.util.HashMap;
import java.util.Map;
import javax.wsdl.Definition;

public class JWSDLOperationCache 
{
	private static Map cache = new HashMap();
	
	public static Definition getCachedOperation(String wsdl)
	{
		return (Definition)cache.get(wsdl);
	}
	
	public static void setToCache(String wsdl, Definition defintion)
	{
		cache.put(wsdl, defintion);
	}
	
	public static boolean containsWSDL(String wsdl)
	{
		return cache.containsKey(wsdl);
	}
}
