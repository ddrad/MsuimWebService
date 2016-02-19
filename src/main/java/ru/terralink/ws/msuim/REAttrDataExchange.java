package ru.terralink.ws.msuim;

import ru.terralink.ws.object.request.REDataExchangeAttrECD;
import ru.terralink.ws.object.response.REAttrDataExchangeResponse;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Created by AzarovD on 19.01.2016.
 */
@WebService()
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface REAttrDataExchange {

    @WebMethod
    String sendReAttrDataExchangeResponse(@WebParam(name = "REAttrDataExchangeResponse") REAttrDataExchangeResponse reAttrDataExchangeResponse);

    @WebMethod(operationName = "REAttrDataExchangeOut", action = "http://sap.com/xi/WebService/soap1.1")
    @Oneway
    public void sendReAttrDataExchangeMessage(
            @WebParam(name = "REAttrDataExchangeMessage", partName = "REAttrDataExchangeMessage")
            REDataExchangeAttrECD reAttrDataExchangeMessage);
}
