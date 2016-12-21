package com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.operation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.wsdl.Operation;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;

import com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse.vo.JWSDLOperation;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception.WSInvokerException;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.ABindingType;
import com.sobey.jcg.sobeyhive.wsinvoker.lang2.w3c.bindingType.feature.BindingFeature;

/**
 * 将WSDL中的Operation解析成能看得懂的方式
 * @author wx
 */
public abstract class AOperationParser {
	public abstract BindingFeature getFeature();
	public abstract ABindingType getType();
	public String toString() {
		return this.getFeature().toString()+this.getType().toString();
	}
	
	protected String wsdl;
	public AOperationParser(String wsdl){
		this.wsdl = wsdl;
	}
	
	protected final XmlSchema[] parseTypesToSchema(Types types)
	{
		List list = new ArrayList();
		if(types!=null){
			Iterator iter = types.getExtensibilityElements().iterator();
			while (iter.hasNext()) {
				ExtensibilityElement elment = (ExtensibilityElement) iter.next();
				if (elment.getElementType().getLocalPart().equalsIgnoreCase(
						"schema")) {
					org.w3c.dom.NodeList nodeList = null;
					if(elment instanceof Schema){
						Schema W3Cschema = (Schema) elment;
						org.w3c.dom.Element W3CschemaElement = W3Cschema.getElement();
						
						try{
							XmlSchemaCollection schemaCol = new XmlSchemaCollection();
							XmlSchema apacheSchema = schemaCol.read(W3CschemaElement, null);
							list.add(apacheSchema);
						}catch(Exception e){
							e.printStackTrace();
						}
						
						nodeList = W3CschemaElement.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "import");
					}else{
						UnknownExtensibilityElement extensibilityElement = (UnknownExtensibilityElement)elment;
						nodeList = extensibilityElement.getElement().getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "import");
					}
						
					if(nodeList!=null&&nodeList.getLength()>0){
						for(int i=0; i<nodeList.getLength(); i++){
							org.w3c.dom.Element node = (org.w3c.dom.Element)nodeList.item(i);
							String location = node.getAttribute("schemaLocation");
							if(location!=null&&location.trim().length()>0){
								try{
									URL url = new URL(location);
									XmlSchemaCollection schemaCol = new XmlSchemaCollection();
									Reader reader = new InputStreamReader(url.openStream());
									XmlSchema apacheSchema = schemaCol.read(reader, null);
									list.add(apacheSchema);
								}catch(Exception e){
                                    e.printStackTrace();
                                    try {
                                        File uri = new File(location);
                                        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
                                        Reader reader = new InputStreamReader(new FileInputStream(uri));
                                        XmlSchema apacheSchema = schemaCol.read(reader, null);
                                        list.add(apacheSchema);
                                    }catch(Exception e1){
                                        e1.printStackTrace();
                                    }
								}
							}
						}
					}
				}
			}
			XmlSchema[] schema = new XmlSchema[list.size()];
			schema = (XmlSchema[])list.toArray(schema);
			return schema;
		}else{
			return new XmlSchema[]{new XmlSchema()};
		}
	}
	
	protected final XmlSchemaElement findElementInSchema(QName elementQName, XmlSchema[] schemas)
	{
		for(int i=0; i<schemas.length; i++){
			XmlSchemaElement element = schemas[i].getElementByName(elementQName);
			if(element!=null){
				return element;
			}
		}
		return null;
	}
	
	protected final XmlSchemaType findTypeInSchema(QName typeQName, XmlSchema[] schemas)
	{
		for(int i=0; i<schemas.length; i++){
			XmlSchemaType type = schemas[i].getTypeByName(typeQName);
			if(type!=null){
				return type;
			}
		}
		return null;
	}

	public abstract JWSDLOperation getJWSDLOperation(Operation operation, Types types, String namespaceURI) throws WSInvokerException;
}
