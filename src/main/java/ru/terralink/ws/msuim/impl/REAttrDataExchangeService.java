package ru.terralink.ws.msuim.impl;

import com.opentext.ecm.api.OTAuthentication;
import com.opentext.livelink.service.core.*;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.DocumentManagement_Service;
import com.opentext.ecm.services.authws.AuthenticationException_Exception;
import com.opentext.ecm.services.authws.AuthenticationService;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.developer.JAXWSProperties;
import com.sun.xml.ws.developer.WSBindingProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.terralink.ws.msuim.AttachmentInfo;
import ru.terralink.ws.msuim.OpenTextAdapter;
import ru.terralink.ws.msuim.REAttrDataExchange;
import ru.terralink.ws.object.request.REDataExchangeAttrECD;
import ru.terralink.ws.object.request.REDataExchangeAttrFile;
import ru.terralink.ws.object.response.REAttrDataExchangeResponse;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import static ru.terralink.ws.msuim.constant.REAttrDataExchangeOutConstant.*;

/**
 * Created by AzarovD on 19.01.2016.
 */
@Component("reAttrDataExchangeService")
@WebService(endpointInterface = "ru.terralink.ws.msuim.impl.REAttrDataExchangeService")
public class REAttrDataExchangeService implements REAttrDataExchange {

    private static final Logger logger = LoggerFactory.getLogger(REAttrDataExchangeService.class.getSimpleName());
    @Autowired
    @Qualifier("openTextAdapter")
    private OpenTextAdapter openTextAdapter;
    @Autowired
    @Qualifier("attachmentInfo")
    private AttachmentInfo attachmentInfo;

    @Override
    public String sendReAttrDataExchangeResponse(REAttrDataExchangeResponse reAttrDataExchangeResponse) {

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
        response.put(RESULT_SUCCESS, reAttrDataExchangeResponse.getSuccess());
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

        logger.info("Request: \n" + strPostRequest.toString());
        return strPostRequest.toString().getBytes();
    }

    @Override
    public void sendReAttrDataExchangeMessage(REDataExchangeAttrECD reAttrDataExchangeMessage) {
        logger.info("Run REAttrDataExchangeService...");

        String contentType = attachmentInfo.getContentType().equals(TEXT_PLAIN) ? OCTET_STREAM : attachmentInfo.getContentType();
        logger.info("Content Type : " + contentType);

        BufferedInputStream inputStream = new BufferedInputStream(attachmentInfo.getInputStream());
        byte[] content = toByteArray(inputStream);
        long inputStreamLength = content.length;
        logger.info("Content size : " + inputStreamLength);

        if (CollectionUtils.isEmpty(reAttrDataExchangeMessage.getAttrFile())) {
            logger.error("Attribute AttrFile not found!");
            throw new RuntimeException("Attribute AttrFile not found!");
        }

        REDataExchangeAttrFile attrFile = reAttrDataExchangeMessage.getAttrFile().iterator().next();
        String fileName = attrFile.getFILENAME();
        logger.info("File Name : " + fileName);
        XMLGregorianCalendar datum = attrFile.getDATUM();

        String otdsAuthToken = getOTDSAuthToken();
        logger.info(RESULT_SUCCESS);

        Authentication authClient = getAuthenticationClient();
        logger.info(RESULT_SUCCESS);

        String csAuthToken = validateOTDSAuthToken(authClient, otdsAuthToken);

        DocumentManagement docManClient = getDocumentManagement();
        logger.info(RESULT_SUCCESS);
        OTAuthentication otAuth = new OTAuthentication();
        otAuth.setAuthenticationToken(csAuthToken);

        fillSoapHeader((WSBindingProvider) docManClient, otAuth);

        String contextID = generateContextId(docManClient, reAttrDataExchangeMessage.getHeader().getObjectNumber(), fileName);
        logger.info(RESULT_SUCCESS);

        ContentService contentServiceClient = getContentServiceClient();
        logger.info(RESULT_SUCCESS);

        ((BindingProvider) contentServiceClient).getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, CHUNK_SIZE);

        FileAtts fileAtts = createFileAtts(fileName, inputStreamLength, datum);

        fillSoapHeader(otAuth, contextID, (WSBindingProvider) contentServiceClient, fileAtts);

        try {
            logger.info("Uploading document...");
            String objectID = contentServiceClient.uploadContent(new DataHandler(content, contentType));
            logger.info(RESULT_SUCCESS + "\nNew document uploaded with ID = " + objectID);
        } catch (SOAPFaultException e) {
            logger.error(RESULT_FAILED + "\n" + e.getFault().getFaultCode() + " : " + e.getMessage());
            throw new RuntimeException(e.getFault().getFaultCode() + " : " + e.getMessage());
        }
    }

    private String getOTDSAuthToken() {
        logger.info("Create the client proxies for each OTDS Authentication service...");
        try {
            URL authWsdl = new URL(openTextAdapter.getOtdsAuthenticationWsdlUrl());
            AuthenticationService otdsAuthService = new AuthenticationService(authWsdl);
            com.opentext.ecm.services.authws.Authentication otdsAuthClient = otdsAuthService.getAuthenticationPort();
            System.out.print("Authenticating User...");
            return otdsAuthClient.authenticate(openTextAdapter.getUser(), openTextAdapter.getPassword());
        } catch (AuthenticationException_Exception e) {
            logger.error(RESULT_FAILED + "\n" + e.getFaultInfo().getFaultCode() + " : " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private Authentication getAuthenticationClient() {
        logger.info("Create the client proxies for each Authentication service...");
        try {
            URL authWsdl = new URL(openTextAdapter.getAuthenticationWsdlUrl());
            Authentication_Service authService = new Authentication_Service(authWsdl);
            return authService.getBasicHttpBindingAuthentication();
        } catch (SOAPFaultException e) {
            logger.error(RESULT_FAILED + "\n" + e.getFault().getFaultCode() + " : " + e.getMessage());
            throw new RuntimeException(e.getFault().getFaultCode() + " : " + e.getMessage());
        } catch (MalformedURLException e) {
            logger.error(RESULT_FAILED + "\n" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private String validateOTDSAuthToken(Authentication authClient, String otdsAuthToken) {
        logger.info("Validate OTDS Authentication Token...");
        try {
            return authClient.validateUser(otdsAuthToken);
        } catch (SOAPFaultException e) {
            logger.error(RESULT_FAILED + "\n" + e.getFault().getFaultCode() + " : " + e.getMessage());
            throw new RuntimeException(e.getFault().getFaultCode() + " : " + e.getMessage());
        }
    }

    private DocumentManagement getDocumentManagement() {
        try {
            logger.info("Create the client proxies for each DocumentManagement service...");
            URL docManager = new URL(openTextAdapter.getDocManagementWsdlUrl());
            DocumentManagement_Service docManService = new DocumentManagement_Service(docManager);
            return docManService.getBasicHttpBindingDocumentManagement();
        } catch (MalformedURLException e) {
            logger.error(RESULT_FAILED + "\n" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private void fillSoapHeader(WSBindingProvider bindingProvider, OTAuthentication otAuth) {
        try {
            SOAPHeader header = MessageFactory.newInstance().createMessage().getSOAPPart().getEnvelope().getHeader();
            SOAPHeaderElement otAuthElement = header.addHeaderElement(new QName(ECM_API_NAMESPACE, "OTAuthentication"));
            SOAPElement authTokenElement = otAuthElement.addChildElement(new QName(ECM_API_NAMESPACE, "AuthenticationToken"));
            authTokenElement.addTextNode(otAuth.getAuthenticationToken());
            bindingProvider.setOutboundHeaders(Headers.create(otAuthElement));
        } catch (SOAPException e) {
            logger.error(RESULT_FAILED + " to set authentication SOAP header!\n" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private void fillSoapHeader(OTAuthentication otAuth, String contextID, WSBindingProvider contentServiceClient, FileAtts fileAtts) {
        try {
            SOAPHeader header = MessageFactory.newInstance().createMessage().getSOAPPart().getEnvelope().getHeader();
            SOAPHeaderElement otAuthElement = header.addHeaderElement(new QName(ECM_API_NAMESPACE, "OTAuthentication"));
            SOAPElement authTokenElement = otAuthElement.addChildElement(new QName(ECM_API_NAMESPACE, "AuthenticationToken"));
            authTokenElement.addTextNode(otAuth.getAuthenticationToken());
            SOAPHeaderElement contextIDElement = header.addHeaderElement(new QName(CORE_NAMESPACE, "contextID"));
            contextIDElement.addTextNode(contextID);
            SOAPHeaderElement fileAttsElement = header.addHeaderElement(new QName(CORE_NAMESPACE, "fileAtts"));
            SOAPElement createdDateElement = fileAttsElement.addChildElement(new QName(CORE_NAMESPACE, "CreatedDate"));
            createdDateElement.addTextNode(fileAtts.getCreatedDate().toString());
            SOAPElement modifiedDateElement = fileAttsElement.addChildElement(new QName(CORE_NAMESPACE, "ModifiedDate"));
            modifiedDateElement.addTextNode(fileAtts.getModifiedDate().toString());
            SOAPElement fileSizeElement = fileAttsElement.addChildElement(new QName(CORE_NAMESPACE, "FileSize"));
            fileSizeElement.addTextNode(fileAtts.getFileSize().toString());
            SOAPElement fileNameElement = fileAttsElement.addChildElement(new QName(CORE_NAMESPACE, "FileName"));
            fileNameElement.addTextNode(fileAtts.getFileName());
            List<Header> headers = new ArrayList<Header>();
            headers.add(Headers.create(otAuthElement));
            headers.add(Headers.create(contextIDElement));
            headers.add(Headers.create(fileAttsElement));
            contentServiceClient.setOutboundHeaders(headers);
        } catch (SOAPException e) {
            logger.error(RESULT_FAILED + " to set SOAP headers!\n" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private String generateContextId(DocumentManagement docManClient, String objectNumber, String fileName) {
        try {
            logger.info("Generating context ID...");
            long parentId = Long.parseLong(objectNumber);
            return docManClient.createDocumentContext(parentId, fileName, null, false, null);
        } catch (SOAPFaultException e) {
            logger.error(RESULT_FAILED + "\n" + e.getFault().getFaultCode() + " : " + e.getMessage());
            throw new RuntimeException(e.getFault().getFaultCode() + " : " + e.getMessage());
        }
    }

    private ContentService getContentServiceClient() {
        logger.info("Create the client proxies for each ContentService service...");
        try {
            URL contentServiceWsdlUrl = new URL(openTextAdapter.getContentserviceWsdlUrl());
            ContentService_Service contentService = new ContentService_Service(contentServiceWsdlUrl);
            return contentService.getBasicHttpBindingContentService(new MTOMFeature());
        } catch (MalformedURLException e) {
            logger.error(RESULT_FAILED + "\n" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private FileAtts createFileAtts(String fileName, long length, XMLGregorianCalendar datum) {
        FileAtts fileAtts = new FileAtts();
        fileAtts.setCreatedDate(datum);
        fileAtts.setFileName(fileName);
        fileAtts.setFileSize(length);
        fileAtts.setModifiedDate(datum);
        return fileAtts;
    }

    public byte[] toByteArray(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int reads = 0;
        try {
            reads = is.read();
            while (reads != -1) {
                baos.write(reads);
                reads = is.read();
            }
            baos.flush();
            baos.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return baos.toByteArray();
    }


    public void setAttachmentInfo(AttachmentInfo attachmentInfo) {
        this.attachmentInfo = attachmentInfo;
    }

    public AttachmentInfo getAttachmentInfo() {
        return attachmentInfo;
    }
}
