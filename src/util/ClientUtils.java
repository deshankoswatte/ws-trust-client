package util;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import constants.Requests;
import exception.WSTrustClientException;

import static constants.Constants.ACTION_RENEW;
import static constants.Constants.ACTION_REQUEST;
import static constants.Constants.ACTION_VALIDATE;
import static constants.Constants.STS_ENDPOINT_URL;
import static constants.Constants.TRUST_STORE_LOCATION;
import static constants.Constants.TRUST_STORE_PASSWORD;

/**
 * Utils class used by the Client to perform common operations.
 */
public class ClientUtils {

    /**
     * Method used to invoke the SOAP web service of WSO2 Identity Server
     * with the help of a SOAP connection.
     *
     * @param action     Action to be performed.
     * @param parameters Identifier for the Security Token.
     * @return soapResponse containing the Request Security Token Response
     * obtained from the Security Token Service.
     *
     * @throws WSTrustClientException if there are any exceptions throws while
     *                                building and sending the request.
     */
    public static SOAPMessage callSoapWebService(String action, String... parameters)
            throws WSTrustClientException {

        setSystemProperties();

        SOAPMessage soapRequest;
        SOAPMessage soapResponse;

        try {
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // Send SOAP Message to SOAP Server
            if (action.equals(ACTION_REQUEST)) {
                soapRequest = buildRequest(action);
            } else if (action.equals(ACTION_RENEW) && parameters.length > 0) {
                soapRequest = buildRequest(action, parameters[0]);
            } else if (action.equals(ACTION_VALIDATE) && parameters.length > 0) {
                soapRequest = buildRequest(action, parameters[0]);
            } else {
                throw new WSTrustClientException("Invalid action parameter passed.");
            }

            soapResponse = soapConnection.call(soapRequest, STS_ENDPOINT_URL);

            soapConnection.close();
        } catch (SOAPException e) {
            throw new WSTrustClientException("Error while initializing the SOAP Factory.", e);
        } catch (IOException e) {
            throw new WSTrustClientException("Error occurred while creating a SOAP Message from an input stream.", e);
        }

        return soapResponse;
    }

    /**
     * Method used to build the RST relevant to the action to be performed.
     *
     * @param parameter            Action to be performed.
     * @param additionalParameters Identifier for the Security Token.
     * @return request SOAP request containing the Request Security Token.
     *
     * @throws IOException            if an error occurs while creating a SOAP Message
     *                                from an input stream.
     * @throws SOAPException          if there was an error in creating the specified
     *                                implementation of MessageFactory.
     * @throws WSTrustClientException if the operation/action type specified
     *                                is not valid.
     */
    private static SOAPMessage buildRequest(String parameter, String... additionalParameters)
            throws IOException, SOAPException, WSTrustClientException {

        SOAPMessage request;
        InputStream byteArrayInputStream;
        String[] timeStamps = generateNewTimeStamps();

        switch (parameter) {

            case ACTION_REQUEST:
                byteArrayInputStream = new ByteArrayInputStream(Requests
                        .buildRSTToRequestSecurityToken(timeStamps[0], timeStamps[1]).getBytes());
                break;

            case ACTION_RENEW:
                byteArrayInputStream = new ByteArrayInputStream(Requests
                        .buildRSTToRenewSecurityToken(timeStamps[0], timeStamps[1], additionalParameters[0]).getBytes());
                break;

            case ACTION_VALIDATE:
                byteArrayInputStream = new ByteArrayInputStream(Requests
                        .buildRSTToValidateSecurityToken(timeStamps[0], timeStamps[1], additionalParameters[0]).getBytes());
                break;

            default:
                throw new WSTrustClientException("Operations of type: Request, Renew and Validate are allowed.");
        }

        request = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
                .createMessage(null, byteArrayInputStream);
        byteArrayInputStream.close();

        return request;
    }

    /**
     * Set system properties to use a custom trust store instead of the default.
     */
    private static void setSystemProperties() {

//        Enable this property to debug ssl related problems
//        System.setProperty("javax.net.debug", "ssl");
        System.setProperty("javax.net.ssl.trustStore", TRUST_STORE_LOCATION);
        System.setProperty("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWORD);
    }

    /**
     * Generate new timestamps for the creation and expiry time of the
     * Request Security Token.
     *
     * @return timestamps string[] containing the timestamps for creation
     * and expiry time of the RequestSecurityToken.
     */
    private static String[] generateNewTimeStamps() {

        String[] timestamps = new String[2];

        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(timeZone);

        Calendar calendar = Calendar.getInstance();
        timestamps[0] = dateFormat.format(calendar.getTime());
        calendar.add(Calendar.MINUTE, 5);
        timestamps[1] = dateFormat.format(calendar.getTime());

        return timestamps;
    }
}
