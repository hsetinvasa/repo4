package job33.gitb;

import com.gitb.core.*;
import com.gitb.ms.*;
import com.gitb.ms.Void;
import com.gitb.tr.TestResultType;
import org.apache.cxf.headers.Header;
import org.apache.cxf.ws.addressing.AddressingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.annotation.Resource;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring component that realises the messaging service.
 */
@Component
public class MessagingServiceImpl implements MessagingService {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(MessagingServiceImpl.class);
    /** The name of the WS-Addressing ReplyTo field. */
    private static QName REPLY_TO_QNAME = new AddressingConstants().getReplyToQName();

    /** The name of the input parameter for the message received from the test bed. */
    public static final String INPUT__MESSAGE = "messageToSend";
    /** The name of the output parameter for the message to send to the test bed. */
    public static final String OUTPUT__MESSAGE = "messageReceived";

    @Value("${service.id}")
    private String serviceId = null;

    @Value("${service.version}")
    private String serviceVersion = null;

    @Autowired
    private SessionManager sessionManager = null;

    @Resource
    private WebServiceContext wsContext = null;

    /**
     * The purpose of the getModuleDefinition call is to inform its caller on how the service is supposed to be called.
     *
     * In this case its main purpose is to define the expected input and output parameters:
     * <ul>
     *     <li>The message to send (string).</li>
     *     <li>The message received (string).</li>
     * </ul>
     * Note that defining output messages is optional as any and all output will be send back to the test bed regardless.
     * It is important however to define all expected inputs here as these are checked by the test bed before making the
     * actual call. Typically all such inputs need to be defined as optional considering that they apply both to the send
     * and receive operations which would likely not require the same inputs. The reason for this was to support inputs
     * for the receive operation but maintain backwards compatibility for existing services. As such, it is best to
     * define all input as optional and simply check them as part of the send and/or receive logic.
     *
     * @param parameters No parameters are expected.
     * @return The response.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void parameters) {
        GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
        response.setModule(new MessagingModule());
        response.getModule().setId(serviceId);
        response.getModule().setMetadata(new Metadata());
        response.getModule().getMetadata().setName(response.getModule().getId());
        response.getModule().getMetadata().setVersion(serviceVersion);
        response.getModule().setInputs(new TypedParameters());
        response.getModule().getInputs().getParam().add(Utils.createParameter(INPUT__MESSAGE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The message to send."));
        response.getModule().getInputs().getParam().add(Utils.createParameter(OUTPUT__MESSAGE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The received message."));
        return response;
    }

    /**
     * The initiate operation is called by the test bed when a new test session is being prepared.
     *
     * This call expects from the service to do the following:
     * <ul>
     *     <li>Generate and store a session identifier to keep track of messages linked to the test session.</li>
     *     <li>Process, if needed, the configuration provided by the SUT.</li>
     *     <li>Return, if needed, configuration to be displayed to the user for the SUT actor.</li>
     * </ul>
     *
     * @param parameters The actor configuration provided by the SUT.
     * @return The session ID and any generated configuration to display for the SUT.
     */
    @Override
    public InitiateResponse initiate(InitiateRequest parameters) {
        InitiateResponse response = new InitiateResponse();
        // Get the ReplyTo address for the test bed callbacks based on WS-Addressing.
        String replyToAddress = getReplyToAddress();
        String sessionId = sessionManager.createSession(replyToAddress);
        response.setSessionId(sessionId);
        LOG.info("Initiated a new session [{}] with callback address [{}]", sessionId, replyToAddress);
        return response;
    }

    /**
     * The receive operation is called when the test bed is expecting to receive a message.
     *
     * Typically this implementation is empty but it could be customised based on received input parameters.
     *
     * @param parameters The input parameters to consider (if any).
     * @return A void result.
     */
    @Override
    public Void receive(ReceiveRequest parameters) {
        LOG.info("Received 'receive' command from test bed for session [{}]", parameters.getSessionId());
        return new Void();
    }

    /**
     * The send operation is called when the test bed wants to send a message through this service.
     *
     * This is the point where input is received for the call that this service needs to translate into an actual
     * communication. This communication would be specific to a communication protocol (e.g. send an email) or
     * a separate system's API.
     *
     * The result of the operation is typically an empty success or failure report depending on whether or not the
     * communication was successful. This report could however include additional information that would be reported
     * back to the test bed.
     *
     * @param parameters The input parameters and configuration to consider for the send operation.
     * @return A status report for the call that will be returned to the test bed.
     */
    @Override
    public SendResponse send(SendRequest parameters) {
        List<AnyContent> messageInput = getInput(parameters, INPUT__MESSAGE);
        if (messageInput.size() != 1) {
            throw new IllegalArgumentException(String.format("Only a single input is expected named [%s]", INPUT__MESSAGE));
        } else {
            /*
            At this point we would expect the actual communication or simulation to take place. In this sample implementation
            we simply log the message received from the test bed.
             */
            LOG.info("Received 'send' command from test bed for session [{}]. The message to send is [{}]", parameters.getSessionId(), messageInput.get(0).getValue());
        }
        SendResponse response = new SendResponse();
        response.setReport(Utils.createReport(TestResultType.SUCCESS));
        return response;
    }

    /**
     * The beginTransaction operation is called by the test bed with a transaction starts.
     *
     * Often there is no need to take any action here but it could be interesting to do so if you need specific
     * actions per transaction.
     *
     * @param parameters The transaction configuration.
     * @return A void result.
     */
    @Override
    public Void beginTransaction(BeginTransactionRequest parameters) {
        LOG.info("Transaction starting for session [{}]", parameters.getSessionId());
        return new Void();
    }

    /**
     * The endTransaction operation is the counterpart of the beginTransaction and is called when the transaction terminates.
     *
     * @param parameters The session ID this transaction related to.
     * @return A void result.
     */
    @Override
    public Void endTransaction(BasicRequest parameters) {
        LOG.info("Transaction ending for session [{}]", parameters.getSessionId());
        return new Void();
    }

    /**
     * The finalize operation is called by the test bed when a test session completes.
     *
     * A typical action that needs to take place here is the cleanup of any resources that were specific to the session
     * in question. This would typically involve the state recorded for the session.
     *
     * @param parameters The session ID that completed.
     * @return A void result.
     */
    @Override
    public Void finalize(FinalizeRequest parameters) {
        LOG.info("Finalising session [{}]", parameters.getSessionId());
        // Cleanup in-memory state for the completed session.
        sessionManager.destroySession(parameters.getSessionId());
        return new Void();
    }

    /**
     * Lookup a provided input from the received request parameters.
     *
     * @param parameters The request's parameters.
     * @param inputName The name of the input to lookup.
     * @return The inputs found to match the parameter name (not null).
     */
    private List<AnyContent> getInput(SendRequest parameters, String inputName) {
        List<AnyContent> inputs = new ArrayList<>();
        if (parameters != null) {
            inputs.addAll(Utils.getInputsForName(parameters.getInput(), inputName));
        }
        return inputs;
    }

    /**
     * Get the test bed address to reply to.
     *
     * @return The address.
     */
    private String getReplyToAddress() {
        List<Header> headers = (List<Header>) wsContext.getMessageContext().get(Header.HEADER_LIST);
        for (Header header: headers) {
            if (header.getName().equals(REPLY_TO_QNAME)) {
                String replyToAddress = ((Element)header.getObject()).getTextContent().trim();
                if (!replyToAddress.toLowerCase().endsWith("?wsdl")) {
                    replyToAddress += "?wsdl";
                }
                return replyToAddress;
            }
        }
        return null;
    }

}
