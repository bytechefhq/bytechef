/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.registry;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisRegistration implements Registration {

    @Value("${server.port}")
    private Integer port;

    @Value("${spring.application.name}")
    private String serviceId;

    private String host;

    @Value("${spring.cloud.redis.discovery.instanceId:null}")
    private String instanceId;

    private Map<String, String> metadata = Collections.emptyMap();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RedisRegistration that = (RedisRegistration) o;

        return serviceId.equals(that.serviceId) && Objects.equals(instanceId, that.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, instanceId);
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getHost() {
        try {
            if (host == null) {
                InetAddress inetAddress = getLocalHostLANAddress();

                return inetAddress.getHostAddress();
            } else {
                return host;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public URI getUri() {
        return new DefaultServiceInstance(instanceId, serviceId, host, port, false).getUri();
    }

    @Override
    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    private InetAddress getLocalHostLANAddress() throws Exception {
        InetAddress inetAddress = null;

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();

            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

            while (inetAddresses.hasMoreElements()) {
                InetAddress curInetAddress = inetAddresses.nextElement();

                if (curInetAddress.isLoopbackAddress()) {
                    continue;
                }

                if (curInetAddress.isSiteLocalAddress()) {
                    inetAddress = curInetAddress;

                    break;
                } else if (inetAddress == null) {
                    inetAddress = curInetAddress;
                }
            }
        }

        if (inetAddress == null) {
            inetAddress = InetAddress.getLocalHost();
        }

        return inetAddress;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = new HashMap<>(metadata);
    }
}
