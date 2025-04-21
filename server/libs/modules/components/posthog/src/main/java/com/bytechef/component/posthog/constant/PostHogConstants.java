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

package com.bytechef.component.posthog.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Nikolina Spehar
 */
public class PostHogConstants {

    public static final String API_KEY = "api_key";
    public static final String DISTINCT_ID = "distinct_id";
    public static final String EVENT = "event";
    public static final String ID = "id";
    public static final String NAME = "name";

    public static final ModifiableObjectProperty PROJECT_OUTPUT_SCHEMA =
        object()
            .properties(
                string("id")
                    .description("The unique identifier for this project."),
                string("organization")
                    .description("The organization UUID associated with this project."),
                string("name")
                    .description("The name of the project."),
                string("product_description")
                    .description("A description of the product."),
                string("created_at")
                    .description("Creation timestamp in ISO 8601 format."),
                integer("effective_membership_level")
                    .description("Level of membership assigned to this organization."),
                bool("has_group_types")
                    .description("Indicates whether group types are enabled."),
                string("live_events_token")
                    .description("Token used for accessing live events."),
                string("updated_at")
                    .description("Last updated timestamp in ISO 8601 format."),
                string("uuid")
                    .description("Universally unique identifier for this record."),
                string("api_token")
                    .description("API token used for authentication."),
                array("app_urls")
                    .items(string())
                    .description("List of application URLs."),
                string("slack_incoming_webhook")
                    .description("Slack webhook URL for incoming messages."),
                bool("anonymize_ips")
                    .description("Whether IPs are anonymized."),
                bool("completed_snippet_onboarding")
                    .description("Whether the snippet onboarding has been completed."),
                bool("ingested_event")
                    .description("Indicates if any event has been ingested."),
                string("test_account_filters")
                    .description("Filters used to define test accounts."),
                bool("test_account_filters_default_checked")
                    .description("Default checked state for test account filters."),
                string("path_cleaning_filters")
                    .description("Filters used to clean paths in analytics."),
                bool("is_demo")
                    .description("Whether this is a demo account."),
                string("timezone")
                    .description("Timezone of the account."),
                string("data_attributes")
                    .description("Additional data attributes."),
                array("person_display_name_properties")
                    .items(string())
                    .description("Properties used to display person's name."),
                string("correlation_config")
                    .description("Configuration for correlation analysis."),
                bool("autocapture_opt_out")
                    .description("Whether autocapture is disabled."),
                bool("autocapture_exceptions_opt_in")
                    .description("Whether exceptions are autocaptured."),
                bool("autocapture_web_vitals_opt_in")
                    .description("Whether web vitals are autocaptured."),
                string("autocapture_web_vitals_allowed_metrics")
                    .description("Metrics allowed for web vitals autocapture."),
                string("autocapture_exceptions_errors_to_ignore")
                    .description("List of error types to ignore in exceptions."),
                bool("capture_console_log_opt_in")
                    .description("Whether console log capturing is enabled."),
                bool("capture_performance_opt_in")
                    .description("Whether performance capturing is enabled."),
                bool("session_recording_opt_in")
                    .description("Whether session recording is enabled."),
                string("session_recording_sample_rate")
                    .description("Sample rate for session recording."),
                integer("session_recording_minimum_duration_milliseconds")
                    .description("Minimum duration for recorded sessions in milliseconds."),
                string("session_recording_linked_flag")
                    .description("Flag linking session recordings."),
                string("session_recording_network_payload_capture_config")
                    .description("Network payload config for session recordings."),
                string("session_recording_masking_config")
                    .description("Masking configuration for session recordings."),
                string("session_replay_config")
                    .description("Configuration for session replay."),
                string("survey_config")
                    .description("Configuration for surveys."),
                bool("access_control")
                    .description("Whether access control is enabled."),
                integer("week_start_day")
                    .description("Defines the first day of the week."),
                integer("primary_dashboard")
                    .description("ID of the primary dashboard."),
                array("live_events_columns")
                    .items(string())
                    .description("Columns shown in live events."),
                array("recording_domains")
                    .items(string())
                    .description("List of domains where recording is allowed."),
                string("person_on_events_querying_enabled")
                    .description("Whether person querying on events is enabled."),
                bool("inject_web_apps")
                    .description("Whether web apps should be injected."),
                string("extra_settings")
                    .description("Any extra settings not covered elsewhere."),
                string("modifiers")
                    .description("Current modifiers in use."),
                string("default_modifiers")
                    .description("Default modifier settings."),
                string("has_completed_onboarding_for")
                    .description("Features the user has completed onboarding for."),
                bool("surveys_opt_in")
                    .description("Whether surveys are enabled."),
                bool("heatmaps_opt_in")
                    .description("Whether heatmaps are enabled."),
                string("product_intents")
                    .description("Product intents settings."),
                string("flags_persistence_default")
                    .description("Default setting for flags persistence."));

    private PostHogConstants() {
    }
}
