package com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAddressing;

/**
 * WSDLOutput的基类,目前只支持了WS-Address
 * @author wx
 */
public class OutputObject <T>
{
	private T bodyObject;
	private StringBuilder soapReturnMessage;
	private WSAddressing addressing;
	public WSAddressing getAddressing() {
		return addressing;
	}
	public void setAddressing(WSAddressing addressing) {
		this.addressing = addressing;
	}
	public T getBodyObject() {
		return bodyObject;
	}
	public void setBodyObject(T bodyObject) {
		this.bodyObject = bodyObject;
	}
	public void setSoapReturnMessage(StringBuilder soapReturnMessage) {
		this.soapReturnMessage = soapReturnMessage;
	}
	public StringBuilder getSoapReturnMessage() {
		return soapReturnMessage;
	}
}
