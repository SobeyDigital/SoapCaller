package com.sobey.jcg.sobeyhive.wsinvoker.lang2;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception.ResponseException;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception.WSInvokerException;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.WSDLParser;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLOperation;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLParam;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.ASOAPHandler;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.ISoapReturnMessageAdapter;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.OutputObject;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.SOAPHandlerFactory;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.soapmessage.util.SoapMessageUtil;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.util.namespace.NameSpaceUtil;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.util.namespace.Namespace;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.Constant;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.ABindingType;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.wsa.vo.WSAddressing;

/**
 * 构造SOAP协议,发送请求,接收返回数据,分析
 * @author wx
 */
public class SoapCaller
{
	private WSAddressing addressing;
	private String operationName;
    private Integer timeout;
	private Object[] params;
	private Class exceptReturnType;
	private String username;//还不知道如何实现WS-Secuirity
	private String password;//还不知道如何实现WS-Secuirity
	private JWSDLOperation[] jwsdlOperation;
    private String wsdl;
	private ISoapReturnMessageAdapter returnMessageAdapter; //自定义处理返回的接口
	
	private static WSDLParser wsdlParser = new WSDLParser();
    private static Logger logger = LoggerFactory.getLogger(SoapCaller.class);
	
	private static Map<String, JWSDLOperation[]> operationCache = new HashMap<String, JWSDLOperation[]>();
	
	private SoapCaller(String wsdl,Integer timeout)
	{
        this.wsdl = wsdl;
        this.timeout = timeout;
		JWSDLOperation[] opertaions = operationCache.get(wsdl);
        if(opertaions==null){
            synchronized (wsdl.intern()){
                if(opertaions==null){
                    opertaions = wsdlParser.parse(wsdl,timeout==null?15000:timeout);
                    operationCache.put(wsdl, opertaions);
                }else{
                    opertaions = operationCache.get(wsdl);
                }
            }
        }

		if(opertaions==null){
			throw new UnsupportedOperationException(new StringBuilder(wsdl).append("中,没有解析出方法").toString());
		}
		int len = opertaions.length;
		this.jwsdlOperation = new JWSDLOperation[len];
		for(int i=0; i<len; i++){
            try {
                this.jwsdlOperation[i] = opertaions[i].clone();
            }catch (CloneNotSupportedException e){
                e.printStackTrace();//不可能
            }
		}
	}
	
	public static SoapCaller getInstance(String wsdl, ABindingType bindingType)
	{
		return new SoapCaller(wsdl,null);
	}

    public static SoapCaller getInstance(String wsdl, ABindingType bindingType,int timeout)
    {
        return new SoapCaller(wsdl,timeout);
    }

    public static  SoapCaller getInstance(String wsdl)
    {
        return new SoapCaller(wsdl,null);
    }

    public static  SoapCaller getInstance(String wsdl,int timeout)
    {
        return new SoapCaller(wsdl,timeout);
    }
	
	public SoapCaller setOperatioName(String operationName)
	{
		this.operationName = operationName;
		return this;
	}
	
    public SoapCaller setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    
	public SoapCaller setExceptReturnType(Class exceptReturnType)
	{
		this.exceptReturnType = exceptReturnType;
		return this;
	}
	
	public SoapCaller setParams(Object... params)
	{
		this.params = params;
		return this;
	}
	
	public SoapCaller setWSAddressing(WSAddressing addressing)
	{
		this.addressing = addressing;
		return this;
	}
	
	public SoapCaller setReturnMessageAdapter(ISoapReturnMessageAdapter returnMessageAdapter)
	{
		this.returnMessageAdapter = returnMessageAdapter;
		return this;
	}
	
	public OutputObject invoke() throws WSInvokerException, ResponseException
	{
		if(operationName==null){
			throw new WSInvokerException("没有设置方法名");
		}
		
		if(addressing==null){
			if(logger.isDebugEnabled()) logger.debug("!没有设置WS-Addressing信息...");
		}
		if(exceptReturnType==null){
			if(logger.isDebugEnabled()) logger.debug("!没有设置exceptReturnType...");
		}
		
		JWSDLOperation currentOperation = SoapHelper.establishAnalyzeContext(jwsdlOperation)
												    .findMatchedOperation(operationName, params)
												    .bindValue();
		if(currentOperation==null){
			throw new WSInvokerException("没有找到方法"+operationName);
		}
		Namespace[] namespaces = NameSpaceUtil.newInstance().analyzeNamespaces(currentOperation);
		

		ASOAPHandler hanlder = SOAPHandlerFactory.getHanlder(currentOperation.getBindingType(),currentOperation.getFeature());
		if(addressing!=null){	
			hanlder.addWSAddressing(addressing);
		}

        SOAPMessage returnMessage = null;
        try {
            SOAPMessage message = hanlder.createSoapMessage(currentOperation, namespaces);
            message.getMimeHeaders().setHeader("SOAPAction", currentOperation.getSoapActionURI());
            if (logger.isDebugEnabled()) {
                logger.debug(new StringBuilder("构造出的SOAP请求: ")
                    .append(SoapMessageUtil.getSoapMessageString(message)).toString());
            }

            System.setProperty("sun.net.client.defaultReadTimeout", "20000");
            SOAPConnectionFactory factory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = factory.createConnection();
            try{
                if (timeout!=null){
                    URL endpoint = new URL(new URL(currentOperation.getInvokeTarget()),"",new URLStreamHandler(){
                        @Override
                        protected URLConnection openConnection(URL url) throws IOException {
                            URL target = new URL(url.toString());
                            URLConnection connection = target.openConnection();
                            // 设置超时时间
                            connection.setConnectTimeout(timeout/2);
                            connection.setReadTimeout(timeout);
                            return connection;
                        }
                    });
                    returnMessage = connection.call(message, endpoint);
                }else{
                    returnMessage = connection.call(message, currentOperation.getInvokeTarget());
                }
            }catch(SOAPException e){
                operationCache.remove(this.wsdl); //如果调用失败，则remove之后重新来解析
                connection.close();
                logger.error("错误:",e);
                throw new Exception(e);
            }

            connection.close();
        }catch (Exception e){
            throw new WSInvokerException(e);
        }
		

		
		if (currentOperation.getStyle().equalsIgnoreCase(Constant.OPERATION_STYLE_ONEWAY)) {
			return null;
		}	
		
		if (returnMessage==null) {
			return null;
		}	

		/*开始处理返回数据*/
		StringBuilder soapReturn = new StringBuilder(SoapMessageUtil.getSoapMessageString(returnMessage));
		if(logger.isDebugEnabled()){
			logger.debug(new StringBuilder("接收到的SOAP返回:")
			.append(soapReturn).toString());
		}

        try {
            SOAPPart replySoapPart = returnMessage.getSOAPPart();
            SOAPEnvelope replySoapEnv = replySoapPart.getEnvelope();
            SOAPBody replySoapBody = replySoapEnv.getBody();
            if (replySoapBody.hasFault()) {
                logger.info(new StringBuilder("ERROR: ").append(replySoapBody.getFault().getFaultString()).toString());
                throw new WSInvokerException("WS调用出现错误: faultCode=" + replySoapBody.getFault().getFaultCode() +
                    "  faultString=" + replySoapBody.getFault().getFaultString());
            }
        }catch (SOAPException e){
            throw new WSInvokerException(e);
        }
 
        OutputObject result = new OutputObject();
		/*如果设置了自定义返回处理类,则使用自定义处理来解析返回数据*/
		if(this.returnMessageAdapter!=null){
			if(logger.isDebugEnabled()) logger.debug(new StringBuilder("自定义处理类:")
			.append(this.returnMessageAdapter).append("开始处理").toString());
			Object bodyObject = this.returnMessageAdapter.handleSOAPResponse(returnMessage,
					currentOperation.getOutputParam(), exceptReturnType);
			result.setBodyObject(bodyObject);
		}else
		/*如果设置了返回类型,并且方法实现了处理,则将返回的数据构造成返回的对象,否则直接返回SOAPMessage数据*/
		if(this.exceptReturnType!=null&&!void.class.isAssignableFrom(exceptReturnType)){
			if(logger.isDebugEnabled()) logger.debug("配置了返回类型,进行转换");
			if(currentOperation.getOutputParam()==null){
				if(logger.isDebugEnabled()) logger.debug("配置了返回类型,但WSDL解析没有发现需要返回数据,因此返回空");
				return null;
			}
            Object bodyObject = hanlder.handleSOAPResponse(returnMessage,
					currentOperation.getOutputParam(), exceptReturnType);
			result.setBodyObject(bodyObject);
		}		
		result.setSoapReturnMessage(soapReturn);		
		return result;
	}
	
	public static LowlevelInstance getLowlevelInstance(String wsdl, ABindingType bindingType) throws Exception{
		SoapCaller caller = new SoapCaller(wsdl,null);
		LowlevelInstance instance = new LowlevelInstance(caller);
		return instance;
	}

    public static LowlevelInstance getLowlevelInstance(String wsdl) throws Exception{
        SoapCaller caller = new SoapCaller(wsdl,null);
        LowlevelInstance instance = new LowlevelInstance(caller);
        return instance;
    }
	
	public static class LowlevelInstance{
		private SoapCaller caller;
		private WSAddressing addressing;
		private String operationName;
		private Element soapBodyContent;
        protected static Logger logger = LoggerFactory.getLogger(LowlevelInstance.class);

		private LowlevelInstance(SoapCaller caller){
			this.caller = caller;
		}
		
		public LowlevelInstance setOperatioName(String operationName)
		{
			this.operationName = operationName;
			return this;
		}
		
		public LowlevelInstance setParams(Element soapBodyContent)
		{
			this.soapBodyContent = soapBodyContent;
			return this;
		}
		
		public LowlevelInstance setWSAddressing(WSAddressing addressing)
		{
			this.addressing = addressing;
			return this;
		}
		
		public OutputObject invoke() throws Exception{
			if(operationName==null){
				throw new WSInvokerException("没有设置方法名");
			}
			if(addressing==null){
				if(logger.isDebugEnabled()) logger.debug("!没有设置WS-Addressing信息...");
			}
			
			JWSDLOperation currentOperation = null;
			for(JWSDLOperation op : this.caller.jwsdlOperation){
				if(op.isAvilable()&&op.getLocalName().equalsIgnoreCase(operationName)){
					currentOperation = op;
					break;
				}
			}
			
			if(currentOperation==null){
				throw new Exception("没有找到方法"+operationName);
			}
			
			ASOAPHandler hanlder = new ASOAPHandler() {
				@Override
				protected Object processReturnMessageBody(SOAPBody body,
						JWSDLParam outputParam, Class clazz) throws ResponseException {
					return null;
				}	
				@Override
				protected void createSoapBodyContent(SOAPEnvelope envelope,
						JWSDLOperation operation, Namespace[] namespaces) throws Exception {
					SOAPElement body = envelope.getBody();				
					Node node = body.getOwnerDocument()
							.importNode((Element)LowlevelInstance.this.soapBodyContent, true);
					body.appendChild(node);
				}
			};
			if(addressing!=null){	
				hanlder.addWSAddressing(addressing);
			}
			SOAPMessage message = hanlder.createSoapMessage(currentOperation, new Namespace[]{});
			message.getMimeHeaders().setHeader("SOAPAction", currentOperation.getSoapActionURI());
			if(logger.isDebugEnabled()){		
				logger.debug(new StringBuilder("构造出的SOAP请求: ")
				.append(SoapMessageUtil.getSoapMessageString(message)).toString());
			}
			
			System.setProperty("sun.net.client.defaultReadTimeout", "20000");
			SOAPConnectionFactory factory = SOAPConnectionFactory.newInstance();
			SOAPConnection connection = factory.createConnection();
			SOAPMessage returnMessage = null;
			try{
				returnMessage = connection.call(message, currentOperation.getInvokeTarget());
			}catch(Throwable e){
				connection.close();
				logger.error("错误",e);
				throw new Exception(e);
			}
			
			connection.close();
			
			if (currentOperation.getStyle().equalsIgnoreCase(Constant.OPERATION_STYLE_ONEWAY)) {
				return null;
			}	
			
			if (returnMessage==null) {
				return null;
			}	

			/*开始处理返回数据*/
			StringBuilder soapReturn = new StringBuilder(SoapMessageUtil.getSoapMessageString(returnMessage));
			if(logger.isDebugEnabled()){
				logger.debug(new StringBuilder("接收到的SOAP返回:")
				.append(soapReturn).toString());
			}
			SOAPPart replySoapPart = returnMessage.getSOAPPart();
	        SOAPEnvelope replySoapEnv = replySoapPart.getEnvelope();
	        SOAPBody replySoapBody = replySoapEnv.getBody();
	        if (replySoapBody.hasFault()) {
	        	logger.info(new StringBuilder("ERROR: ").append(replySoapBody.getFault().getFaultString()).toString());
	        	throw new WSInvokerException("WS调用出现错误: faultCode="+replySoapBody.getFault().getFaultCode()+
	        			"  faultString="+replySoapBody.getFault().getFaultString());
	        }
	        OutputObject result = new OutputObject();
	        
	        result.setBodyObject(returnMessage.getSOAPBody().getFirstChild());
			result.setSoapReturnMessage(soapReturn);		
			return result;
		}
	}
}
