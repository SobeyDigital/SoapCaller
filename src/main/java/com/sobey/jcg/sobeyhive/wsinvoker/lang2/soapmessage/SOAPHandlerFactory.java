package com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage;

import java.util.HashMap;
import java.util.Map;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.soap11.SOAP11DocumentLiteralSOAPHandler;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.soap11.SOAP11RPCEncodeSOAPHanlder;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.soap11.SOAP11RPCLiteralSOAPHandler;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.ABindingType;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.feature.BindingFeature;

public class SOAPHandlerFactory 
{
	private static Map cache = new HashMap();
	public static ASOAPHandler getHanlder(ABindingType soapType, BindingFeature feature)
	{
		if(cache==null||cache.size()<1){
			/*SOAP1.1*/
			cache.put(ABindingType.SOAP11+""+BindingFeature.DOCUMENT_LITERAL, new SOAP11DocumentLiteralSOAPHandler());
			cache.put(ABindingType.SOAP11+""+BindingFeature.RPC_ENCODED, new SOAP11RPCEncodeSOAPHanlder());
			cache.put(ABindingType.SOAP11+""+BindingFeature.RPC_LITERAL, new SOAP11RPCLiteralSOAPHandler());
			/*SOAP1.2*/
			cache.put(ABindingType.SOAP12+""+BindingFeature.DOCUMENT_LITERAL, new SOAP11DocumentLiteralSOAPHandler());
						cache.put(ABindingType.SOAP12+""+BindingFeature.RPC_ENCODED, new SOAP11RPCEncodeSOAPHanlder());
			cache.put(ABindingType.SOAP12+""+BindingFeature.RPC_LITERAL, new SOAP11RPCLiteralSOAPHandler());
		}
		
		return (ASOAPHandler)cache.get(soapType+""+feature);
	}
}
