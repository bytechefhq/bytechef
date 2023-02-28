package org.springframework.cloud.function.discovery.aws

import org.assertj.core.api.Assertions
import org.assertj.core.api.Condition
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient
import org.springframework.test.context.junit4.SpringRunner
import java.util.function.Predicate


@SpringBootTest(classes = [DiscoveryClientTest.SampleConfig::class])
@RunWith(SpringRunner::class)
class DiscoveryClientTest {

	@SpringBootApplication
	@EnableDiscoveryClient
	class SampleConfig(val discoveryClient: DiscoveryClient)

	@Autowired
	val sc: SampleConfig? = null

	@Test
	fun provides() {
		val dc = sc!!.discoveryClient
		Assertions.assertThat(dc).isNotNull
		val predicate = Predicate<DiscoveryClient> {
			it is CompositeDiscoveryClient && it.discoveryClients.filterIsInstance<LambdaDiscoveryClient>().any()
		}
		Assertions.assertThat(dc).`is`(Condition<DiscoveryClient>(
				predicate, "this should be a ${CompositeDiscoveryClient::javaClass} that contains an " +
							"implementation of a ${LambdaDiscoveryClient::javaClass}"))
	}
}