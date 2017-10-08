import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HTTPClientUtils
{
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_DELETE = "DELETE";
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3";
    public static final String DEFAULT_REQ_CONTENT_TYPE = "application/x-www-form-urlencoded";
    public static final String ACCEPT_Encoding = "Accept-Encoding";
    public static final String ACCEPT_CHARSET = "Accept-Charset";
    public static final String CONTENT_LANGUAGE = "Content-Language";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String USER_AGENT = "User-Agent";
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APP_JSON = "application/json";
    public static final String DQUOTE = "\"";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String CR=" \n";
    public static final String PIPE="|";

    public static final String SSL_TRUST_STORE_PROP = "javax.net.ssl.trustStore";
    public static final String HTTP_PREFIX = "http://";
    public static final String HTTPS_PREFIX = "https://";
    public static final String DSWEB_TRUST_ALL = "dsweb.trust.all";
    public static final String[] supportedLanguages = {"en", "de", "es", "fr", "it", "ja", "ko", "pt_BR", "zh_CN", "zh_TW", "cs", "hu", "pl", "ru"};
    public static final String INPUT_STRINGS_DO_NOT_MATCH = "input strings do not match";
    public static final String ARG_HELP = "help";
    public static final String PARAM_FORMAT = "format";
    public static final String PARAM_JSON = "json";
    
    public static boolean isDebug = ("true".equalsIgnoreCase(System.getProperty("DSHTTPCLIENT_DEBUG")) 
                                    || ("true".equalsIgnoreCase(System.getenv("DSHTTPCLIENT_DEBUG")) ));

    public static LinkedHashMap<String, String> languagesMap = new LinkedHashMap<String, String>();
    public static LinkedHashMap<String, Locale> localeMap = new LinkedHashMap<String, Locale>();
    static 
    {
        setupLangLocaleMaps(supportedLanguages, languagesMap, localeMap);
    };

    public static void debugLog(String... args)
    {
        if(isDebug)
        {
            doLog(args);
        }
    }

    public static void doLog(String... args)
    {
        doLog(System.err, args);
    }
    
    public static void doLog(PrintStream logStream, String... args)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        logStream.print(dateFormat.format(date) + " ");
        for (int i = 0; i < args.length; i++)
        {
            String currStr = args[i];
            logStream.print(currStr);
        }
        logStream.println("");
    }

    public static Logger createLogger(String logFileName, int maxlogSize, int maxlogFiles) 
    {
        Logger logger = Logger.getLogger(logFileName);  
        try
        {
            FileHandler fh =  new FileHandler(logFileName, maxlogSize, maxlogFiles);  
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);
        }
        catch (IOException e)
        {
            System.err.println("Log file creation failure:  " + e   + " logging to console");
        }
        
        return logger;
    }
    
    public static Locale standardLocale = getStandardLocale();
    
    public static String clientLocaleStr = standardLocale.toString(); 
    
    public static void setupLangLocaleMaps(String[] languagesSupported, LinkedHashMap<String, String> langMap, LinkedHashMap<String, Locale> locMap)
    {
        for (int i = 0; i < languagesSupported.length; i++)
        {
            String currLang = languagesSupported[i];
            langMap.put(currLang, currLang);
            Locale currLocale = null;
            String[] parts = currLang.split("_");
            if(parts.length == 1)
            {
                currLocale = new Locale(parts[0]);
            }
            else if(parts.length == 2)
            {
                currLocale = new Locale(parts[0],parts[1]);
            }
            else 
            {
                throw new UnsupportedOperationException("TODO: locale support");
            }
            locMap.put(currLang, currLocale);
        }
    }

    /**
     * for the given locale - finds the standardized string name from the 'supportedLanguages' array above. 
     * could return null, if its not a supported language  
     * @param locale
     * @return
     */
    public static String findMatchForInternal(Locale locale, LinkedHashMap<String, String> langMap)
    {
        String matched = locale.toString();
        if(!langMap.containsKey(matched))
        {
            // hmm.. it did not exist ..
            matched = locale.getLanguage();
            if(!langMap.containsKey(matched))
            {
                // ok - unknown language..
                matched = null;
            }
        }
        return matched;
    }

    public static Locale getStandardLocale()
    {

        Locale javaLocale = Locale.getDefault();
        String matched = findMatchForInternal(javaLocale, languagesMap);
        if(matched == null)
        {
            matched = "en"; // default
        }
        return localeMap.get(matched);
    }
    //Defect 72713 : We will use default Fonts for ja  and zh
    //Check and compare the current Locale
    public static boolean testLocaleForFontSetting(Locale locale) 
    {
 	   Locale zh_CN = new Locale("zh","CN");
 	   Locale zh_TW = new Locale("zh","TW");
 	   Locale ja = new Locale("ja");
 	   Locale ja_JP = new Locale("ja","JP");
 	   if (zh_CN.equals(locale) || zh_TW.equals(locale) || ja.equals(locale) || ja_JP.equals(locale))
 		   return true;
 	   return false;
 	}
    
    public static void recognizeAllHosts()
    {
        HostnameVerifier hostnameVerifier = new HostnameVerifier()
        {
            public boolean verify(String urlHostName, SSLSession session)
            {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
    }

    public static void trustAllHosts()
    {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
        {
            public java.security.cert.X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }
            
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
            {
            }
            
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
            {
            }
        } };
        

        // Install the all-trusting trust manager
        try
        {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public static boolean _isInitedHTTPS = false;
    public static void initHTTPS()
    {
    	if(!_isInitedHTTPS)
    	{
    		_isInitedHTTPS = true;
        	String trustAll = System.getProperty(DSWEB_TRUST_ALL, System.getProperty(DSWEB_TRUST_ALL));
			if("TRUE".equalsIgnoreCase(trustAll))
			{
    			recognizeAllHosts();
    			trustAllHosts();
			}
    	}
    }

    public static HttpURLConnection getHTTPSConnection(String site, String trustStoreFile)
    {
        HttpURLConnection connection = null;
        try
        {
            if(null != trustStoreFile)
            {
                System.setProperty(SSL_TRUST_STORE_PROP, trustStoreFile);
            }
            URL url = new URL(site);
            connection = (HttpURLConnection) url.openConnection();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return connection;
    }

    public static boolean isHTTPS(String siteURL)
    {
    	if ((null == siteURL) || (siteURL.length() == 0))
    	{
    		return false; // wise guy, eh ?
    	}
    	// cannot use startsWith - because of case issues
    	String prefix = siteURL.substring(0,HTTPS_PREFIX.length());
    	if(HTTPS_PREFIX.equalsIgnoreCase(prefix))
    	{
    		return true;
    	}
    	return false;
    }
    

    public static HttpURLConnection makeGuardiumHTTPCall (String siteURL, String jsonRequestBody, String contentType,  String  authorization) throws IOException {
    	URL content = null;

    	debugLog("connecting to: ", siteURL);
    	content = new URL(siteURL ); 

    	HttpURLConnection connection = null;
    	boolean isHTTPS = isHTTPS(siteURL);
    	if(isHTTPS)
    	{
    		initHTTPS();
    		connection = getHTTPSConnection(siteURL, null);
    	}
    	else
    	{
    		connection = (HttpURLConnection) content.openConnection();
    	}

    	connection.setRequestMethod(METHOD_POST);
    	connection.setInstanceFollowRedirects(false);
    
    	connection.setRequestProperty(ACCEPT_LANGUAGE,  clientLocaleStr + ",en");
    	connection.setRequestProperty(USER_AGENT, DEFAULT_USER_AGENT);
    	connection.setRequestProperty(CONTENT_LENGTH, ""
    			+ Integer.toString(jsonRequestBody.getBytes().length));
    	connection.setRequestProperty(CONTENT_LANGUAGE, "en");
    	connection.setRequestProperty(ACCEPT_CHARSET, "utf-8");

    	// Tell the server we will be sending JSON content
    	connection.setRequestProperty(CONTENT_TYPE, contentType);
    	// Set the access token for the appropriate authorization
    	connection.setRequestProperty("Authorization", authorization);
    	


    	connection.setUseCaches(false);
    	connection.setDoInput(true);
    	connection.setDoOutput(true);             


    	DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
    	debugLog("writing jsonRequestBody: ", jsonRequestBody);
    	wr.writeBytes(jsonRequestBody);
    	wr.flush();
    	wr.close();

    	return connection;

    }

    
    public static HttpURLConnection makeHTTPCall( List<String> cookies, String siteURL, String urlParameters) throws IOException
    {
    	return makeHTTPCall(cookies, siteURL, urlParameters, null);
    }
    
    public static HttpURLConnection makeHTTPCall( List<String> cookies, String siteURL, String urlParameters,  Map<String, String> additionalHeaders) throws IOException
    {
    	return makeHTTPCall(cookies, siteURL, urlParameters, additionalHeaders, null);
    }
    
    public static HttpURLConnection makeHTTPCall( List<String> cookies, String siteURL, String urlParameters, Map<String, String> additionalHeaders, String requestMethod) throws IOException
    {
        URL content = null;

        debugLog("connecting to: ", siteURL);
        content = new URL(siteURL ); 
        
        HttpURLConnection connection = null;
        boolean isHTTPS = isHTTPS(siteURL);
        if(isHTTPS)
        {
        	initHTTPS();
        	connection = getHTTPSConnection(siteURL, null);
        }
        else
        {
        	connection = (HttpURLConnection) content.openConnection();
        }
        if(additionalHeaders!=null)
        {
            if(null == requestMethod)
            {
                requestMethod = METHOD_GET;
            }
        	for(String key: additionalHeaders.keySet())
	        {
        	    String val = additionalHeaders.get(key);
        	    if((null != val) && (val.length() > 0))
        	    {
        	        connection.setRequestProperty(key,val );
        	    }
	        }
        }
        else
        {
            additionalHeaders = new HashMap<String, String>();
        }
        if(null == requestMethod)
        {
            requestMethod = METHOD_POST;
        }
        
        connection.setRequestMethod(requestMethod);
        if((urlParameters == null) || (urlParameters.trim().length() == 0))
        {
            urlParameters = "";
        }
        if(!additionalHeaders.containsKey(CONTENT_TYPE))
        {
            connection.setRequestProperty(CONTENT_TYPE,DEFAULT_REQ_CONTENT_TYPE);
        }
        if(!additionalHeaders.containsKey(ACCEPT_LANGUAGE))
        {
            connection.setRequestProperty(ACCEPT_LANGUAGE,  clientLocaleStr + ",en");
        }
        if(!additionalHeaders.containsKey(USER_AGENT))
        {
            connection.setRequestProperty(USER_AGENT, DEFAULT_USER_AGENT);
        }
        if(!additionalHeaders.containsKey(CONTENT_LENGTH))
        {
            int len = urlParameters.getBytes("utf-8").length;
            connection.setRequestProperty(CONTENT_LENGTH, "" + len);
        }
        if(!additionalHeaders.containsKey(CONTENT_LANGUAGE))
        {
            connection.setRequestProperty(CONTENT_LANGUAGE, "en");
        }
        if(!additionalHeaders.containsKey(ACCEPT_CHARSET))
        {
            connection.setRequestProperty(ACCEPT_CHARSET, "utf-8");
        }

        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);    
        
        if((null != cookies) && (!cookies.isEmpty()))
        {
            String cookie = "";
            String sep = "";
            for (Iterator cookieIter = cookies.iterator(); cookieIter.hasNext();)
            {
                String currCookie = (String) cookieIter.next();
                 
                String[] cookieList = currCookie.split(";");
                for (String currCookieParam : cookieList) 
                {
                    String[] nvPair = currCookieParam.split("=");
                    if(nvPair.length >=2)
                    {
                        if(!"Path".equals(nvPair[0].trim())) // Path is a reserved cookie - we shouldn't resend it.. 
                        {
                            cookie = cookie + sep + nvPair[0] + "=" + nvPair[1];
                            sep = "; ";
                        }
                    }
                    else if(nvPair.length == 1)
                    {
                        cookie = cookie + sep + nvPair[0] ;
                        sep = "; ";
                    }
                }
                
                //cookie = cookie + sep + currCookie;
                sep = "; ";
            }
            //System.out.println("cookie : " + cookie);
            connection.setRequestProperty("Cookie", cookie);
        }
        
        if(urlParameters.length() > 0)
        {
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            debugLog("writing urlParameters: ", urlParameters);
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
        }
        return connection;
    }

    public static String readHTTPBuffer(HttpURLConnection connection)
    {
        return readHTTPBuffer(200, connection);
    }
    

    public static String readHTTPBuffer(int code, HttpURLConnection connection)
    {
        
        if(code != 408)
        {
            // Get and read the input stream.
            try
            {
                InputStream in = connection.getInputStream();            
                return readHTTPBuffer(connection, in);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return "";
    }
    
    public static String readHTTPErrorBuffer(HttpURLConnection connection)
    {
        InputStream in = connection.getErrorStream();            
        return readHTTPBuffer(connection, in);
    }

    public static String readHTTPBuffer(HttpURLConnection connection, InputStream in)
    {
        StringBuffer buffer = new StringBuffer();
        if(in == null){
        	connection.disconnect();
            debugLog("HTTPClientUtils.readHTTPBuffer: InputStream is null");
        	return "";
        }
        try
            {
                byte[] b = new byte[512];
                int numRead = 0;
                int totalNumRead = 0;
                do
                {
                    numRead = in.read(b);
                    if(numRead > 0)
                    {
                        totalNumRead += numRead;
                        String str = new String(b, 0, numRead, "UTF-8");
                        buffer.append(str);
                    }
                }
                while (numRead > 0 );
                in.close();
            }
            catch (Throwable e)
            {
                buffer.append(e);
            }
            finally {
            	//Ensure HTTP connection is always closed at the end of the request
            	connection.disconnect();
            }
        String resp = buffer.toString();
        debugLog("readHTTPBuffer: ", resp);
        return resp;
    }

    public static boolean downloadFile(String fileSRCURL, OutputStream destStream, 
                                    String urlParameters, Map<String, String> additionalHeaders) throws IOException
    {
        boolean success = true;
    
        URL content = null;
        
        
        try
        {
            content = new URL(fileSRCURL);
        }
        catch (MalformedURLException exception)
        {
            throw new IOException(fileSRCURL + " is a malformed url.");
        }
        
        
        HttpURLConnection connection = null;
        
        try
        {
            connection = HTTPClientUtils.makeHTTPCall(null, fileSRCURL, urlParameters, additionalHeaders);
        }
        catch (IOException exception)
        {
            success = false;
            throw new IOException("Problem opening " + fileSRCURL + ": "
                                  + exception.toString());
        }
    

        try
        {
            InputStream in = connection.getInputStream();
            
            byte[] b = new byte[512];
            int numRead = 0;
            int totalNumRead = 0;
            do
            {
                numRead = in.read(b);
                if(numRead > 0)
                {
                    totalNumRead += numRead;
                    if(null != destStream)
                    {
                        destStream.write(b,0,numRead);
                    }
                }
            }
            while (numRead > 0 );
            in.close();
            connection.disconnect();
        }
        catch (IOException e)
        {
            success = false;
            throw e;
        }
        
     return success;   
    }
    
    public static boolean uploadFile(String fileSRCURL, String fullFile, Map<String, String> additionalHeaders) throws IOException
    {
    	boolean success = false;

    	URL content = null;


    	try
    	{
    		content = new URL(fileSRCURL+fullFile.substring(fullFile.lastIndexOf("/")+1));
    	}
    	catch (MalformedURLException exception)
    	{
    		throw new IOException(fileSRCURL + " is a malformed url.");
    	}


    	HttpURLConnection connection = null;
    	int responseCode;
    	
        try
        {
            connection = (HttpURLConnection) content.openConnection();
            
        	for(String key: additionalHeaders.keySet())
	        {
        	    String val = additionalHeaders.get(key);
        	    if((null != val) && (val.length() > 0))
        	    {
        	        connection.setRequestProperty(key,val );
        	    }
	        }
        	connection.setRequestMethod("PUT");
        	connection.setDoOutput(true);
            
        }
        catch (IOException exception)
        {
            success = false;
            throw new IOException("Problem opening " + fileSRCURL + ": "
                                  + exception.toString());
        }
        
        try {
        	
        	PrintWriter writer = null;
        	try {
			
        		File uploadFile = new File(fullFile);
        		
        		writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));

        		BufferedReader reader = null;
        		try {
        			reader = new BufferedReader(new InputStreamReader(new FileInputStream(uploadFile), "UTF-8"));
        			for (String line; (line = reader.readLine()) != null;) {
        				writer.println(line);
        			}
        			
        		} finally {
        			if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
        		}

        	} finally {
        		if (writer != null) writer.close();
        	}
        }
        catch (IOException e)
        {
            success = false;
            throw e;
        }
             
        responseCode = connection.getResponseCode();
		connection.disconnect();

		if ((responseCode == 200)||(responseCode == 201)) {
        	
        	success = true; 
        }
        
    	return success;
    }


    public static final String  SRC_ID = "fileSRCURL";
    
    public static final String  TGT_ID = "fileTGTID";
    
    public static final String  ENCODING  = "UTF-8";
    
    public final static String DSWEB_TMP_DIR = "dsweb.tmp.dir";
    public final static String JAVA_TMP_DIR = "java.io.tmpdir";
    

    public static String mapToQryString(Map<String, String> qryParams)
    {
        String amp = "";
        StringBuffer qryParamsBuf = new StringBuffer();
        if(null != qryParams)
        {
            Set keys = qryParams.keySet();
            for (Iterator keysIter = keys.iterator(); keysIter.hasNext();)
            {
                String currKey = (String) keysIter.next();
                String currValue = qryParams.get(currKey);
                String encValue = currValue;
                try
                {
                    encValue = URLEncoder.encode(currValue,"UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                    encValue = currValue;
                }
                
                String p = amp + currKey + "=" + encValue ;
                qryParamsBuf.append(p);
                
                amp = "&";
            }
        }
       return qryParamsBuf.toString();
    }
    
    public static String mapListToQryString(Map<String, List<String>> qryParams)
    {
        String amp = "";
        StringBuffer qryParamsBuf = new StringBuffer();
        if(null != qryParams)
        {
            Set keys = qryParams.keySet();
            for (Iterator keysIter = keys.iterator(); keysIter.hasNext();)
            {
                String currKey = (String) keysIter.next();
                ArrayList<String> currValues = (ArrayList<String>) qryParams.get(currKey);
                for(String currValue: currValues)
                {
	                String encValue = currValue;
	                try
	                {
	                    encValue = URLEncoder.encode(currValue,"UTF-8");
	                }
	                catch (UnsupportedEncodingException e)
	                {
	                    e.printStackTrace();
	                }
	                
	                String p = amp + currKey + "=" + encValue ;
	                qryParamsBuf.append(p);
	                
	                amp = "&";
                }
            }
        }
       return qryParamsBuf.toString();
    }
    
    public static String toJSON(Map<String, String>props)
    {
        StringBuffer jsonStr = new StringBuffer();
        
        jsonStr.append("{");
        
        String app = "";
        Set keySet = props.keySet();
        for (Iterator keysIter = keySet.iterator(); keysIter.hasNext();)
        {
            String currKey = (String) keysIter.next();
            String currVal = props.get(currKey);
            jsonStr.append(app);
            jsonStr.append(DQUOTE + currKey  + DQUOTE );
            jsonStr.append(COLON + ' ' + DQUOTE + currVal  + DQUOTE );
            app = COMMA;
        }
        
        jsonStr.append("}");
        return jsonStr.toString();
    }
    
    public static Properties loadFromFile(String propsFileName) throws FileNotFoundException,IOException
    {
        Properties props = new Properties();
        
        File mfile = new File(propsFileName);
        
        if (mfile.canRead())
        {
            FileInputStream propFile = new FileInputStream(mfile);
            props.load(propFile);
        }
        return props;
    }
    
    public static BufferedReader openFileReader(String fileName) throws FileNotFoundException,IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        return br;
    }

    public static String readLine(BufferedReader br)
    {
        String buff = null;
        if(null != br)
        {
            try
            {
                buff = br.readLine();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                buff = null;
            }
        }
        return buff;
    }
    
    public static void closeFileReader(BufferedReader br)
    {
        if(null != br)
        {
            try
            {
                br.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    /**
     * return a list of strings from 'str' that are delimited by 'delim'
     * note - I have had trouble with java.lang.String's split() method with multi-byte chars.. so.. 
     * @param str
     * @param delim
     * @return
     */
    public static List<String> splitString(String str, String delim)
    {
        List<String> results = new ArrayList<String>();
        
        int idx = -1;
        int sIdx = 0;
        do
        {
            idx = str.indexOf(delim,sIdx);
            if(idx >= 0)
            {
                String currResult = str.substring(sIdx, idx);
                results.add(currResult);
                sIdx = idx + 1;
            }
        }
        while(idx >= 0);
        // any left over
        if(sIdx < str.length())
        {
            results.add(str.substring(sIdx));
        }
        
        return results;
    }

    

    public static ResourceBundle nlLoadFromClassPath(String propsFileName)
    {
        Locale stdLocale = getStandardLocale();
        ResourceBundle resBundle = null;
        try
        {
            resBundle = ResourceBundle.getBundle(propsFileName, stdLocale);
        }
        catch (Exception e)
        {
            doLog("nlLoadFromClassPath failure:" + e);
        }
        
        return resBundle;
    }

    public static final String PROPERTIESFILE_SUFFIX = ".properties";
    
    
    public static Properties nlLoadFromFileLocation(String propsBaseFileName)
    {
        Locale stdLocale = getStandardLocale();
        Properties allProps = null;
        try
        {
            allProps = loadFromFile(propsBaseFileName + PROPERTIESFILE_SUFFIX); 
            Properties nlProps = loadFromFile(propsBaseFileName + "_" + clientLocaleStr + PROPERTIESFILE_SUFFIX);
            allProps.putAll(nlProps);
        }
        catch (Exception e)
        {
            doLog("nlLoadFromFileLocation failure:" + e);
        }
        
        if(null == allProps)
        {
            allProps = new Properties();
        }
        return allProps;

    }

    public static String getNLMessage(Properties props, String key, String... args)
    {
        if(null == args)
        {
            args = new String[0];
        }
        String nlMsg = null;
        if(null != props)
        {
            try
            {
                nlMsg = props.getProperty(key);
            }
            catch (Exception e)
            {
                doLog("getNLMessage getProperty failure:" + key);
            }
        }
        if(null == nlMsg)
        {
            nlMsg = key + Arrays.toString(args);
        }
        else if(args.length >= 1)
        {
            try
            {
                nlMsg = MessageFormat.format(nlMsg, args );
            }
            catch (Exception e)
            {
                doLog("getNLMessage format failure:" + key);
            }
        }
        
        return nlMsg;
    }
    
    public static String getNLMessage(ResourceBundle res, String key, String... args)
    {
        if(null == args)
        {
            args = new String[0];
        }
        String nlMsg = null;
        if(null != res)
        {
            try
            {
                nlMsg = res.getString(key);
            }
            catch (Exception e)
            {
                doLog("getNLMessage getString failure:" + key);
            }
        }
        if(null == nlMsg)
        {
            nlMsg = key + args;
        }
        else if(args.length >= 1)
        {
            try
            {
                nlMsg = MessageFormat.format(nlMsg, (Object[])args );
            }
            catch (Exception e)
            {
                doLog("getNLMessage format failure:" + key);
            }
        }
        
        return nlMsg;
    }
    

    // simplistic parsing of cmd line args of this pattern  -argKey1 val1 -argKey2 val2 etc..
    public static Map<String, String> parseArgs(String[] args)
    {
        return parseArgs(args,new HashMap());
    }
    
    public static Map<String, String> parseArgs(String[] args, Map<String,String> noArgValueListMap)
    {
        Map<String, String> argMap = new HashMap<String, String>();
        for (int i = 0; i < args.length; )
        {
            String currArg = args[i];
            if(currArg.startsWith("-"))
            {
                currArg = currArg.substring(1);
            }
            else
            {
                i++;
                continue; 
            }
            String argName = currArg;
            String argValue = null;
            boolean skipVal = false;
            if(noArgValueListMap.containsKey(argName))
            {
                skipVal = true;
            }
            if(skipVal)
            {
                argValue = "";
            }
            else
            {
                int valIdx = i + 1;
                if(valIdx < args.length)
                {
                    argValue = args[valIdx];
                }
                if(null != argName)
                {
                    argName = argName.trim();
                }
                if(null != argValue)
                {
                    argValue = argValue.trim();
                }
                else
                {
                    argValue = "";
                }
            }

            argMap.put(argName, argValue);
            if(skipVal)
            {
                i = i + 1;
            }
            else
            {
                i = i + 2;
            }
        }
        return argMap;
    }

    public static String consolePrompt(String promptStr, boolean isPwd)
    {
        String val = null;
        
        if(null != System.console())
        {
            if(isPwd)
            {
                char[] pass = System.console().readPassword("%s :", promptStr);
                val = new String(pass);
            }
            else
            {
                val = System.console().readLine("%s : ", promptStr);
            }
        }
        else
        {
            System.out.println(promptStr + " :");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            try
            {
                val = in.readLine();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return val.trim();
    }
    
    public static String promptWithNoEchoAndConfirm(String promptStr, String reEnterPromptStr)
    {
        String password = null;
        
        String passStr1 = consolePrompt(promptStr, true);
        if(null == reEnterPromptStr)
        {
            // do not need to prompt twice
            password = passStr1;
        }
        else
        {
            String passStr2 = consolePrompt(reEnterPromptStr, true);

            
            if(passStr1.equals(passStr2))
            {
                 password = passStr1;
            }
            else
            {
                throw new IllegalArgumentException(INPUT_STRINGS_DO_NOT_MATCH);
            }
        }
        return password;
    }

    public static Thread[] pipeInOutErr(Process process,InputStream stdin, PrintStream stdout, PrintStream stderr)
    {
        Thread[] threads = new Thread[3];
        threads[0] = pipein(stdin, process.getOutputStream());
        threads[1] = pipeOut(process.getInputStream(), stdout);
        threads[2] = pipeOut(process.getErrorStream(), stderr);
        return threads;
    }
    
    public static Thread pipeOut(final InputStream src, final PrintStream dest)
    {
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    int c = src.read();
                    while(c != -1)
                    {
                        dest.write(c);
                        dest.flush();
                        c = src.read();
                    }
                }
                catch (IOException e)
                { // just exit
                }
            }
        });
        t.start();
        return t;
    }

    public static Thread pipein(final InputStream src, final OutputStream dest)
    {
        
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
     
                    InputStreamReader br = (new InputStreamReader(src));
                    int ret = -1;
                    do
                    {
                        ret = br.read();
                        dest.write(ret);
                        dest.flush();
                        while(!br.ready())
                        {
                            Thread.sleep(200);
                        }
                    }
                    while(ret !=  -1);
                }
                catch (Exception e)
                {
                    
                }
            }
        });
        t.start();
        return t;
    }

    public static final String CLEAR_SCREEN = "clear";
    public static final String CLEAR_SCREEN_WIN= "cls";

    public static String os = System.getProperty("os.name");
    public static boolean isWindows = os.contains("indows"); //who knows --W or w may be present

    public static  void clearScreen()
    {
        String clearCommand = CLEAR_SCREEN;
        if(isWindows)
        {
            clearCommand = CLEAR_SCREEN_WIN;
        }
        try
        {
            Runtime.getRuntime().exec(clearCommand);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static final String RESULTCODE_SUCCESS = "resultCode':'success'";
    public static final String RESPONSE_MSG_PREFIX = "message:'";
    public static final String RESPONSE_MSG_SUFFIX = "'";

    // cheap way to identify success or failure -- one find day will get JSON lib here too..
    
    public static boolean isSuccessful(String httpResponse, StringBuffer outMsg)
    {
        
        if(httpResponse.contains(RESULTCODE_SUCCESS.replace('\'', '"')))
        {
            return true;
        }
        else
        {
            String failureMsg = null;
            int msgStart = httpResponse.indexOf(RESPONSE_MSG_PREFIX);
            if(msgStart > 0)
            {
                msgStart = msgStart + RESPONSE_MSG_PREFIX.length();
                int msgEnd = httpResponse.indexOf(RESPONSE_MSG_SUFFIX, msgStart);
                if(msgEnd > 0)
                {
                    failureMsg = httpResponse.substring(msgStart, msgEnd);
                }
            }
            if(null == failureMsg)
            {
                failureMsg =  "FAILURE: \n" + httpResponse;
            }
            outMsg.append(failureMsg);
            return false;
        }
    }

    public static void unzip(String zipFileName, String outputFolder) throws IOException
    {
        
        byte[] buffer = new byte[1024];
    
        try
        {
            ZipFile zipFile = new ZipFile(zipFileName);
            Enumeration<?> enu = zipFile.entries();
            while (enu.hasMoreElements())
            {
                ZipEntry zipEntry = (ZipEntry) enu.nextElement();
                
                String name = zipEntry.getName();
                long size = zipEntry.getSize();
                long compressedSize = zipEntry.getCompressedSize();
                
                File file = new File(outputFolder + File.separator + name);
                if (name.endsWith("/"))
                {
                    file.mkdirs();
                    continue;
                }
                
                File parent = file.getParentFile();
                if (parent != null)
                {
                    parent.mkdirs();
                }
                
                InputStream is = zipFile.getInputStream(zipEntry);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = is.read(bytes)) >= 0)
                {
                    fos.write(bytes, 0, length);
                }
                is.close();
                fos.close();
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }

}
