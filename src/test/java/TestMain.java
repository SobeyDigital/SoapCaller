import java.io.ByteArrayOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.w3c.dom.Document;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.SoapCaller;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLParam;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.ISoapReturnMessageAdapter;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.OutputObject;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.endpointreference.EndpointReference;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAAddress;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAMessageID;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAReplyTo;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAddressing;
import com.sobey.jcg.sobeyhive.wsinvoker.viewer.MethodList;

/**
 * Created by WX on 2015/10/10.
 */
public class TestMain {
    public static void main(String[] args) throws Exception {
        WSAddressing wsAddressing = new WSAddressing();
        wsAddressing.setMessageID(new WSAMessageID("dfasdfad34234df"));
        wsAddressing.setReplyTo(new WSAReplyTo(new WSAAddress("http://111.111.111.111")));
        EndpointReference endpointReference = new EndpointReference("http://hive.sobey.com");
        endpointReference.addProperty("ReplyMethod", "noMethod");
        endpointReference.addProperty("ListenOn", "11111");
        wsAddressing.setEndpointReference(endpointReference);
       OutputObject outputObject =  SoapCaller.getInstance("http://172.16.163.5:8989/?wsdl")
                .setOperatioName("actorcommit")
                .setParams(new Object[]{"12312123123123","大法师打发士大夫"})
               .setExceptReturnType(String.class)
               .setWSAddressing(wsAddressing)
               
                .invoke();

        System.out.println(outputObject.getSoapReturnMessage());

        outputObject.getAddressing();
        System.out.println(outputObject.getBodyObject());

//        SoapCaller.getLowlevelInstance("http://172.16.163.5:8088/?wsdl")
//                .setOperatioName("mpc-commit")
//                .setParams(getSOAPElementFromBindingObject())
//                .invoke();
//        invokeHTTP();
        
    }

    /*
    * <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
	<SOAP-ENV:Body>
		<m:actorcommit xmlns:m="urn:actor" SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
			<strInput xsi:type="xsd:string">String</strInput>
			<strReserve xsi:type="xsd:string">String</strReserve>
		</m:actorcommit>
	</SOAP-ENV:Body>
</SOAP-ENV:Envelope>
*/

    /**
     * 使用JAXB 把对象转换成SOAPElement
     *
     * @param
     * @return
     */
    public static SOAPElement getSOAPElementFromBindingObject() {
        SOAPElement soapElem = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            MessageFactory mf = MessageFactory.newInstance();
            SOAPMessage msg = mf.createMessage();
            SOAPBody body = msg.getSOAPBody();

            SOAPBodyElement soapBodyElement = body.addBodyElement(new QName("actorcommit", "m"));
            soapBodyElement.addNamespaceDeclaration("m", "urn:actor");

            SOAPElement soapElement = soapBodyElement.addChildElement(new QName("strInput"));
            soapElement.addAttribute(new QName("type", "xsi"), "xsd:string");
            soapElement.addTextNode("String");

            soapElement = soapBodyElement.addChildElement(new QName("strReserve"));
            soapElement.addAttribute(new QName("type", "xsi"), "xsd:string");
            soapElement.addTextNode("String");

            soapElem = soapBodyElement;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return soapElem;
    }

    /**
     * 使用JAXB 把对象转换成SOAPElement
     *
     * @param obj
     * @return
     */
    public static SOAPElement getSOAPElementFromBindingObject(Object obj) {
        SOAPElement soapElem = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());

            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(obj, doc);

            MessageFactory mf = MessageFactory.newInstance();
            SOAPMessage msg = mf.createMessage();
            SOAPBody body = msg.getSOAPBody();
            soapElem = body.addDocument(doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return soapElem;
    }

    public static void invokeHTTP() throws Exception {
        System.setProperty("sun.net.client.defaultReadTimeout", "20000");
        // 获取SOAP连接工厂  
        SOAPConnectionFactory factory = SOAPConnectionFactory.newInstance();

        // 从SOAP连接工厂创建SOAP连接对象  
        SOAPConnection connection = factory.createConnection();

        // 获取消息工厂  
        MessageFactory mFactory = MessageFactory.newInstance();
        // 从消息工厂创建SOAP消息对象  
        SOAPMessage message = mFactory.createMessage();
        message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");

        // 创建SOAPPart对象  
        SOAPPart part = message.getSOAPPart();
        // 创建SOAP信封对象  
        SOAPEnvelope envelope = part.getEnvelope();

        envelope.addNamespaceDeclaration("SOAP-ENC", "http://schemas.xmlsoap.org/soap/encoding/");
        envelope.addNamespaceDeclaration("SOAP-ENV", "http://schemas.xmlsoap.org/soap/envelope/");
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");

        // 创建SOAPBody对象  
        SOAPBody body = envelope.getBody();

        // 创建XML的根元素  
        SOAPBodyElement bodyElementRoot = body.addBodyElement(new QName("urn:actor", "actorcommit", "m"));

        SOAPElement element = bodyElementRoot.addChildElement("strInput");
        element.addTextNode("测试测试");

        element = bodyElementRoot.addChildElement("strReserve");
        element.addTextNode("测试测试2");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        System.out.println(new StringBuilder("生成的SOAP请求:").append(baos.toString("utf-8")).toString());

        SOAPMessage reMessage = connection.call(message, "http://172.16.163.5:8989");

        if (reMessage==null) {
            System.out.println("调用返回的响应为空");
        }else {
            ByteArrayOutputStream baoouts = new ByteArrayOutputStream();
            reMessage.writeTo(baoouts);
            System.out.println(new StringBuilder("调用返回的响应:").append(baoouts.toString("utf-8")).toString());
        }
        
    }
}
