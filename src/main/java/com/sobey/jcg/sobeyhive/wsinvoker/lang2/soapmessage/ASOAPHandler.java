package com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLOperation;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLParam;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLParamType;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLParamTypeChoiceField;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLParamTypeField;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.util.PrimativeTypeUtil;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception.ResponseException;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.util.namespace.Namespace;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.Constant;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.W3CDataTypeConstant;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.ABindingType;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.WSAConstant;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.endpointreference.EndpointReference;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.endpointreference.ReferenceProperty;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAAction;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAMessageID;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSARelatesTo;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAReplyTo;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSATo;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAddressing;

public abstract class ASOAPHandler 
{
    protected static Logger logger = LoggerFactory.getLogger(ASOAPHandler.class);

	/*由于ASOAPHandler是单例(@see SOAPHandlerFactory),所以这里使用ThreadLocal来保存变量*/
	private static ThreadLocal<WSAddressing> addressingLocal = new ThreadLocal<WSAddressing>();
	
	public ASOAPHandler addWSAddressing(WSAddressing addressing){
		addressingLocal.set(addressing);
		return this;
	}
	
	private static String[] sunSaajClasses = new String[]{
		"com.sun.xml.messaging.saaj.soap.ver1_2.SOAPMessageFactory1_2Impl",
		"com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl",
		"com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl"
	};
	private static String[] sunclasses = new String[]{
		"com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"
	};
	private static boolean validateRuntimeEnv(String[] classNames) throws ClassNotFoundException{
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try{
			for(int i=0; i<classNames.length; i++){		
				try{
					Class.forName(classNames[i]);
					return true;
				}catch (ClassNotFoundException e) {
					if(classLoader!=null){
						classLoader.loadClass(classNames[i]);
						return true;
					}
				}
			}
			return false;
		}catch(ClassNotFoundException e){
			return false;
		}
	}
	
	public SOAPMessage createSoapMessage(JWSDLOperation operation, Namespace[] namespaces) throws Exception
	{
		/*开始调用SAAJ-API进行调用,这里引用的是sun saaj 1.3,当然apache,cxf,oralcews都有对应实现*/
		/*SOAP12的创建方式 MessageFactory mf12 = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);*/
	
		/*2011-05-09 对于Transformer的获取,要么采用标准JDK1.5中的com.sun.org.apche.xalan...
		 * 要么采用xalan中的org.apache.xalan...,应对非sun标准JDK1.5的情况,比如IBMJDK*/
		WSAddressing addressing =null;
		try {
			boolean isSunTransformer = validateRuntimeEnv(sunclasses);
			if(isSunTransformer){
				System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
			}else{
				logger.warn("SAAJ TRANSFORMER CAN NOT FIND \"com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl\"" +
				", Try to use Xalan");
				System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
			}
			
			MessageFactory factory = null;
			/**
			 * 2011-05-09 wx 改为:默认按sun实现,如果sun找不到,则采用容器或其他环境提供的saaj处理
			 */
			boolean sunSaajAvliable = validateRuntimeEnv(sunSaajClasses);	
			if(sunSaajAvliable){
				String protocol = SOAPConstants.SOAP_1_1_PROTOCOL;
				if(operation.getBindingType().equals(ABindingType.SOAP12)){
					protocol = SOAPConstants.SOAP_1_2_PROTOCOL;
					System.setProperty("javax.xml.soap.MessageFactory",    
						    "com.sun.xml.messaging.saaj.soap.ver1_2.SOAPMessageFactory1_2Impl");
				}else{
					System.setProperty("javax.xml.soap.MessageFactory",    
							"com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl");  
				}
				System.setProperty("javax.xml.soap.MetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl");
				factory = MessageFactory.newInstance(protocol);
			}else{
				factory = MessageFactory.newInstance();
				logger.warn("SAAJ ATTEMP TO USE SUN-IMPL, BUT CAN NOT LOAD \"com.snu.xml.messaging...\"");
			}
			
			SOAPMessage message = factory.createMessage();
			SOAPPart part = message.getSOAPPart();
			SOAPEnvelope envelope = part.getEnvelope();
			/*将命名空间全部加到SOAPEnvelope节点中*/
			for(int i=0; i<namespaces.length; i++){
				String namespaceURI = namespaces[i].getUri();
				String namespacePrefix = namespaces[i].getPrefix();
				envelope.addNamespaceDeclaration(namespacePrefix, namespaceURI);
			}
			/*设置WSA信息*/
			addressing = addressingLocal.get();
			if(addressing!=null){
				this.createWSAddressingInfo(envelope, addressing);
			}
			/*设置Body中的内容*/
			this.createSoapBodyContent(envelope, operation, namespaces);

			message.saveChanges();
			return message;
		}finally{
			addressing = null;
			addressingLocal.remove(); //清除缓存,让GC处理内存
		}
		
	}
	
	/**
	 * 设置ws-addressing的内容
	 * @param envelope
	 * @param addressing
	 * @throws Exception
	 */
	private void createWSAddressingInfo(SOAPEnvelope envelope, WSAddressing addressing) throws Exception
	{
		SOAPHeader header = envelope.getHeader();
		WSAMessageID messageID = addressing.getMessageID();
		envelope.addNamespaceDeclaration("wsa", WSAConstant.namespace);
		if(messageID!=null&&messageID.getContent()!=null){
			Name name = envelope.createName(WSAConstant.WSA_MESSAGEID, "wsa", WSAConstant.namespace);
			SOAPHeaderElement messageIdElement = header.addHeaderElement(name);
			messageIdElement.setValue(messageID.getContent());
		}
		
		WSARelatesTo relatesTo = addressing.getRelatesTo();
		if(relatesTo!=null&&relatesTo.getContent()!=null){
			Name name = envelope.createName(WSAConstant.WSA_RELATESTO, "wsa", WSAConstant.namespace);
			SOAPHeaderElement messageIdElement = header.addHeaderElement(name);
			messageIdElement.setValue(relatesTo.getContent());
		}
		
		WSAAction action = addressing.getAction();
		if(action!=null&&action.getContent()!=null){
			Name name = envelope.createName(WSAConstant.WSA_ACTION, "wsa", WSAConstant.namespace);
			SOAPHeaderElement actionElement = header.addHeaderElement(name);
			actionElement.setValue(action.getContent());
		}
		
		WSAReplyTo replyTo = addressing.getReplyTo();
		if(replyTo!=null&&replyTo.getAddress()!=null&&replyTo.getAddress().getContent()!=null){
			Name name = envelope.createName(WSAConstant.WSA_REPLYTO, "wsa", WSAConstant.namespace);
			SOAPHeaderElement replyToElement = header.addHeaderElement(name);
			name = envelope.createName(WSAConstant.WSA_ADDRESS, "wsa", WSAConstant.namespace);
			replyToElement.addChildElement(name).setValue(replyTo.getAddress().getContent());
		}

		WSATo to = addressing.getTo();
		if(to!=null&&to.getContent()!=null){
			Name name = envelope.createName(WSAConstant.WSA_TO, "wsa", WSAConstant.namespace);
			SOAPHeaderElement toElement = header.addHeaderElement(name);
			toElement.setValue(to.getContent());
		}
		
		EndpointReference endpointReference = addressing.getEndpointReference();
		if(endpointReference!=null){
			envelope.addNamespaceDeclaration("sh", endpointReference.getNamespace());
			Iterator itr = endpointReference.iterator();
			while(itr.hasNext()){
				ReferenceProperty rp = (ReferenceProperty)itr.next();
				Name name = envelope.createName(rp.getKey(), "sh", endpointReference.getNamespace());
				SOAPHeaderElement element = header.addHeaderElement(name);
				element.setValue(rp.getValue());
			}
		}
	}
	
	/**
	 * 构造SOAPBody
	 * @param operation
	 * @throws Exception
	 */
	protected abstract void createSoapBodyContent(SOAPEnvelope envelope, JWSDLOperation operation, Namespace[] namespaces) throws Exception;

    /**
     * 处理返回结果
     * @param returnMessage
     * @param outputParam
     * @param clazz
     * @return
     * @throws ResponseException
     */
	public <T> T handleSOAPResponse(SOAPMessage returnMessage,
			JWSDLParam outputParam, Class<T> clazz) throws ResponseException
	{
		try{
			SOAPBody body = returnMessage.getSOAPBody();
			T o = this.processReturnMessageBody(body, outputParam, clazz);
			return o;
		}catch(Exception e){
			throw new ResponseException(e);
		}
	}
	
	/**
	 * 处理返回的Body中的内容
	 * @param body
	 * @param outputParam
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	protected abstract <T> T processReturnMessageBody(SOAPBody body, JWSDLParam outputParam, Class<T> clazz) throws ResponseException;
	
	/**
	 * 为复杂类型绑定数据到SOAP中
	 * @param envelope 用来产生节点用,不参与逻辑的
	 * @param element 复杂类型的节点名
	 * @param elName 生成的节点
	 * @param jwsdlType 复杂类型难过
	 * @param value 复杂类型对应的Object值
	 * @param namespaceMap 命名空间集合
	 * @param bindSchemaType 是否绑定参数的类型描述
	 */
	protected void processComplexType(SOAPEnvelope envelope, Map namespaceMap, SOAPElement element,
			QName elName, JWSDLParamType jwsdlType, Object value, boolean bindSchemaType) throws Exception
	{
		if(value==null){
			return;
		}
		
		if(jwsdlType.isArray()){
			if(logger.isDebugEnabled()) logger.debug(new StringBuilder("绑定数组类型数值: 本地数据- ")
			.append(value).append("  wsdl - ").append(jwsdlType).toString());
            if(jwsdlType.isComplexTypeArray()){
                //wx @see SoapHelper.isParamTypeMatch
                String local = elName.getLocalPart();
                String uri = elName.getNamespaceURI();
                String prefix = (String)namespaceMap.get(uri);
                Name soapName = envelope.createName(local, prefix, uri);
                element = element.addChildElement(soapName);
                elName = jwsdlType.getComplexTypeArrayFieldElementName();
            }

			JWSDLParamType arrayElementType = jwsdlType.getArrayElementType(); //WSDL中的数组元素的类型
			if(value!=null&&value.getClass().isArray()){
				Object[] arrayValues = (Object[])value; //将数组的每个值取出来
				for(int i=0; i<arrayValues.length; i++){
					processComplexType(envelope, namespaceMap, element, elName,
                        arrayElementType, arrayValues[i], bindSchemaType);
				}
			}
			
		}else{
			String local = elName.getLocalPart();
			String uri = elName.getNamespaceURI();
			String prefix = (String)namespaceMap.get(uri);
			Name soapName = envelope.createName(local, prefix, uri);
            SOAPElement paramlement = element.addChildElement(soapName);
			
			if(jwsdlType.getQname()!=null&&jwsdlType.getQname().getLocalPart().equalsIgnoreCase("anyType")){
				if(value instanceof Element){
					Node node = paramlement.getOwnerDocument().importNode((Element)value, true);
					paramlement.appendChild(node);
				}else{
					paramlement.setValue(value.toString());
				}
			}else if(jwsdlType.isPrimative()){
				/*绑定普通类型*/
				if(logger.isDebugEnabled()) logger.debug(new StringBuilder("绑定基本类型数值: 本地数据- ")
				.append(value).append("  wsdl - ").append(jwsdlType).toString());
				if(value!=null&&PrimativeTypeUtil.isMapping(jwsdlType, value.getClass())){
					String valueString = value.toString();
					if(value instanceof Date
							&&PrimativeTypeUtil.isMapping(jwsdlType, Date.class)){
						String dateString = PrimativeTypeUtil.dateFormatMapping(jwsdlType);
						valueString = new SimpleDateFormat(dateString).format((Date)value);
					}
					paramlement.setValue(valueString);
				}		
			}else{
				if(value!=null){
					JWSDLParamTypeField[] jwsdlFields = jwsdlType.getDeclaredFields();//复杂类型的字段
					Field[] apiFields = value.getClass().getDeclaredFields();//绑定的对象的字段
					if(jwsdlFields!=null&&apiFields!=null){
						List alreadyBuild = new ArrayList();

                        List<Field> apiFieldList = new ArrayList<Field>(Arrays.asList(apiFields));
						for(int i=0; i<jwsdlFields.length; i++){	
							JWSDLParamTypeField temp = jwsdlFields[i];
							if(!alreadyBuild.contains(temp)){

                                if(temp.getType().getJavaType().equals(Constant.Choice.class)){
                                    JWSDLParamTypeChoiceField choiceField = (JWSDLParamTypeChoiceField)temp;
                                    List<JWSDLParamTypeField> choiceList = choiceField.getChoices();
                                    for(JWSDLParamTypeField jwsdlParamTypeField : choiceList){
                                        for(Iterator<Field> apiFieldItr = apiFieldList.iterator();
                                            apiFieldItr.hasNext();){
                                            Field apifield = apiFieldItr.next();
                                            apifield.setAccessible(true);
                                            Object fieldCallValue = apifield.get(value);
                                            QName jwsdlFieldName = jwsdlParamTypeField.getQname();
                                            String apiName = apifield.getName();

                                            if (jwsdlFieldName.getLocalPart().equalsIgnoreCase(apiName)) {
                                                processComplexType(envelope, namespaceMap, paramlement, jwsdlParamTypeField.getQname(),
                                                    jwsdlParamTypeField.getType(), fieldCallValue, bindSchemaType);
                                                apiFieldItr.remove();
                                                break;
                                            }
                                        }
                                    }
                                }else{
                                    for(Iterator<Field> apiFieldItr = apiFieldList.iterator();
                                        apiFieldItr.hasNext();){
                                        Field apifield = apiFieldItr.next();
                                        apifield.setAccessible(true);
                                        Object fieldCallValue = apifield.get(value);
                                        QName jwsdlFieldName = temp.getQname();
                                        String apiName = apifield.getName();

                                        if (jwsdlFieldName.getLocalPart().equalsIgnoreCase(apiName)) {
                                            processComplexType(envelope, namespaceMap, paramlement, temp.getQname(),
                                                temp.getType(), fieldCallValue, bindSchemaType);
                                            apiFieldItr.remove();
                                            break;
                                        }
                                    }
                                }
								alreadyBuild.add(temp);
							}
						}
					}
				}
			}
			
			/*如果需要绑定类型,则绑定WSDL中的类型*/
			if(bindSchemaType){
				QName typeName = jwsdlType.getQname();
				if(typeName!=null){
					String typeLocalPart = typeName.getLocalPart();
					String typeURI = typeName.getNamespaceURI();
					String typePrefix = (String)namespaceMap.get(typeURI);
					String typeValueInSOAP = typePrefix+":"+typeLocalPart;
					
					/*http://www.w3.org/2001/XMLSchema-instance*/
					String XSIPrefix = (String)namespaceMap.get(W3CDataTypeConstant.SCHEMA_INTANCE);
					Name soapAttrName = envelope.createName("type", XSIPrefix, W3CDataTypeConstant.SCHEMA_INTANCE);
					paramlement.addAttribute(soapAttrName, typeValueInSOAP);
				}
			}
		}	
	}
	
	/**
	 * 从一个Element中获取一个对象类型数据.用来处理SOAP的返回
	 * @param type
	 * @param clazz
	 * @param w3cElement
	 * @return
	 * @throws Exception
	 */
	protected Object treasComplexObject(JWSDLParamType type, Class clazz, Element w3cElement) throws Exception
	{
		if(w3cElement==null||clazz==null){
			if(logger.isDebugEnabled()) logger.debug(new StringBuilder(type.getQname().toString())
			.append("(").append(clazz).append(")").append(" has no SOAPElement to transform").toString());
			return null;
		}
		
		if(type.isPrimative()){
			/*普通类型,直接构造出结果返回*/
			String value = w3cElement.getTextContent();
			return PrimativeTypeUtil.createPrimativeObject(clazz, value);
		}
		else if(type.getQname().getLocalPart().equalsIgnoreCase("anyType")){
			return this.treasAnyType(w3cElement);
		}else{
			/*对象类型*/
			Object returnObject = clazz.newInstance();
			JWSDLParamTypeField[] jwsdlFields = type.getDeclaredFields();

            List<Field> classFields = new ArrayList<Field>(Arrays.asList(clazz.getDeclaredFields()));
			/*设置对象属性的值*/
			for(int i=0; i<jwsdlFields.length; i++){
				QName jwsdlFieldQName = jwsdlFields[i].getQname();//取得WSDL对象字段描述
                if(jwsdlFields[i].getType().getJavaType().equals(Constant.Choice.class)){
                    JWSDLParamTypeChoiceField choiceField = (JWSDLParamTypeChoiceField)jwsdlFields[i];
                    List<JWSDLParamTypeField> choices = choiceField.getChoices();
                    for(JWSDLParamTypeField choice : choices) {
                        for(Iterator<Field> itr = classFields.iterator();itr.hasNext();) {
                            Field apiField = itr.next();
                            if(choice.getQname().getLocalPart().equalsIgnoreCase(apiField.getName())) {
                                Object fieldValue = null;

                                NodeList nodeList = w3cElement.getElementsByTagNameNS(choice.getQname().getNamespaceURI(),
                                    choice.getQname().getLocalPart());
                                if (nodeList == null || nodeList.getLength() < 1) {
                                    nodeList = w3cElement.getElementsByTagName(choice.getQname().getLocalPart());
                                }
                                if (nodeList != null && nodeList.getLength() > 0) {
                                    List<Element> elements = new ArrayList<Element>();
                                    for(int m=0; m<nodeList.getLength(); m++){
                                        Element element = (Element)nodeList.item(m);
                                        if(element.getParentNode().isSameNode(w3cElement)){
                                            elements.add(element);
                                        }
                                    }
                                    if(elements.size()>0) {
                                        if (choice.getType().isArray()) {
                                            fieldValue = treasArrayType(choice.getType(),
                                                apiField.getType(), elements);
                                        } else {
                                            fieldValue = treasComplexObject(choice.getType(),
                                                apiField.getType(), elements.get(0));
                                        }
                                    }
                                }
                                apiField.setAccessible(true);
                                apiField.set(returnObject, fieldValue);
                                itr.remove();
                                break;
                            }
                        }
                    }
                }else {
                    for(Iterator<Field> itr = classFields.iterator();itr.hasNext();) {
                        Field apiField = itr.next();
                        if (logger.isDebugEnabled()) logger.debug(
                            new StringBuilder("处理返回结果, 字段:").append(apiField.getName()).toString()
                        );

                        if(jwsdlFieldQName.getLocalPart().equalsIgnoreCase(apiField.getName())) {
                            Object fieldValue = null;

                            NodeList nodeList = w3cElement.getElementsByTagNameNS(jwsdlFieldQName.getNamespaceURI(),
                                jwsdlFieldQName.getLocalPart());
                            if (nodeList == null || nodeList.getLength() < 1) {
                                nodeList = w3cElement.getElementsByTagName(jwsdlFieldQName.getLocalPart());
                            }
                            if (nodeList != null && nodeList.getLength() > 0) {
                                List<Element> elements = new ArrayList<Element>();
                                for(int m=0; m<nodeList.getLength(); m++){
                                    Element element = (Element)nodeList.item(m);
                                    if(element.getParentNode().isSameNode(w3cElement)){
                                        elements.add(element);
                                    }
                                }
                                if(elements.size()>0) {
                                    if (jwsdlFields[i].getType().isArray()) {
                                        fieldValue = treasArrayType(jwsdlFields[i].getType(),
                                            apiField.getType(), elements);
                                    } else {
                                        fieldValue = treasComplexObject(jwsdlFields[i].getType(),
                                            apiField.getType(), elements.get(0));
                                    }
                                }
                            }
                            apiField.setAccessible(true);
                            apiField.set(returnObject, fieldValue);
                            itr.remove();
                            break;
                        }
                    }
                }
			}
			return returnObject;
		}
	}
	
	protected Object treasArrayType(JWSDLParamType arrayType,  
			Class arrayClass, List<Element> arrayNodeList) throws Exception{
		JWSDLParamType arrayElementType = arrayType.getArrayElementType();
		if(arrayElementType==null){
			throw new Exception("WSDL元素类型"+arrayType+"是数组,但没有数组元素的类型描述");
		}
		Class elementClass = arrayClass.getComponentType();
		Object returnObject = Array.newInstance(elementClass, arrayNodeList.size());
		for(int i=0; i<arrayNodeList.size(); i++){
			Element node = arrayNodeList.get(i); //每个数组对象
			Object fieldValue = treasComplexObject(arrayElementType, elementClass, node);
			Array.set(returnObject, i, fieldValue);
		}
		return returnObject;
	}
	
	private AnyTypeObject treasAnyType(Element w3cElement)
	{
		AnyTypeObject anyType = new AnyTypeObject();
		anyType.setName(w3cElement.getNodeName());
		if(w3cElement.hasChildNodes()){
			NodeList childs = w3cElement.getChildNodes();
			AnyTypeObject[] value = new AnyTypeObject[childs.getLength()];
			for(int i=0; i<childs.getLength(); i++){
				Element childElement = (Element)childs.item(i);
				value[i] = treasAnyType(childElement);
			}
			anyType.setChild(value);
		}else{	
			anyType.setValue(w3cElement.getTextContent());
		}
		return anyType;
	}
}
