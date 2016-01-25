package ru.terralink.ws.msuim.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.terralink.ws.msuim.REAttrDataExchangeOut;
import ru.terralink.ws.object.REAttrDataExchangeResponse;

import javax.jws.WebService;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ru.terralink.ws.msuim.constant.REAttrDataExchangeOutConstant.*;

/**
 * Created by AzarovD on 19.01.2016.
 */
@WebService(endpointInterface = "ru.terralink.ws.msuim.impl.REAttrDataExchangeOutService")
public class REAttrDataExchangeOutService implements REAttrDataExchangeOut {

    private static final Logger logger = LoggerFactory.getLogger(REAttrDataExchangeOutService.class.getSimpleName());
    private static final String FUNCTION_NAME = "msuimsync.setMsuimResponse";
    private static final String FUNCTION_PREFIX = "func";
    private static final String SEPARATOR = "=";
    private static final String AND = "&";
    private static final String UTF_8 = "UTF-8";

    @Override
    public String printMessage(REAttrDataExchangeResponse reAttrDataExchangeResponse) {

        if (reAttrDataExchangeResponse == null) {
            logger.error("Argument REAttrDataExchangeResponse = null.");
        }

        Map<String, Object> reAttrExchangeResponse = buildReAttrExchangeResponse(reAttrDataExchangeResponse);

        byte[] postData = convertToPostDataFormat(reAttrExchangeResponse);
        int postDataLength = postData.length;
        String request = "http://srv-phg-ukd3.itps.local/OTCS/cs.exe";

        try {
            URL url = new URL(request);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(postData);
            wr.flush();
            wr.close();

            int responseCode = conn.getResponseCode();
            System.out.println("POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                System.out.println(response.toString());
            } else {
                String location = conn.getHeaderField("Location");
                System.out.println("POST request not worked");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Map<String, Object> buildReAttrExchangeResponse(REAttrDataExchangeResponse reAttrDataExchangeResponse) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put(LOGICAL_SYSTEM, reAttrDataExchangeResponse.getSuccess());
        response.put(INTERNAL_OBJECT_NUMBER, reAttrDataExchangeResponse.getInternalObjectNumber());
        response.put(EXTERNAL_OBJECT_NUMBER, reAttrDataExchangeResponse.getExternalObjectNumber());
        response.put(TRAY_CODE, reAttrDataExchangeResponse.getTrayCode());
        response.put(SUCCESS, reAttrDataExchangeResponse.getSuccess());
        response.put(DESTINATION, reAttrDataExchangeResponse.getDestination());
        response.put(PARENT_MSG_ID, reAttrDataExchangeResponse.getParentMsgID());

        if (reAttrDataExchangeResponse.getFiles() != null && !reAttrDataExchangeResponse.getFiles().getFile().isEmpty()) {
            for (REAttrDataExchangeResponse.Files.File file : reAttrDataExchangeResponse.getFiles().getFile()) {

            }
        }
        return response;
    }

    private byte[] convertToPostDataFormat(Map<String, Object> params) {
        StringBuilder strPostRequest = new StringBuilder();

        if (params == null || params.isEmpty()) {
            logger.error("Faild build Http Request. The incomig params is null.");
            return null;
        }

        try {
            strPostRequest.append(URLEncoder.encode(FUNCTION_PREFIX, UTF_8)).append(SEPARATOR).append(URLEncoder.encode(FUNCTION_NAME, UTF_8));
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed encode for " + FUNCTION_PREFIX + SEPARATOR + FUNCTION_NAME);
        }

        Iterator<Map.Entry<String, Object>> entryIterator = params.entrySet().iterator();

        while (entryIterator.hasNext()) {
            logger.info("Building params query:");
            Map.Entry<String, Object> entry = entryIterator.next();
            String entryKey = entry.getKey();
            String entryValue = (String) entry.getValue();
            logger.info("Param " + entryKey + " = " + entryValue);
            strPostRequest.append(AND);
            try {
                strPostRequest.append(URLEncoder.encode(entryKey, UTF_8));
                strPostRequest.append(SEPARATOR);
                strPostRequest.append(URLEncoder.encode(entryValue, UTF_8));
            } catch (UnsupportedEncodingException e) {
                logger.error("Failed encode params:  " + e);
            }
        }
        logger.info("Result: \n" + strPostRequest.toString());
        return strPostRequest.toString().getBytes();
    }
}
