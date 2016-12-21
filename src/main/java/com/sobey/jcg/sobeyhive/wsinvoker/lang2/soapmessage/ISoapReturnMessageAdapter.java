package com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage;

import javax.xml.soap.SOAPMessage;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLParam;

/**
 * 用来自定义的处理返回的内容.主要是用于处理AIXS,CXF等框架的自定义返回
 * @author wx
 */
public interface ISoapReturnMessageAdapter<T> {
	public  T handleSOAPResponse(SOAPMessage returnMessage,
			JWSDLParam outputParam, Class<T> clazz) ;
}
