package com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.util;

import java.io.ByteArrayOutputStream;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

public class SoapMessageUtil
{
	public static String getSoapMessageString(SOAPMessage message)
	{
        try(
            ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ) {
            Source source = message.getSOAPPart().getContent();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StreamResult streamResult = new StreamResult(outStream);
            transformer.transform(source, streamResult);
            String returnValue = outStream.toString("utf-8");
            return returnValue;
        }catch (Exception e) {
            throw new IllegalStateException(e);
        }
	}
}
