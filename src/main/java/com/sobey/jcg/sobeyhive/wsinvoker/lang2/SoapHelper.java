package com.sobey.jcg.sobeyhive.wsinvoker.lang2;

import java.util.Hashtable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception.WSInvokerException;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLOperation;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLParam;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.util.TypeCompareUtil;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.feature.BindingFeature;

/**
 * 辅助调用的类,作一些分析处理
 * 所有方法本包可见.不提供外部访问
 */
public class SoapHelper
{
	private static Map dynamicClassCache = new Hashtable();
	private static Map sameNameMethodSuffixCache = new Hashtable();
    private static Logger logger = LoggerFactory.getLogger(SoapHelper.class);
	private SoapHelper(){}
	
	static Analyzer establishAnalyzeContext(JWSDLOperation[] jwsdlOperations)
	{
		Analyzer analyzer = new Analyzer();
		analyzer.setJwsdlOperations(jwsdlOperations);
		return analyzer;
	}
	
	/**
	 * 用一个内部类的方式来屏蔽外部的调用顺序.对与调用Helper,必须先设置Target
	 * @author wx
	 */
    static class Analyzer
	{
    	private JWSDLOperation[] jwsdlOperations;
    	
    	JWSDLOperation[] getJwsdlOperations() {
			return jwsdlOperations;
		}
		void setJwsdlOperations(JWSDLOperation[] jwsdlOperations) {
			this.jwsdlOperations = jwsdlOperations;
		}
    	
		/**
		 * 分析出一个可用的方法,因为方法重载的定义是:参数类型和个数不相同,并不是返回类型不同.因此不需要验证返回类型
		 * @param operationName
		 * @param params
		 * @return
		 * @throws Exception
		 */
		ValueBinder findMatchedOperation(String operationName, Object[] params) throws WSInvokerException
		{
			JWSDLOperation operation = this.find(operationName, params);

			if(operation==null){
				throw new WSInvokerException("无法获得匹配的调用方法"+operationName+"," +
					"请检查方法名,参数个数,参数类型(不包含参数名),对象型参数的字段类型和名字。特别注意Choice的情况和顺序");
			}
			
			ValueBinder valueBinder = new ValueBinder(operation, params);
			return valueBinder;
		}
		
		private JWSDLOperation find(String operationName, Object[] params) {
			if(SoapHelper.logger.isDebugEnabled()) SoapHelper.logger.debug(new StringBuilder("分析可用方法: 传入名称-")
			.append(operationName).toString());
			JWSDLOperation operation = null;
			for(int i=0; i<jwsdlOperations.length; i++){
				/*方法中某一个可能为空,因为c发布的有可能是错误的,
				 * 比如http://www.webxml.com.cn/WebServices/WeatherWebService.asmx?wsdl*/
				JWSDLOperation temp = jwsdlOperations[i];
				/**
				 * 2010-03-08 对于document/literal的,可以只比较参数.这样就可以不传方法名.由于只比较参数,因此方法名不同
				 * 而参数相同的可能会被调用.这也正符合了document/literal无方法名,不好进行方法定位的特性
				 */
				if((operationName==null||operationName.equalsIgnoreCase(""))
						&&temp!=null&&temp.getFeature().equals(BindingFeature.DOCUMENT_LITERAL)){
					operationName = temp.getLocalName();
				}
				if(temp!=null&&temp.isAvilable()&&temp.getLocalName().equalsIgnoreCase(operationName)){
					/*方法名匹配*/
					JWSDLParam[] jwsdlParams = temp.getInputParam();
					if(jwsdlParams!=null&&params!=null){
                        if(jwsdlParams.length==params.length) {
                            /*参数个数也匹配*/
                            if (SoapHelper.logger.isDebugEnabled())
                                SoapHelper.logger.debug(new StringBuilder("分析可用方法: 传入名称-")
                                    .append(operationName).append(" 方法参数个数匹配,个数=")
                                    .append(jwsdlParams.length).append(",分析参数匹配度").toString());
                            if (this.isParamMatch(jwsdlParams, params)) {
                                operation = temp;
                                break;
                            }
                        }
					}else if((jwsdlParams==null&&params==null)
							||(jwsdlParams==null&&params!=null&&params.length==0)
							||(jwsdlParams!=null&&jwsdlParams.length==0&&params==null)){
						/*参数都为空*/
						if(SoapHelper.logger.isDebugEnabled())
							SoapHelper.logger.debug(new StringBuilder("分析可用方法: 传入名称-")
							.append(operationName).append(" 方法参数为空,匹配WSDL中空参数方法")
							.append(temp.getLocalName()).toString());
						operation = temp;
						break;
					}

                    /**
					if(operation==null){
						/*因为调用的时候,直接传入的参数肯定是按照方法的参数描述来传的.而对于document/literal
						 * -wrapped的情况比较特殊,它是把方法也作为了元素.因此尝试获取wrapper的方法
						if(jwsdlParams!=null&&jwsdlParams.length==1&&temp.getFeature().equals(BindingFeature.DOCUMENT_LITERAL)){
							JWSDLParam wrapperParam = jwsdlParams[0];
							JWSDLParamType wsdlParamType = wrapperParam.getType();
							if(Void.class.isAssignableFrom(wsdlParamType.getJavaType())&&
									(params==null||(params!=null&&params.length==0))){
								operation = temp;
								operation.setInputWrapper(wrapperParam.getParamName());
								operation.setInputParam(null);
								break;
							}else if(params!=null&&params.length>0){
								/*去掉wrapper的方法元素
								JWSDLParamTypeField[] fields = wsdlParamType.getDeclaredFields();
								if(fields!=null){
                                    boolean match = true;
                                    if(fields.length==params.length) {
                                        //参数一一匹配的情况
                                        for (int k = 0; k < fields.length; k++) {
                                            match = match && (params[k] == null || TypeCompareUtil.isParamTypeMatch(fields[k].getType(), params[k].getClass(), "base"));
                                        }

                                        if (match) {
                                            /*重新设置方法参数集合为解开wrapper之后的参数
                                            operation = temp;
                                            operation.setInputWrapper(wrapperParam.getParamName());
                                            JWSDLParam[] wrappedParam = new JWSDLParam[fields.length];
                                            for (int m = 0; m < fields.length; m++) {
                                                wrappedParam[m] = new JWSDLParam();
                                                wrappedParam[m].setParamName(fields[m].getQname());
                                                wrappedParam[m].setType(fields[m].getType());
                                                wrappedParam[m].setValue(params[m]);
                                            }
                                            if (SoapHelper.logger.isDebugEnabled())
                                                SoapHelper.logger.debug(new StringBuilder("|__分析可用方法: 传入名称-")
                                                    .append(operationName).append(",通过document/literal wrapped手段分析成功,重设参数").toString());
                                            operation.setInputParam(wrappedParam);
                                            break;
                                        }
                                    }
								}
							}
						}
					}*/
				}
			}
			
			if(operation!=null&&SoapHelper.logger.isDebugEnabled()){
				StringBuilder buffer = new StringBuilder();
				buffer.append("===方法===:").append(operation.getLocalName()).append(" [")
				.append(operation.getFeature()).append("]\n");
				if(operation.getInputParam()!=null){
					for(int k=0; k<operation.getInputParam().length; k++){
						buffer.append("|__入参:").append(operation.getInputParam()[k].getParamName())
								.append(" [类型-").append(operation.getInputParam()[k].getType()).append("]\n");
					}
				}
				if(operation.getOutputParam()!=null){
					buffer.append("|__返回:").append(operation.getOutputParam().getParamName())
							.append(" [类型-").append(operation.getOutputParam().getType()).append("]\n");
				}
				SoapHelper.logger.debug(buffer.toString());
			}
			return operation;
		}
		
		private boolean isParamMatch(JWSDLParam[] jwsdlParams, Object[] params){
			boolean match = true;

			for(int j=0; j<jwsdlParams.length; j++){
				/*看看参数的类型是否一样*/
				JWSDLParam jwsdlparam = jwsdlParams[j];
				Object apiParam = params[j];
				if(SoapHelper.logger.isDebugEnabled()) SoapHelper.logger.debug(new StringBuilder("分析第")
				.append(j+1).append("参数匹配度: wsdlparam-").append(jwsdlparam)
				.append("   |and|  apiparam-").append(apiParam).toString());
				boolean resultJ = false;
				if(apiParam==null){
					resultJ = true;
				}else{
					resultJ = TypeCompareUtil.isParamTypeMatch(jwsdlparam.getType(), apiParam.getClass(), "base");
				}
				match = match&&resultJ;
			}
			return match;
		}
	}
    
    /**
     * 绑定JAVA参数得到JWSDL参数中
     * @author wx
     */
    static class ValueBinder
    {
    	private JWSDLOperation jwsdlOperation;
    	private Object[] params;
    	ValueBinder(JWSDLOperation jwsdlOperation, Object[] params)
    	{
    		this.jwsdlOperation = jwsdlOperation;
    		this.params = params;
    	}
    	
    	/**
    	 * 为WSDL的参数绑定数据值
    	 * @return
    	 */
    	JWSDLOperation bindValue()
    	{
    		JWSDLParam[] jwsdlParams = jwsdlOperation.getInputParam();
    		if(jwsdlParams!=null&&params!=null&&jwsdlParams.length==params.length){
    			for(int i=0; i<jwsdlParams.length; i++){
    				jwsdlParams[i].setValue(params[i]);
    			}
    		}
    		return jwsdlOperation;
    	}
    }
}
