package com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception.WSDLParseException;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.operation.AOperationParser;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.operation.JWSDLSingletonOperationParserFactory;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLOperation;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.W3CDataTypeConstant;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.ABindingType;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.feature.BindingFeature;

public class WSDLParser
{
    private static Logger logger = LoggerFactory.getLogger(WSDLParser.class);
    private String wsdl;

    public JWSDLOperation[] parse(String wsdl,int timeout){
        return parse(wsdl, SmartType,timeout);
    }

	public JWSDLOperation[] parse(String wsdl, ABindingType type,int timeout)
	{
        this.wsdl = wsdl;
		if(null==type){
			throw new IllegalArgumentException("type不能为空");
		}

        Definition definition;

        URLConnection connection = null;
        try {
            URL url = new URL(wsdl);
            connection = url.openConnection();
            connection.setConnectTimeout(timeout/2);
            connection.setReadTimeout(timeout);
            try(
                java.io.InputStream stream = connection.getInputStream()
            ){
                org.xml.sax.InputSource is = new org.xml.sax.InputSource(stream);
                WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
                reader.setFeature("javax.wsdl.verbose", true);
                reader.setFeature("javax.wsdl.importDocuments", true);
                definition = reader.readWSDL(null, is);
            }catch (IOException|WSDLException ex){
                throw new WSDLParseException(ex);
            }
        }catch(MalformedURLException e){
            File file = new File(wsdl);
            try(
                java.io.InputStream stream = new FileInputStream(file)
            ){
                org.xml.sax.InputSource is = new org.xml.sax.InputSource(stream);
                WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
                reader.setFeature("javax.wsdl.verbose", true);
                reader.setFeature("javax.wsdl.importDocuments", true);
                definition = reader.readWSDL(null, is);
            }catch (IOException|WSDLException ex){
                throw new WSDLParseException(ex);
            }
        }catch (IOException ex){
            throw new WSDLParseException(ex);
        }

		Types types = definition.getTypes();	
		if(types==null||types.getExtensibilityElements().size()<1){
			Map imports = definition.getImports();
			Iterator importItr = imports.values().iterator();
			while(importItr.hasNext()){
				Vector importVector = (Vector)importItr.next();
				Iterator importVectorItr = importVector.iterator();
				while(importVectorItr.hasNext()){
					Import wsdlImport = (Import)importVectorItr.next();
					Definition importDefinition = wsdlImport.getDefinition();
					types = importDefinition.getTypes();
					if(types!=null){
						break;
					}
				}
			}
		}

		if(logger.isDebugEnabled()) logger.debug(new StringBuilder("wsdl=").append(wsdl)
				.append("  开始分析binding..").toString());

		JWSDLOperation[] jwsdlOperation = null;
		/*解析Service节点,service节点中endpoint描述:*/
		Map services = definition.getServices();
		Iterator serviceItr = services.keySet().iterator();
		Service service = (Service)services.get(serviceItr.next());
		Map ports = service.getPorts();
		Iterator portsItr = ports.keySet().iterator();
		while(portsItr.hasNext()){
            //该循环，从Sevice节点中招到Port描述。
            //port描述可能同时存在好几种
            //我们只寻找soap12和soap11的描述，如果招到了soap11的描述，则以soap11为准
            //@see parseSpecSoap11, parseSpecSoap12
			Port port = (Port)ports.get(portsItr.next());
			Binding binding = port.getBinding();

			PortType portType = binding.getPortType();
			String namespace = portType.getQName().getNamespaceURI();
			/*WSDL:Operation*/
			List bindingOperations = binding.getBindingOperations();
            Collections.sort(bindingOperations, BindingOPComparetor);
			
			if(logger.isDebugEnabled()) logger.debug("|__开始构造方法描述..");
			if(jwsdlOperation==null){
				jwsdlOperation = new JWSDLOperation[bindingOperations.size()];
			}
			Iterator optItr = bindingOperations.iterator();

			for(int i=0;optItr.hasNext();i++){
                if(jwsdlOperation[i]!=null&&jwsdlOperation[i].getBindingType()!=null
                    &&jwsdlOperation[i].getBindingType().equals(ABindingType.SOAP11)){
                    //这个if，保证了始终以Soap11为最优先的结果。
                    //当if不满足的时候，说明：
                    //可能还没有解析到soap11的binding。如果一直都没有，那么至少会解析出Soap12的。
                    //当先解析了Soap12的，后解析出Soap11的，会以Soap11为最终结果
                    continue;
                }

				BindingOperation bindingOperation = (BindingOperation)optItr.next();

				/*判断WSDL中是否有当前传入的标准的Binding,目前就是SOAP11*/
                SpecBag specBag = parseSpecSoap(binding, bindingOperation);

                String invokeTarget = null;
                List elements = port.getExtensibilityElements();
                for (int m = 0; m < elements.size(); m++) {
                    Object o = elements.get(m);
                    if (o instanceof SOAPAddress) {
                        SOAPAddress address = (SOAPAddress) o;
                        invokeTarget = address.getLocationURI();
                    }else if (o instanceof SOAP12Address) {
                        SOAP12Address address = (SOAP12Address) o;
                        invokeTarget = address.getLocationURI();
                    } else if (o instanceof UnknownExtensibilityElement) {
                        UnknownExtensibilityElement element = (UnknownExtensibilityElement) o;
                        QName typeName = element.getElementType();
                        if (typeName.getLocalPart().equalsIgnoreCase("address")) {
                            invokeTarget = element.getElement().getAttribute("location");
                            break;
                        }
                    }
                }

                if(specBag.getBindingType()==null){
                    String faultCode = "解析方法" + bindingOperation.getName() + "失败，不是soap11或soap12方式的binding";
                    logger.error("", new WSDLParseException(faultCode));
                    jwsdlOperation[i] = new JWSDLOperation();
                    jwsdlOperation[i].setFaultCode(faultCode);
                    jwsdlOperation[i].setAvilable(false);
                    continue;
                }

				assert specBag.getFeature()!=null;

                if(logger.isDebugEnabled()) logger.debug(new StringBuilder("|__通过soapbinding(")
                    .append(type).append(":").append(specBag.getFeature()).append(")获取解析类..").toString());
                AOperationParser parser = JWSDLSingletonOperationParserFactory.getOperationParser(wsdl,
                    specBag.getBindingType(),  specBag.getFeature());
                if(parser!=null){
                    if(logger.isDebugEnabled()) logger.debug(new StringBuilder("|__解析类")
                    .append(parser.getClass()).append("..").toString());
                    try{
                        jwsdlOperation[i] = parser.getJWSDLOperation(bindingOperation.getOperation(), types,(specBag.getBindingBodyNameSpace()==null||specBag.getBindingBodyNameSpace().trim().equals(""))?namespace:specBag.getBindingBodyNameSpace().trim());
                        jwsdlOperation[i].setInvokeTarget(invokeTarget);
                        jwsdlOperation[i].setSoapActionURI(specBag.getSoapActionURL());
                        jwsdlOperation[i].setBindingType(specBag.getBindingType());
                        continue;
                    }catch(Exception e){
                        if(logger.isDebugEnabled()) logger.error("解析方法出现错误:", e);
                        String faultCode = e.toString();
                        jwsdlOperation[i] = new JWSDLOperation();
                        jwsdlOperation[i].setFaultCode(faultCode);
                        jwsdlOperation[i].setAvilable(false);
                        continue;
                    }
                }
			}
		}	

		int faildOpCount = 0;
		for(int i=0; i<jwsdlOperation.length; i++){
			if(jwsdlOperation[i]==null||!jwsdlOperation[i].isAvilable()){
				faildOpCount++;
			}
		}
		
		if(faildOpCount==jwsdlOperation.length){
			throw new WSDLParseException("WSDL:"+wsdl+" 中解析所有方法均出现错误,无可用方法.比如有些元素是引用了schema");
		}
	
		return jwsdlOperation;
	}

    private SpecBag parseSpecSoap(Binding binding, BindingOperation bindingOperation){
        List extElementList = binding.getExtensibilityElements(); //bingding的版本描述,http或soap11,soap12

        SpecBag specBag = new SpecBag();
        for(int k=0; k<extElementList.size(); k++) {
            if (extElementList.get(k) instanceof ExtensibilityElement) {
                //取得binding的一个element
                ExtensibilityElement bindingExt = (ExtensibilityElement) extElementList.get(k);
                //在binding中，可以通过binding的定义，比如<soap:binding ...>
                //或者operation的定义，<soap12:operation soapAction="http://WebXml.com.cn/getMobileCodeInfo" style="document"/>
                //来获取到soap版本，以及soap的样式

                BindingInput bindintInput = bindingOperation.getBindingInput();
                List extensiElementList = bindintInput.getExtensibilityElements();
                String use = null;
                for (int m = 0; m < extensiElementList.size(); m++) {
                    Object o = extensiElementList.get(m);
                    if(o instanceof ExtensibilityElement) {
                        if (o instanceof SOAPBody) {
                            SOAPBody soapBody = (SOAPBody)o;
                            use = soapBody.getUse();
                            specBag.setBindingBodyNameSpace(soapBody.getNamespaceURI());
                            specBag.setBindingType(ABindingType.SOAP11);
                            break;
                        }else if (o instanceof SOAP12Body) {
                            SOAP12Body soap12Body = (SOAP12Body)o;
                            specBag.setBindingBodyNameSpace(soap12Body.getNamespaceURI());
                            specBag.setBindingType(ABindingType.SOAP12);
                            use = soap12Body.getUse();
                            break;
                        }
                    } else {
                        /*无法识别为SoapBody*/
                        UnknownExtensibilityElement element = (UnknownExtensibilityElement) extensiElementList.get(m);
                        QName typeName = element.getElementType();
                        if (typeName.getLocalPart().equalsIgnoreCase("body")) {
                            use = element.getElement().getAttribute("use");
                            String nameSpace = element.getElement().getNamespaceURI();
                            if(nameSpace.equals(W3CDataTypeConstant.XMLNS_SOAPENV11)){
                                specBag.setBindingType(ABindingType.SOAP11);
                            }else if(nameSpace.equals(W3CDataTypeConstant.XMLNS_SOAPENV12)){
                                specBag.setBindingType(ABindingType.SOAP12);
                            }
                            specBag.setBindingBodyNameSpace(nameSpace);
                            break;
                        }
                    }
                }
                if (use == null) {
                    //可能是http-bind，我们不处理
                    continue;
                }

                String style = null;
                Object o = bindingOperation.getExtensibilityElements().get(0);
                if(o instanceof ExtensibilityElement){
                    if (o  instanceof SOAPOperation) {
                        SOAPOperation soapOpertaionBody = (SOAPOperation)o;
                        style = soapOpertaionBody.getStyle();
                        if (style == null) {
                            SOAPBinding soapBindg = (SOAPBinding) binding.getExtensibilityElements().get(0);
                            style = soapBindg.getStyle();
                        }
                        String action = soapOpertaionBody.getSoapActionURI();
                        specBag.setSoapActionURL(action == null ? "" : action);
                    }else if (o instanceof SOAP12Operation) {
                        SOAP12Operation soapOpertaionBody = (SOAP12Operation)o;
                        style = soapOpertaionBody.getStyle();
                        if (style == null) {
                            SOAP12Binding soapBindg = (SOAP12Binding) binding.getExtensibilityElements().get(0);
                            style = soapBindg.getStyle();
                        }
                        String action = soapOpertaionBody.getSoapActionURI();
                        specBag.setSoapActionURL(action == null ? "" : action);
                    }
                }else{
                    UnknownExtensibilityElement soap12Element = (UnknownExtensibilityElement) bindingOperation.getExtensibilityElements().get(0);
                        /*无法识别为SoapBind*/
                    style = soap12Element.getElement().getAttribute("style");
                    if (style == null) {
                        /*TODO: 可能有错*/
                        SOAPBinding soapBindg = (SOAPBinding) binding.getExtensibilityElements().get(0);
                        style = soapBindg.getStyle();
                    }
                    String action = soap12Element.getElement().getAttribute("soapAction");
                    specBag.setSoapActionURL(action == null ? "" : action);
                }

                specBag.setFeature(new BindingFeature(style, use));
                break;
            }
        }
        return specBag;
    }

    private final class SpecBag{
        private BindingFeature feature;
        private String soapActionURL;
        private ABindingType bindingType;
        private String bindingBodyNameSpace;

        private SpecBag(BindingFeature feature, String soapActionURL, ABindingType bindingType) {
            this.feature = feature;
            this.soapActionURL = soapActionURL;
            this.bindingType = bindingType;
        }

        private SpecBag(){

        }

        private BindingFeature getFeature() {
            return feature;
        }

        private void setFeature(BindingFeature feature) {
            this.feature = feature;
        }

        private String getSoapActionURL() {
            return soapActionURL;
        }

        private void setSoapActionURL(String soapActionURL) {
            this.soapActionURL = soapActionURL;
        }

        private void setBindingType(ABindingType bindingType) {
            this.bindingType = bindingType;
        }

        private ABindingType getBindingType() {
            return bindingType;
        }

        public String getBindingBodyNameSpace() {
            return bindingBodyNameSpace;
        }

        public void setBindingBodyNameSpace(String bindingBodyNameSpace) {
            this.bindingBodyNameSpace = bindingBodyNameSpace;
        }
    }

    private static final Comparator BindingOPComparetor = new Comparator<BindingOperation>() {
        @Override
        public int compare(BindingOperation o1, BindingOperation o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

    private static final ABindingType SmartType = new ABindingType() {
        @Override
        protected void setType() {
            this.type = "Smart";
        }
    };
}
