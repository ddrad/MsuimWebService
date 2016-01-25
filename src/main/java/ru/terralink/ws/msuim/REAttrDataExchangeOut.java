package ru.terralink.ws.msuim;

import ru.terralink.ws.object.REAttrDataExchangeResponse;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
/**
 * Created by AzarovD on 19.01.2016.
 */
@WebService
@SOAPBinding(style = Style.RPC)
public interface REAttrDataExchangeOut {

    @WebMethod
    String printMessage(@WebParam(name = "REAttrDataExchangeResponse") REAttrDataExchangeResponse reAttrDataExchangeResponse);
}
