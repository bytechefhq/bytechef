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

package com.bytechef.component.urlscan.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class UrlscanResultAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("result")
        .title("Result")
        .description("Using the Scan ID, you can use the Result API to poll for the scan.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/api/v1/result/{scanId}/"

            ))
        .properties(string("scanId").label("Scan ID")
            .description("UUID of scan result.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(
            object()
                .properties(
                    object("data")
                        .properties(
                            array("requests")
                                .items(object().properties(
                                    object("request")
                                        .properties(string("requestId").required(false),
                                            string("loaderId").required(false), string("documentURL").required(false),
                                            object("request")
                                                .properties(string("url").required(false),
                                                    string("method").required(false),
                                                    object("headers").additionalProperties(string())
                                                        .required(false),
                                                    string("mixedContentType").required(false),
                                                    string("initialPriority").required(false),
                                                    string("referrerPolicy").required(false),
                                                    bool("isSameSite").required(false),
                                                    bool("isLinkPreload").required(false))
                                                .required(false),
                                            number("timestamp").required(false), number("wallTime").required(false),
                                            object("initiator").properties(string("type").required(false),
                                                string("url").required(false), integer("lineNumber").required(false),
                                                integer("columnNumber").required(false))
                                                .required(false),
                                            bool("redirectHasExtraInfo").required(false),
                                            string("type").required(false), string("frameId").required(false),
                                            bool("hasUserGesture").required(false),
                                            bool("primaryRequest").required(false))
                                        .required(false),
                                    object("response")
                                        .properties(integer("encodedDataLength").required(false),
                                            integer("dataLength").required(false), string("requestId").required(false),
                                            string("type").required(false), bool("hasExtraInfo").required(false),
                                            string("hash").required(false), integer("size").required(false),
                                            object("asn")
                                                .properties(string("ip").required(false), string("asn").required(false),
                                                    string("country").required(false),
                                                    string("registrar").required(false), string("date")
                                                        .required(false),
                                                    string("description").required(false),
                                                    string("route").required(false), string("name").required(false))
                                                .required(false),
                                            object("geoip").properties(
                                                string("country").required(false), string("region").required(false),
                                                string("timezone").required(false), string("city").required(false),
                                                array("ll").items(number(null))
                                                    .required(false),
                                                string("country_name").required(false), integer("metro")
                                                    .required(false),
                                                integer("area").required(false))
                                                .required(false),
                                            object("rdns")
                                                .properties(string("ip").required(false), string("ptr").required(false))
                                                .required(false),
                                            object("response").properties(
                                                string("url").required(false), integer("status").required(false),
                                                string("statusText").required(false),
                                                object("headers").additionalProperties(string())
                                                    .required(false),
                                                string("mimeType").required(false),
                                                string("remoteIPAddress").required(false),
                                                integer("remotePort").required(false),
                                                integer("encodedDataLength").required(false),
                                                object("timing").properties(number("requestTime").required(false),
                                                    number("proxyStart").required(false),
                                                    number("proxyEnd").required(false),
                                                    number("dnsStart").required(false),
                                                    number("dnsEnd").required(false),
                                                    number("connectStart").required(false),
                                                    number("connectEnd").required(false),
                                                    number("sslStart").required(false),
                                                    number("sslEnd").required(false),
                                                    number("workerStart").required(false),
                                                    number("workerReady").required(false),
                                                    number("workerFetchStart").required(false),
                                                    number("workerRespondWithSettled").required(false),
                                                    number("sendStart").required(false),
                                                    number("sendEnd").required(false),
                                                    number("pushStart").required(false),
                                                    number("pushEnd").required(false),
                                                    number("receiveHeadersStart").required(false),
                                                    number("receiveHeadersEnd").required(false))
                                                    .required(false),
                                                number("responseTime").required(false),
                                                string("protocol").required(false),
                                                string("alternateProtocolUsage").required(false),
                                                string("securityState").required(false),
                                                object("securityDetails")
                                                    .properties(string("protocol").required(false),
                                                        string("keyExchange").required(false),
                                                        string("keyExchangeGroup").required(false),
                                                        string("cipher").required(false),
                                                        integer("certificateId").required(false),
                                                        string("subjectName").required(false),
                                                        array("sanList").items(string())
                                                            .required(false),
                                                        string("issuer").required(false),
                                                        integer("validFrom").required(false),
                                                        integer("validTo").required(false),
                                                        array("signedCertificateTimestampList").items(string())
                                                            .required(false),
                                                        string("certificateTransparencyCompliance").required(false),
                                                        integer("serverSignatureAlgorithm").required(false),
                                                        bool("encryptedClientHello").required(false))
                                                    .required(false),
                                                array("securityHeaders")
                                                    .items(object().properties(string("name").required(false),
                                                        string("value").required(false)))
                                                    .required(false))
                                                .required(false))
                                        .required(false),
                                    object("initiatorInfo").properties(string("url").required(false),
                                        string("host").required(false), string("type").required(false))
                                        .required(false)))
                                .required(false),
                            array("cookies").items(object())
                                .required(false),
                            array("console").items(object())
                                .required(false),
                            array("links").items(
                                object().properties(string("href").required(false), string("text").required(false)))
                                .required(false),
                            object("timing").required(false), array("globals").items(object())
                                .required(false))
                        .description(
                            "Raw scan data including network requests, responses, cookies, console logs, and page elements.")
                        .required(false),
                    object("stats")
                        .properties(
                            array("resourceStats")
                                .items(object().properties(integer("count").required(false),
                                    integer("size").required(false), integer("encodedSize").required(false),
                                    number("latency").required(false), array("countries").items(string())
                                        .required(false),
                                    array("ips").items(string())
                                        .required(false),
                                    string("type").required(false), string("compression").required(false),
                                    integer("percentage").required(false)))
                                .required(false),
                            array("protocolStats")
                                .items(
                                    object().properties(integer("count").required(false),
                                        integer("size").required(false), integer("encodedSize").required(false),
                                        number("latency").required(false), array("countries").items(string())
                                            .required(false),
                                        array("ips").items(string())
                                            .required(false),
                                        integer("percentage").required(false), string("protocol").required(false),
                                        object("securityState").required(false)))
                                .required(false),
                            array("tlsStats")
                                .items(
                                    object().properties(integer("count").required(false),
                                        integer("size").required(false), integer("encodedSize").required(false),
                                        number("latency").required(false), array("countries").items(string())
                                            .required(false),
                                        array("ips").items(string())
                                            .required(false),
                                        integer("percentage").required(false),
                                        object("protocols").additionalProperties(integer())
                                            .required(false),
                                        string("securityState").required(false)))
                                .required(false),
                            array("serverStats")
                                .items(object()
                                    .properties(integer("count").required(false), integer("size").required(false),
                                        integer("encodedSize").required(false), number("latency").required(false),
                                        array("countries").items(string())
                                            .required(false),
                                        array("ips").items(string())
                                            .required(false),
                                        integer("percentage").required(false), string("server").required(false)))
                                .required(false),
                            array("domainStats")
                                .items(
                                    object().properties(integer("count").required(false), array("ips").items(string())
                                        .required(false), integer("redirects").required(false),
                                        integer("size").required(false), integer("encodedSize").required(false),
                                        array("countries").items(string())
                                            .required(false),
                                        integer("index").required(false), array("initiators").items(string())
                                            .required(false),
                                        integer("requests").required(false), string("domain").required(false)))
                                .required(false),
                            array("regDomainStats")
                                .items(object().properties(integer("count").required(false),
                                    array("ips").items(string())
                                        .required(false),
                                    integer("redirects").required(false), integer("size").required(false),
                                    integer("encodedSize").required(false), array("countries").items(string())
                                        .required(false),
                                    integer("index").required(false), array("initiators").items(string())
                                        .required(false),
                                    integer("requests").required(false), string("regDomain").required(false),
                                    array("subDomains")
                                        .items(object().properties(string("domain").required(false),
                                            string("country").required(false)))
                                        .required(false)))
                                .required(false),
                            integer("secureRequests").required(false), integer("securePercentage").required(false),
                            integer("IPv6Percentage").required(false), integer("uniqCountries").required(false),
                            integer("totalLinks").required(false), integer("maliciousRequests").required(false),
                            integer("adBlocked").required(false), integer("malicious").required(false),
                            array("ipStats")
                                .items(object().properties(integer("requests").required(false),
                                    array("domains").items(string())
                                        .required(false),
                                    array("ips").items(string())
                                        .required(false),
                                    array("countries").items(string())
                                        .required(false),
                                    array("asns")
                                        .items(object().properties(string("asn").required(false),
                                            string("country").required(false), string("organisation").required(false)))
                                        .required(false),
                                    integer("encoded_size").required(false), integer("size").required(false),
                                    integer("redirects").required(false), string("ip").required(false),
                                    object("asn")
                                        .properties(string("ip").required(false), string("asn").required(false),
                                            string("country").required(false), string("registrar").required(false),
                                            string("date").required(false), string("description").required(false),
                                            string("route").required(false), string("name").required(false))
                                        .required(false),
                                    object("dns").required(false),
                                    object("geoip")
                                        .properties(string("country").required(false), string("region").required(false),
                                            string("timezone").required(false), string("city").required(false),
                                            array("ll").items(number(null))
                                                .required(false),
                                            string("country_name").required(false), integer("metro").required(false),
                                            integer("area").required(false))
                                        .required(false),
                                    integer("encodedSize").required(false), integer("index").required(false),
                                    bool("ipv6").required(false), integer("count").required(false),
                                    object("rdns")
                                        .properties(string("ip").required(false), string("ptr").required(false))
                                        .required(false)))
                                .required(false))
                        .description(
                            "Statistical analysis of the scan including resource counts, protocols, security metrics, and geographic distribution.")
                        .required(false),
                    object("meta")
                        .properties(
                            object("processors")
                                .properties(object("umbrella").properties(array("data")
                                    .items(
                                        object().properties(string("hostname").required(false), integer("rank")
                                            .required(false)))
                                    .required(false))
                                    .required(false),
                                    object("geoip")
                                        .properties(array("data").items(object().properties(string("ip").required(
                                            false),
                                            object("geoip")
                                                .properties(string("country").required(false), string("country_name")
                                                    .required(false), string("region").required(false),
                                                    string("timezone").required(false), string("city").required(false),
                                                    array("ll").items(number(null))
                                                        .required(false),
                                                    integer("metro").required(false), integer("area").required(false))
                                                .required(false)))
                                            .required(false))
                                        .required(false),
                                    object("rdns").properties(array(
                                        "data").items(
                                            object().properties(
                                                string("ip").required(false), string("ptr").required(false)))
                                            .required(false))
                                        .required(false),
                                    object(
                                        "asn").properties(
                                            array("data").items(object()
                                                .properties(string("ip").required(false), string("asn").required(false),
                                                    string("country").required(false),
                                                    string("organisation")
                                                        .required(false),
                                                    string("registrar").required(false), string("date").required(false),
                                                    string("description").required(false),
                                                    string("route").required(false), string("name").required(false)))
                                                .required(false))
                                            .required(false),
                                    object("wappa").properties(
                                        array("data").items(
                                            object()
                                                .properties(array("confidence")
                                                    .items(object().properties(integer("confidence").required(false),
                                                        string("pattern").required(false)))
                                                    .required(false), integer("confidenceTotal").required(false),
                                                    string("app").required(false), string("icon").required(false),
                                                    string("website").required(false),
                                                    array("categories")
                                                        .items(
                                                            object().properties(string("name").required(false),
                                                                string("id").required(false),
                                                                integer("priority").required(false)))
                                                        .required(false)))
                                            .required(false))
                                        .required(false))
                                .required(false))
                        .description(
                            "Enriched metadata from external processors including domain rankings, geolocation, DNS records, and ASN information.")
                        .required(false),
                    object("task")
                        .properties(string("uuid").required(false), string("time").required(false),
                            string("url").required(false), string("visibility").required(false),
                            object("options").required(false), string("method").required(false),
                            string("source").required(false), string("userAgent").required(false),
                            string("reportURL").required(false), string("screenshotURL").required(false),
                            string("domURL").required(false), array("tags").items(string())
                                .required(false))
                        .description(
                            "Information about the scan task including configuration, URLs, and submission details.")
                        .required(false),
                    object("page").properties(string("country").required(false), string("server").required(false),
                        string("city").required(false), string("domain").required(false), string("ip").required(false),
                        string("asnname")
                            .required(false),
                        string("asn").required(false), string("url").required(false), string("ptr").required(false))
                        .description(
                            "Information about the scanned page including server details, location and network properties.")
                        .required(false),
                    object("lists").properties(array("ips").items(string())
                        .required(false),
                        array("countries").items(string())
                            .required(false),
                        array("asns").items(string())
                            .required(false),
                        array("domains").items(string())
                            .required(false),
                        array("servers").items(string())
                            .required(false),
                        array("urls").items(string())
                            .required(false),
                        array("linkDomains").items(string())
                            .required(false),
                        array("certificates")
                            .items(object().properties(
                                string("subjectName").required(false), string("issuer").required(false),
                                integer("validFrom").required(false), integer("validTo").required(false)))
                            .required(false),
                        array("hashes").items(string())
                            .required(false))
                        .description(
                            "Aggregated lists of unique elements found during the scan including IPs, domains, URLs, and certificates.")
                        .required(false),
                    object("verdicts")
                        .properties(
                            object("overall")
                                .properties(integer("score").required(false), array("categories").items(string())
                                    .required(false),
                                    array("brands").items(object())
                                        .required(false),
                                    array("tags").items(string())
                                        .required(false),
                                    bool("malicious").required(false), bool("hasVerdicts").required(false))
                                .required(false),
                            object("urlscan")
                                .properties(integer("score").required(false), array("categories").items(string())
                                    .required(false),
                                    array("brands").items(object())
                                        .required(false),
                                    array("tags").items(string())
                                        .required(false),
                                    bool("malicious").required(false), bool("hasVerdicts").required(false))
                                .required(false),
                            object("engines")
                                .properties(integer("score").required(false), array("categories").items(string())
                                    .required(false),
                                    array("brands").items(object())
                                        .required(false),
                                    array("tags").items(string())
                                        .required(false),
                                    bool("malicious").required(false), integer("enginesTotal").required(false),
                                    integer("maliciousTotal").required(false), integer("benignTotal").required(false),
                                    array("verdicts")
                                        .items(object().properties(string("engine").required(false),
                                            string("classification").required(false)))
                                        .required(false),
                                    array("maliciousVerdicts").items(object())
                                        .required(false),
                                    array("benignVerdicts").items(object())
                                        .required(false),
                                    bool("hasVerdicts").required(false))
                                .required(false),
                            object("community").properties(integer("score").required(false), array("categories")
                                .items(string())
                                .required(false),
                                array("brands").items(object())
                                    .required(false),
                                array("tags").items(string())
                                    .required(false),
                                bool("malicious").required(false), integer("votesBenign").required(false),
                                integer("votesMalicious").required(false), integer("votesTotal").required(false),
                                bool("hasVerdicts").required(false))
                                .required(false))
                        .description(
                            "Security verdicts and threat analysis from multiple sources including urlscan.io, third-party engines, and community ratings.")
                        .required(false),
                    object("submitter").properties(string("country").required(false))
                        .description("Information about the entity that submitted the scan request.")
                        .required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON))));

    private UrlscanResultAction() {
    }
}
