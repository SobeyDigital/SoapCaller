package com.sobey.jcg.sobeyhive.wsinvoker.viewer;


import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.WSDLParser;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLOperation;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.ABindingType;

public class MethodList
{
	public static String[] getMethodDesc(String wsdl)
	{
		WSDLParser parser = new WSDLParser();
		JWSDLOperation[] jwsdlOperation = parser.parse(wsdl, ABindingType.SOAP11,15000);
		if(jwsdlOperation!=null){
			String[] ret = new String[jwsdlOperation.length];
			for(int i=0; i<jwsdlOperation.length; i++){
				StringBuilder buffer = new StringBuilder();
				JWSDLOperation jwsdlOp = jwsdlOperation[i];
				buffer.append("===方法===:"+jwsdlOp.getLocalName()+" ["+jwsdlOp.getFeature()+"]\n");
				if(jwsdlOp.getInputWrapper()!=null){
					buffer.append(" wrapper:"+jwsdlOp.getInputWrapper()+"\n");
				}
				if(jwsdlOp.getInputParam()!=null){
					for(int k=0; k<jwsdlOp.getInputParam().length; k++){
						buffer.append("|__入参:"+jwsdlOp.getInputParam()[k].getParamName()
								+" [类型-"+jwsdlOp.getInputParam()[k].getType()+"]\n");
					}
				}
				if(jwsdlOp.getOutputParam()!=null){
					buffer.append("|__返回:"+jwsdlOp.getOutputParam().getParamName()
							+" [类型-"+jwsdlOp.getOutputParam().getType()+"]\n");
				}
				ret[i] = buffer.toString();
			}
			return ret;
		}
		return null;
	}
}
