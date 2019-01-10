package job33.gitb;

import com.gitb.ms.MessagingClient;
import com.gitb.ms.NotifyForMessageRequest;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component used to store sessions and their state.
 *
 * This class is key in maintaining an overall context across a request and one or more
 * responses. It allows mapping of received data to a given test session running in the
 * test bed.
 *
 * This implementation stores session information in memory. An alternative solution
 * that would be fault-tolerant could store test session data in a DB.
 *
 * This class also holds the implementation responsible for notifying the test bed for
 * received messages. This is done via the test bed's call-back service interface. Apache
 * CXF is used for this for maximum flexibility. As an example, the configuration of
 * a proxy to be used for this call is provided that can be optionally set on the call-back
 * service proxy via configuration properties (set in application.properties).
 */
@Component
public class SessionManager {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

    /** The map of in-memory active sessions. */
    private Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    @Autowired
    private ProxyInfo proxy = null;

    /**
     * Create a new session.
     *
     * @param callbackURL The callback URL to set for this session.
     * @return The session ID that was generated.
     */
    public String createSession(String callbackURL) {
        if (callbackURL == null) {
            throw new IllegalArgumentException("A callback URL must be provided");
        }
        String sessionId = UUID.randomUUID().toString();
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put(SessionData.CALLBACK_URL, callbackURL);
        sessions.put(sessionId, sessionInfo);
        return sessionId;
    }

    /**
     * Remove the provided session from the list of tracked sessions.
     *
     * @param sessionId The session ID to remove.
     */
    public void destroySession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Get a given item of information linked to a specific session.
     *
     * @param sessionId The session ID we want to lookup.
     * @param infoKey The key of the value that we want to retrieve.
     * @return The retrieved value.
     */
    public Object getSessionInfo(String sessionId, String infoKey) {
        Object value = null;
        if (sessions.containsKey(sessionId)) {
            value = sessions.get(sessionId).get(infoKey);
        }
        return value;
    }

    /**
     * Set the given information item for a session.
     *
     * @param sessionId The session ID to set the information for.
     * @param infoKey The information key.
     * @param infoValue The information value.
     */
    public void setSessionInfo(String sessionId, String infoKey, Object infoValue) {
        sessions.get(sessionId).put(infoKey, infoValue);
    }

    /**
     * Get all the active sessions.
     *
     * @return An unmodifiable map of the sessions.
     */
    public Map<String, Map<String, Object>> getAllSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    /**
     * Notify the test bed for a given session.
     *
     * @param sessionId The session ID to notify the test bed for.
     * @param report The report to notify the test bed with.
     */
    public void notifyTestBed(String sessionId, TAR report){
        String callback = (String)getSessionInfo(sessionId, SessionData.CALLBACK_URL);
        if (callback == null) {
            LOG.warn("Could not find callback URL for session [{}]", sessionId);
        } else {
            try {
                LOG.info("Notifying test bed for session [{}]", sessionId);
                callTestBed(sessionId, report, callback);
            } catch (Exception e) {
                LOG.warn("Error while notifying test bed for session [{}]", sessionId, e);
                callTestBed(sessionId, Utils.createReport(TestResultType.FAILURE), callback);
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Call the test bed to notify it of received communication.
     *
     * @param sessionId The session ID that this notification relates to.
     * @param report The TAR report to send back.
     * @param callbackAddress The address on which the call is to be made.
     */
    private void callTestBed(String sessionId, TAR report, String callbackAddress) {
        /*
         * First setup the service client. This is not created once and reused since the address to call
         * is determined dynamically from the WS-Addressing information (passed here as the callback address).
         */
        JaxWsProxyFactoryBean proxyFactoryBean = new JaxWsProxyFactoryBean();
        proxyFactoryBean.setServiceClass(MessagingClient.class);
        proxyFactoryBean.setAddress(callbackAddress);
        MessagingClient serviceProxy = (MessagingClient)proxyFactoryBean.create();
        Client client = ClientProxy.getClient(serviceProxy);
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        httpConduit.getClient().setAutoRedirect(true);
        // Apply proxy settings (if applicable).
        if (proxy.isEnabled()) {
            proxy.applyToCxfConduit(httpConduit);
        }
        // Make the call.
        NotifyForMessageRequest request = new NotifyForMessageRequest();
        request.setSessionId(sessionId);
        request.setReport(report);
        serviceProxy.notifyForMessage(request);
    }

    /**
     * Constants used to identify data maintained as part of a session's state.
     */
    static class SessionData {

        /** The URL on which the test bed is to be called back. */
        public static final String CALLBACK_URL = "callbackURL";

    }

}
