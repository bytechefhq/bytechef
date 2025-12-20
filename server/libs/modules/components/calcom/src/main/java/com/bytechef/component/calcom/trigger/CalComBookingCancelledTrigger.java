/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.calcom.trigger;

import static com.bytechef.component.calcom.constant.CalComConstants.PERSON_RECORD;
import static com.bytechef.component.calcom.constant.CalComConstants.RESPONSE_VALUE;
import static com.bytechef.component.calcom.constant.CalComConstants.WEBHOOK_ID;
import static com.bytechef.component.calcom.util.CalComUtils.getContent;
import static com.bytechef.component.calcom.util.CalComUtils.subscribeWebhook;
import static com.bytechef.component.calcom.util.CalComUtils.unsubscribeWebhook;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class CalComBookingCancelledTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("bookingCanceled")
        .title("Booking Canceled")
        .description("Triggers when a booking is canceled.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string("bookerUrl")
                            .description("Booker URL."),
                        string("title")
                            .description("Title of the booking."),
                        integer("length")
                            .description("Length of the booking."),
                        string("type")
                            .description("Type of the booking."),
                        string("additionalNotes")
                            .description("Additional notes of the booking."),
                        string("description")
                            .description("Description of the booking."),
                        object("customInputs")
                            .description("Custom inputs of the booking."),
                        integer("eventTypeId")
                            .description("ID of the event type of the booking."),
                        object("userFieldsResponses")
                            .description("User field responses of the booking."),
                        object("responses")
                            .description("Responses of the booking.")
                            .properties(
                                object("name")
                                    .description("Name of the respondee.")
                                    .properties(RESPONSE_VALUE),
                                object("email")
                                    .description("Email of the respondee.")
                                    .properties(RESPONSE_VALUE),
                                object("location")
                                    .description("Location of the respondee.")
                                    .properties(RESPONSE_VALUE),
                                object("title")
                                    .description("Title of the response.")
                                    .properties(
                                        string("label")
                                            .description("Title label of the response."),
                                        bool("isHidden")
                                            .description("Whether the title is hidden")),
                                object("notes")
                                    .description("Notes of the response.")
                                    .properties(
                                        string("label")
                                            .description("Notes label of the response."),
                                        bool("isHidden")
                                            .description("Whether the notes are hidden")),
                                object("guests")
                                    .description("Guests of the response.")
                                    .properties(RESPONSE_VALUE),
                                object("rescheduleReason")
                                    .description("Reschedule reason of the response.")
                                    .properties(
                                        string("label")
                                            .description("Reschedule reason of the response."),
                                        bool("isHidden")
                                            .description("Whether the rescheduling reason is hidden"))),
                        string("startTime")
                            .description("Start time of the booking."),
                        string("endTime")
                            .description("End time of the booking."),
                        object("organizer")
                            .description("Organizer of the booking.")
                            .properties(
                                PERSON_RECORD),
                        array("attendees")
                            .description("Attendees of the booking.")
                            .items(
                                PERSON_RECORD),
                        string("uid")
                            .description("UID of the booking."),
                        integer("bookingId")
                            .description("ID of the booking."),
                        string("location")
                            .description("Location of the booking."),
                        array("destinationCalendar")
                            .description("Destination calendar of the booking.")
                            .items(
                                object("calendar")
                                    .properties(
                                        integer("id")
                                            .description("ID of the calendar."),
                                        string("integration")
                                            .description("Integration of the calendar."),
                                        string("externalId")
                                            .description("External ID of the calendar."),
                                        string("primaryEmail")
                                            .description("Primary email of the calendar."),
                                        integer("userId")
                                            .description("User ID of the calendar."),
                                        integer("eventTypeId")
                                            .description("Event type id of the event that is booked in the calendar."),
                                        integer("credentialId")
                                            .description("Credential ID of the calendar."),
                                        integer("delegationCredentialId")
                                            .description("Delegation credential ID of the calendar."),
                                        integer("domainWideDelegationCredentialId")
                                            .description("Domain wide delegation credential ID of the calendar."))),
                        string("cancellationReason")
                            .description("Cancellation reason of the booking cancellation."),
                        integer("seatsPerTimeSlot")
                            .description("How many seats are available in the booking timeslot."),
                        bool("seatsShowAttendees")
                            .description("Whether the seats show attendees."),
                        string("iCalUID")
                            .description("UID of the iCal."),
                        integer("iCalSequence")
                            .description("Sequence of the iCal."),
                        bool("hideOrganizerEmail")
                            .description("Whether the organizer email is hidden."),
                        string("customReplyToEmail")
                            .description("Custom reply to the email."),
                        string("eventTitle")
                            .description("Event title of the booking."),
                        string("eventDescription")
                            .description("Event description of the booking."),
                        bool("requiresConfirmation")
                            .description("Whether booking requires confirmation."),
                        integer("price")
                            .description("Price of the booking."),
                        string("currency")
                            .description("Currency of the price of the booking."),
                        string("status")
                            .description("Status of the booking"),
                        string("cancelledBy")
                            .description("User that cancelled the booking."))))
        .webhookEnable(CalComBookingCancelledTrigger::webhookEnable)
        .webhookDisable(CalComBookingCancelledTrigger::webhookDisable)
        .webhookRequest(CalComBookingCancelledTrigger::webhookRequest);

    private CalComBookingCancelledTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(WEBHOOK_ID, subscribeWebhook("BOOKING_CANCELLED", context, webhookUrl)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        unsubscribeWebhook(context, outputParameters.getString(WEBHOOK_ID));
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return getContent(body);
    }
}
