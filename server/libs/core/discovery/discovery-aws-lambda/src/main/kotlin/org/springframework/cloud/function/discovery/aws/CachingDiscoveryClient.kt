package org.springframework.cloud.function.discovery.aws

import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.event.HeartbeatEvent
import org.springframework.context.ApplicationListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
class CachingDiscoveryClient(private val targetDiscoveryClient: DiscoveryClient) : DiscoveryClient, ApplicationListener<HeartbeatEvent> {

	private val cache = ConcurrentHashMap<String, Any>()
	private fun <T> cache(key: String, arrive: (String) -> Any): T = this.cache.computeIfAbsent(key) { arrive(key) } as T

	override fun onApplicationEvent(p0: HeartbeatEvent) {
		this.cache.clear()
	}

	override fun getServices(): MutableList<String> {
		return cache("services") { targetDiscoveryClient.services }
	}

	override fun getInstances(serviceId: String?): MutableList<ServiceInstance> {
		return cache("instances:${serviceId}") {
			this.targetDiscoveryClient.getInstances(serviceId)
		}
	}

	override fun description(): String {
		return cache("description") { targetDiscoveryClient.description() }
	}
}