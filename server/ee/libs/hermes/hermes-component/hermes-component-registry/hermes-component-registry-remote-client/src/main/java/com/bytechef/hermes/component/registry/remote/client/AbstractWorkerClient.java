
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
            
package com.bytechef.hermes.component.registry.remote.client;

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

public abstract class AbstractWorkerClient {

    protected static final String WORKER_APP = "worker-app";
    protected final DefaultWebClient defaultWebClient;

    protected final DiscoveryClient discoveryClient;
    protected final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public AbstractWorkerClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.defaultWebClient = defaultWebClient;
        this.discoveryClient = discoveryClient;
        this.objectMapper = objectMapper;
    }

    protected URI toUri(
        UriBuilder uriBuilder, String componentName, String path, Object... uriVariables) {

        return build(uriBuilder, componentName, path)
            .build(uriVariables);
    }

    protected URI toUri(
        UriBuilder uriBuilder, ServiceInstance serviceInstance, String path, Map<String, ?> uriVariables,
        Map<String, List<String>> queryParams) {

        return build(uriBuilder, serviceInstance, path)
            .queryParams(CollectionUtils.toMultiValueMap(queryParams))
            .build(uriVariables);
    }

    protected URI toUri(UriBuilder uriBuilder, ServiceInstance serviceInstance, String path, Object... uriVariables) {
        return build(uriBuilder, serviceInstance, path)
            .build(uriVariables);
    }

    private UriBuilder build(UriBuilder uriBuilder, String componentName, String path) {
        ServiceInstance serviceInstance = WorkerDiscoveryUtils.filterServiceInstance(
            discoveryClient.getInstances(WORKER_APP), componentName, objectMapper);

        return build(uriBuilder, serviceInstance, path);
    }

    private static UriBuilder build(UriBuilder uriBuilder, ServiceInstance serviceInstance, String path) {
        return uriBuilder
            .scheme("http")
            .host(serviceInstance.getHost())
            .port(serviceInstance.getPort())
            .path("/remote" + path);
    }
}
