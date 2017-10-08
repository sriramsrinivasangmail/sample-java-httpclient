
import java.net.HttpURLConnection;
import java.security.Security;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

//import HTTPClientUtils;

public class HTTPClientTest
{
    
    public static final String JDK_TLS_DISABLED_ALGORITHMS = "jdk.tls.disabledAlgorithms";
    public final static String JAVA_PROTOCOL_HDLR_PKGS_KEY = "java.protocol.handler.pkgs"; 
    public final static String JAVA_PROTOCOL_HDLR_PKGS_DEFAULT_VALUE = "com.ibm.net.ssl.www2.protocol"; 
    
    // needed with JDK 7 to avoid issues with making https client requests & getting handshake issues
    public final static String JAVA_JSSE_SNI = "jsse.enableSNIExtension";
   
    private static void setJVMProperties() 
    {

        
        //System.setProperty(JAVA_PROTOCOL_HDLR_PKGS_KEY, JAVA_PROTOCOL_HDLR_PKGS_DEFAULT_VALUE);
        
        //System.setProperty(JAVA_JSSE_SNI,"false");
        
        // allow for a config param in the environment
        String disabledAlgorithms = System.getProperty("dsweb_tls_disabledAlgorithms",
                Security.getProperty(JDK_TLS_DISABLED_ALGORITHMS));
        
        if (disabledAlgorithms == null) 
        {
            disabledAlgorithms = "SSLv3,RC4,DESede,MD5,DH keySize <2048";  // we turn these off by default - for BEAST & other vulnerabilities
        } 
        else
        {
            if (!disabledAlgorithms.contains("RC4")) 
            {
                disabledAlgorithms += ", RC4";
            }
            if(!disabledAlgorithms.contains("SSLv3"))
            {
                disabledAlgorithms += ",SSLv3";
            }
            if(!disabledAlgorithms.contains("DESede"))
            {
                disabledAlgorithms += ",DESede";
            }
            if(!disabledAlgorithms.contains("MD5"))
            {
                disabledAlgorithms += ",MD5";
            }
            if(!disabledAlgorithms.contains("DH keySize <2048"))
            {
                disabledAlgorithms += ",DH keySize <2048";
            }
        }
                
        Security.setProperty(JDK_TLS_DISABLED_ALGORITHMS, disabledAlgorithms);
        
        System.out.println("disabled algorithms: "  + Security.getProperty(JDK_TLS_DISABLED_ALGORITHMS));
        
        System.out.println("System properties: " +  System.getProperties());
        
    }
    public static void main(String[] args) throws Exception
    {
        String url = args[0];
        String method = null;
	String urlParameters = null;
	String additionalHeaders = null;

	if(args.length > 1)
	{
		method = args[1];
	}
	if(args.length > 2)
	{
        	urlParameters = args[2];
	}
	if(args.length > 3)
	{
        	additionalHeaders =  args[3];
	}
        
        if(null != System.getenv("enableJVMRestrictions"))
        {
            setJVMProperties();
        }
        HashMap additionalHeadersMap = new HashMap();
        if (null != additionalHeaders)
        {
            List hdrValPair = HTTPClientUtils.splitString(additionalHeaders, "&");
            for (Iterator pairIter = hdrValPair.iterator(); pairIter.hasNext();)
            {
                String currPair = (String) pairIter.next();
                String pair[] = currPair.split(HTTPClientUtils.COLON);
                additionalHeadersMap.put(pair[0], pair[1]);
            }
        }
        
        HttpURLConnection conn = HTTPClientUtils.makeHTTPCall((List) null, url, urlParameters, additionalHeadersMap, method);
        String output = HTTPClientUtils.readHTTPBuffer(conn);
        System.out.println("output: \n" + output);
        
    }
}
