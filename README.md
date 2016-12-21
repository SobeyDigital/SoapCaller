# DynamicSoapCaller
A dynamic soap caller, can help you perform soap-based-webservice very convenient！

if you have used XMLSpy, you will see this is the same function like XMLSpy's soapcaller.

--------------

## Fetures
1. support **soap1.1**, **soap1.2**
2. **automatic match** soap1.1 or soap1.2
3. support **rpc/encoded**, **rpc/literal**, **document/literal**, **document/literal wrapped**
3. support **ws-addressing**
4. support **endpoint-reference**
5. support **automatic java bean assembling**

------------
## How to use
### **1. hello word**
```java
OutputObject outputObject =  SoapCaller.getInstance("http://172.16.163.5:8989/?wsdl") //specific a wsdl
                .setOperatioName("method") //operation to invoke
                .setParams("param1","param2", 3) //params
                .setExceptReturnType(String.class) //set your except return type to automatic assemble
                .invoke(); //call it

        System.out.println(outputObject.getSoapReturnMessage());//return of soap message
        System.out.println(outputObject.getBodyObject()); //this is the except return type val
        //notice that if you didn't setExceptReturnType,  outputObject.getBodyObject() will return null
```

### **2. show methods of wsdl**
```java
String[] methodsDesc = MethodList.getMethodDesc(wsdl);
```

### **3. call with java bean use `automatic type assemble`**
if the webservice A with wsdl "http://example/service?wsdl", has method "Amethod", and, its param is a "schema descript object and can transform to java bean", and return type is also.  you can use `automatic type assemble`

```java
OutputObject outputObject =  SoapCaller.getInstance("http://example/service?wsdl") 
                .setOperatioName("Amethod") 
                .setParams(new Person()) //this is a java bean, it described as schema in the wsdl as an inputmessage
                .setExceptReturnType(Teacher.class) //set your except return type to automatic assemble
                //the Teacher class is also described as schema in wsdl as an outputmessage
                .invoke(); //call it

        System.out.println(outputObject.getSoapReturnMessage());//return of soap message
        System.out.println((Teacher)outputObject.getBodyObject()); //this is the except return type val
```

and you can deserialize the output customly

```java
OutputObject outputObject =  SoapCaller.getInstance("http://example/service?wsdl") 
                .setOperatioName("Amethod") 
                .setParams(new Person()) //this is a java bean, it described as schema in the wsdl as an inputmessage
                .setReturnMessageAdapter(new ISoapReturnMessageAdapter<Teacher>() {
            	   @Override
	            	public Teacher handleSOAPResponse(SOAPMessage returnMessage, JWSDLParam outputParam,
	            			Class<String> clazz) {
	            		// generate output object your self
	            		return null;
	            	}
               	})
                .invoke(); //call it

        System.out.println(outputObject.getSoapReturnMessage());//return of soap message
        System.out.println((Teacher)outputObject.getBodyObject()); //this is the except return type val
```

### **4. use `ws-addressing` and `endpoint-reference`**
as known of "ws-addressing"(@see http://www.w3school.com.cn/ for more information), we can simply set it to the `soap-header`

```java
WSAddressing wsAddressing = new WSAddressing();
wsAddressing.setMessageID(new WSAMessageID("dfasdfad34234df"));
        wsAddressing.setReplyTo(new WSAReplyTo(new WSAAddress("http://111.111.111.111")));
        
EndpointReference endpointReference = new EndpointReference("http://hive.sobey.com");
endpointReference.addProperty("ReplyMethod", "noMethod");
endpointReference.addProperty("ListenOn", "11111");

wsAddressing.setEndpointReference(endpointReference);

OutputObject outputObject = SoapCaller.getInstance("http://172.16.163.5:8989/?wsdl")
                .setOperatioName("test")
                .setWSAddressing(wsAddressing) //set addressing
                .invoke();
                //with this call, WSAddressing and EndpointReference will build to soap-header
```

### **5. call with w3cElement**

```java
  SoapCaller.getLowlevelInstance("http://172.16.163.5:8989/?wsdl")
       			 .setOperatioName("test")
       			 .setParams(new W3CElement())
       			 .invoke();
```


### **7. use a dynamic interface**

```java
//fisrt: define a interface
interface TestServiceCtrl{
		public static String wsdl = "http://172.16.1.6:9080/SobeyMAMWEB/services/DCMSystemConstantControl?wsdl";
		public DCMSystemConstant getSystemConstantById(Params p);
}

//use WebServiceProxyFactory to get a implemetion
TestServiceCtrl dctr = (TestServiceCtrl)WebServiceProxyFactory.getInstance().getWebServiceProxy(TestServiceCtrl.wsdl, TestServiceCtrl.class);
//use the dynamic implemetion to call
DCMSystemConstant con = dctr.getSystemConstantById(new Param(1021, "test"));

```