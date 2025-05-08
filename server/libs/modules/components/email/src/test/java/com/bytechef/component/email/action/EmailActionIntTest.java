package com.bytechef.component.email.action;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.email.EmailProtocol;
import com.bytechef.component.email.constant.EmailConstants;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.MockParametersImpl;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class EmailActionIntTest {


    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP)
        .withConfiguration(GreenMailConfiguration
            .aConfig()
            .withUser("bytecheftest@localhost", "bytecheftest", "bytecheftest"));

    @Test
    public void testEmailActions() throws Exception {
        ActionContext actionContext = mock(ActionContext.class);

        SendEmailAction.perform(getSendEmailActionParameters(), getConnectionParameters(EmailProtocol.smtp), actionContext);

        greenMail.waitForIncomingEmail(1);

        Object result = ReadEmailAction.perform(new MockParametersImpl(Collections.emptyMap()), getConnectionParameters(EmailProtocol.imap), actionContext);

        System.out.println(result);
    }

    private Parameters getConnectionParameters(EmailProtocol protocol) {
        HashMap<String, Object> parameterMap = new HashMap<>();

        AbstractServer server = greenMail.getSmtp();

        if (protocol == EmailProtocol.imap) {
            server = greenMail.getImap();
        }


        parameterMap.put(EmailConstants.PORT, String.valueOf(server.getPort()));
        parameterMap.put(EmailConstants.HOST, "localhost");
        parameterMap.put(Authorization.USERNAME, "bytecheftest");
        parameterMap.put(Authorization.PASSWORD, "bytecheftest");
        parameterMap.put(EmailConstants.PROTOCOL, protocol.name());

        return MockParametersFactory.create(parameterMap);
    }

    private Parameters getSendEmailActionParameters() {
        HashMap<String, Object> parameterMap = new HashMap<>();

        parameterMap.put("from", "test.from@test.com");
        parameterMap.put("to", Arrays.asList("test.to@test.com"));
        parameterMap.put("cc", Arrays.asList("test.cc@test.com"));
        parameterMap.put("bcc", Arrays.asList("test.bcc@test.com"));
        parameterMap.put("replyTo", Arrays.asList("test.replyto@test.com"));
        parameterMap.put("subject", "Do Test Until Success");
        parameterMap.put("content", "Magic appears always");

        return MockParametersFactory.create(parameterMap);
    }
}
