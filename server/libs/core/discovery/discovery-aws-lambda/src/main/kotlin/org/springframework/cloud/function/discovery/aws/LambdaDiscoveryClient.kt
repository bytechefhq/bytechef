package org.springframework.cloud.function.discovery.aws

import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.*
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.GetFunctionRequest
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import java.net.URI

/**
 * A {@link DiscoveryClient} implementation that provides URLs for
 * functions that have been registered in AWS Lambda and then exposed
 * through an AWS API Gateway trigger. This implementation returns the URL
 * for the AWS API Gateway.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
open class LambdaDiscoveryClient(
		private val region: Regions,
		private val amazonApiGateway: AmazonApiGateway,
		private val lambda: AWSLambda) : DiscoveryClient {

	/**
	 * Returns a list of the logical names for AWS Lambda functions
	 */
	override fun getServices(): List<String> =
			(lambda.listFunctions()?.functions ?: emptyList())
					.map { it.functionName }
					.toMutableList()

	/**
	 * For any given service {@code foo} there's only one addressable URL in
	 * AWS (behind the gateway), so this returns that single URL.
	 */
	override fun getInstances(serviceId: String): MutableList<ServiceInstance> {
		val split = serviceId.split(':')
		val serviceName = split[0]
		val verbs: Array<String> = if (split.size > 1) {
			val lastPart: String = split[split.size - 1]
			if (lastPart.contains(',')) lastPart.split(',').toTypedArray() else arrayOf(lastPart)
		} else {
			arrayOf("GET", "POST", "DELETE", "OPTIONS", "ANY", "PUT")
		}
		val url: String? = urlByFunctionName(serviceName, methods = verbs)
		val res: MutableList<ServiceInstance>? = url?.let {
			val uri = URI.create(url)
			val si = SimpleServiceInstance(uri = uri, sid = serviceName) as ServiceInstance
			mutableListOf(si)
		}
		val results = mutableListOf<ServiceInstance>()
		res?.forEach { results.add(it) }
		return results
	}


	override fun description(): String = ("A discovery client that returns URIs " +
			"for AWS Lambda functions mapped to API Gateway endpoints")
			.trim()

	/**
	 * Finds a function by its logical name, then finds any REST APIs
	 * that have an integration with that function.
	 */
	private fun urlByFunctionName(functionName: String, methods: Array<String> = arrayOf("GET", "POST")): String? {

		data class PathContext(val resource: Resource,
		                       val integrationResult: GetIntegrationResult,
		                       val restApi: RestApi)

		return this.services
				.firstOrNull { it.toLowerCase().contains(functionName.toLowerCase()) } //
				?.let { match ->

					val fnArn = lambda.getFunction(GetFunctionRequest()
							.withFunctionName(match))
							.configuration
							.functionArn

					return amazonApiGateway
							.getRestApis(GetRestApisRequest())
							.items
							.flatMap { restApi ->

								val pathContexts: List<PathContext> = amazonApiGateway
										.getResources(GetResourcesRequest().withRestApiId(restApi.id))
										.items
										.flatMap { resource ->

											fun forMethod(method: String): PathContext? {
												val integration: GetIntegrationResult? =
														try {
															val integrationRequest = GetIntegrationRequest()
																	.withHttpMethod(method)
																	.withRestApiId(restApi.id)
																	.withResourceId(resource.id)
															amazonApiGateway.getIntegration(integrationRequest)
														} catch (e: Exception) {
															null
														}
												return integration?.let { PathContext(resource, it, restApi) }
											}

											methods.flatMap {
												forMethod(it)?.let { str -> listOf(str) } ?: emptyList()
											}
										}
								pathContexts
							}//
							.mapNotNull { ctx ->
								if (ctx.integrationResult.uri.contains(fnArn)) {
									"https://${ctx.restApi.id}.execute-api.${region.getName()}.amazonaws.com/Prod/${ctx.resource.pathPart}"
								} else
									null
							}
							.toSet()
							.first { true }
				}

	}
}

class SimpleServiceInstance(private val uri: URI, private val sid: String) : ServiceInstance {

	override fun getServiceId(): String = sid

	override fun getMetadata(): Map<String, String> = emptyMap()

	override fun getPort(): Int = uri.port

	override fun getHost() = uri.host

	override fun getUri(): URI = uri

	override fun isSecure(): Boolean = (uri.scheme
			?: "http").toLowerCase().contains("https")
}
