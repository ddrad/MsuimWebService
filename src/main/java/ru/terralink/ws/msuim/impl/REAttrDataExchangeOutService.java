package ru.terralink.ws.msuim.impl;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.terralink.ws.msuim.REAttrDataExchangeOut;
import ru.terralink.ws.object.REAttrDataExchangeResponse;

import javax.jws.WebService;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Map;

import static ru.terralink.ws.msuim.constant.REAttrDataExchangeOutConstant.*;

/**
 * Created by AzarovD on 19.01.2016.
 */
@WebService(endpointInterface = "ru.terralink.ws.msuim.impl.REAttrDataExchangeOutService")
public class REAttrDataExchangeOutService implements REAttrDataExchangeOut {

    private static final Logger logger = LoggerFactory.getLogger(REAttrDataExchangeOutService.class.getSimpleName());

    @Override
    public String printMessage(REAttrDataExchangeResponse reAttrDataExchangeResponse) {

        if (reAttrDataExchangeResponse == null) {
            logger.error("Argument REAttrDataExchangeResponse = null.");
        }

        JSONObject reAttrExchangeResponse = buildReAttrExchangeResponse(reAttrDataExchangeResponse);
        byte[] httPostRequest = createHttPostRequest(reAttrExchangeResponse);

        int postDataLength = httPostRequest.length;
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
            wr.write(httPostRequest);
            wr.flush();
            wr.close();

            int responseCode = conn.getResponseCode();
            logger.info("POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                logger.info("Pass arguments: " + response.toString());
            } else {
                String location = conn.getHeaderField("Location");
                logger.error("POST request not worked. \nThe header field is " + location);
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

    private JSONObject buildReAttrExchangeResponse(REAttrDataExchangeResponse reAttrDataExchangeResponse) {
        JSONObject response = new JSONObject();
        JSONObject jsonFileObject = new JSONObject();
        JSONArray jsonFilesObject = new JSONArray();

        response.put(LOGICAL_SYSTEM, reAttrDataExchangeResponse.getSuccess());
        response.put(INTERNAL_OBJECT_NUMBER, reAttrDataExchangeResponse.getInternalObjectNumber());
        response.put(EXTERNAL_OBJECT_NUMBER, reAttrDataExchangeResponse.getExternalObjectNumber());
        response.put(TRAY_CODE, reAttrDataExchangeResponse.getTrayCode());
        response.put(SUCCESS, reAttrDataExchangeResponse.getSuccess());
        response.put(DESTINATION, reAttrDataExchangeResponse.getDestination());
        response.put(PARENT_MSG_ID, reAttrDataExchangeResponse.getParentMsgID());

        if (reAttrDataExchangeResponse.getFiles() != null && !reAttrDataExchangeResponse.getFiles().getFile().isEmpty()) {
            for (REAttrDataExchangeResponse.Files.File file : reAttrDataExchangeResponse.getFiles().getFile()) {
                jsonFileObject.put(FILE_NAME, file.getFileName());
                jsonFileObject.put(FILE_SIZE, file.getFileSize());
                jsonFileObject.put(INTERNAL_FILE_ID, file.getInternalFileID());
                jsonFileObject.put(EXTERNAL_FILE_ID, file.getExternalFileID());
                jsonFileObject.put(CURRENT_PART, file.getCurrentPart());
                jsonFileObject.put(ALL_PARTS, file.getAllParts());
                jsonFileObject.put(LOCATION_SUIM, file.getLocationSUIM());
                jsonFileObject.put(LOCATION_ELAR, file.getLocationELAR());
                REAttrDataExchangeResponse.Files.File.ErrorText errorText = file.getErrorText();
                fillFileErrorText(errorText, jsonFileObject);

                jsonFilesObject.put(jsonFileObject);
            }
        }

        response.put(FILES, jsonFilesObject);

        fillErrorText(reAttrDataExchangeResponse, response);

        return response;
    }

    private void fillErrorText(REAttrDataExchangeResponse reAttrDataExchangeResponse, JSONObject response) {
        REAttrDataExchangeResponse.ErrorText errorText = reAttrDataExchangeResponse.getErrorText();
        if (errorText != null && !errorText.getLine().isEmpty()) {
            JSONArray jsonFileLine = new JSONArray();
            for (String line : errorText.getLine()) {

                jsonFileLine.put(line);
            }
            response.put(ERROR_TEXT, jsonFileLine);
        }
    }

    private void fillFileErrorText(REAttrDataExchangeResponse.Files.File.ErrorText errorText, JSONObject jsonObject) {
        if (errorText != null && !errorText.getLine().isEmpty()) {
            JSONArray jsonFileLine = new JSONArray();
            for (String line : errorText.getLine()) {
                jsonFileLine.put(line);
            }
            jsonObject.put(ERROR_TEXT, jsonFileLine);
        }
    }

    private byte[] createHttPostRequest(JSONObject jsonObject) {
        StringBuilder strPostRequest = new StringBuilder();

        if (jsonObject == null) {
            logger.error("Faild build Http Request. The incomig params is null.");
            return null;
        }

        try {
            strPostRequest.append(URLEncoder.encode(FUNCTION_PREFIX, UTF_8)).append(SEPARATOR).append(URLEncoder.encode(FUNCTION_NAME, UTF_8));
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed encode for " + FUNCTION_PREFIX + SEPARATOR + FUNCTION_NAME);
        }

        strPostRequest.append(AND);
        try {
            strPostRequest.append(URLEncoder.encode(CONTENT, UTF_8));
            strPostRequest.append(SEPARATOR);
            strPostRequest.append(URLEncoder.encode(jsonObject.toString(), UTF_8));
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed encode params:  " + e);
        }

        logger.info("Result: \n" + strPostRequest.toString());
        return strPostRequest.toString().getBytes();
    }
}
