
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.execution.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.data.storage.db.repository.converter.DataEntryValueWrapperToStringConverter;
import com.bytechef.data.storage.db.repository.converter.StringToDataEntryValueWrapperConverter;
import com.bytechef.hermes.execution.repository.converter.StringToTriggerStateValueConverter;
import com.bytechef.hermes.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.hermes.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.hermes.execution.repository.converter.TriggerStateValueToStringConverter;
import com.bytechef.hermes.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.hermes.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToFileEntryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public JdbcConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<?> userConverters() {
        return Arrays.asList(
            new DataEntryValueWrapperToStringConverter(objectMapper),
            new ExecutionErrorToStringConverter(objectMapper),
            new FileEntryToStringConverter(objectMapper),
            new MapWrapperToStringConverter(objectMapper),
            new StringToDataEntryValueWrapperConverter(objectMapper),
            new StringToExecutionErrorConverter(objectMapper),
            new StringToFileEntryConverter(objectMapper),
            new StringToMapWrapperConverter(objectMapper),
            new StringToWebhooksConverter(objectMapper),
            new StringToWorkflowExecutionIdConverter(),
            new StringToWorkflowTaskConverter(objectMapper),
            new StringToWorkflowTriggerConverter(objectMapper),
            new StringToTriggerStateValueConverter(objectMapper),
            new TriggerStateValueToStringConverter(objectMapper),
            new WebhooksToStringConverter(objectMapper),
            new WorkflowExecutionIdToStringConverter(),
            new WorkflowTaskToStringConverter(objectMapper),
            new WorkflowTriggerToStringConverter(objectMapper));
    }
}
