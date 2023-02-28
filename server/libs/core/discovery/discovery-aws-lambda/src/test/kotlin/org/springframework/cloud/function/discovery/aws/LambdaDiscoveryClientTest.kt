package org.springframework.cloud.function.discovery.aws

import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.*
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.amazonaws.services.lambda.model.GetFunctionRequest
import com.amazonaws.services.lambda.model.GetFunctionResult
import com.amazonaws.services.lambda.model.ListFunctionsResult
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(classes = arrayOf(LambdaDiscoveryClientTest.MyApp::class))
@RunWith(SpringRunner::class)
class LambdaDiscoveryClientTest {

	@MockBean
	val amazonApiGateway: AmazonApiGateway? = null

	@MockBean
	val lambda: AWSLambda? = null

	@Import(AwsAutoConfiguration::class)
	@Configuration
	class MyApp

	@Autowired
	val ldc: DiscoveryClient? = null

	@Test
	fun getServices() {

		fun setUp() {
			val awsLambda = this.lambda!!
			val result = Mockito.mock(ListFunctionsResult::class.java)
			val fns = arrayListOf(FunctionConfiguration().withFunctionName("A"),
					FunctionConfiguration().withFunctionName("B"))
			Mockito.`when`(result.functions).thenReturn(fns)
			Mockito.`when`(awsLambda.listFunctions()).thenReturn(result)

		}

		setUp()

		val svcs = this.ldc!!.services
		Assertions.assertThat(svcs).contains("A")
		Assertions.assertThat(svcs).contains("B")
	}

	@Test
	fun getInstances() {

		val arn = "a:arn:thing"
		val integrationUri = "integrationUri${arn}"
		fun lambdaSetup() {

			val result = Mockito.mock(GetFunctionResult::class.java)
			val fnConfig = Mockito.mock(FunctionConfiguration::class.java)
			Mockito.`when`(fnConfig.functionArn).thenReturn(arn)
			Mockito.`when`(result.configuration).thenReturn(fnConfig)
			Mockito.`when`(this.lambda!!.getFunction(Mockito.any(GetFunctionRequest::class.java)))
					.thenReturn(result)


			val fc = FunctionConfiguration().withFunctionArn("arn").withFunctionName("serviceId")
			val lfr = ListFunctionsResult().withFunctions(arrayListOf(fc))
			Mockito.`when`(this.lambda!!.listFunctions()).thenReturn(lfr)
		}

		fun gwSetup() {

			val restApiId = "restApiIdNo1"
			val resourceId = "resourceIdNo1"

			val getRestApisResult = GetRestApisResult().withItems(arrayListOf(RestApi().withId(restApiId)))

			Mockito.`when`(this.amazonApiGateway!!.getRestApis(GetRestApisRequest())).thenReturn(getRestApisResult)

			val getResourcesResult = GetResourcesResult()
					.withItems(Resource().withId(resourceId))

			val getIntegrationRequest = GetIntegrationRequest()
					.withHttpMethod("ANY")
					.withRestApiId(restApiId)
					.withResourceId(resourceId)
			Mockito.`when`(this.amazonApiGateway!!.getIntegration(getIntegrationRequest))
					.thenReturn(GetIntegrationResult().withUri(integrationUri))

			Mockito.`when`(this.amazonApiGateway!!.getResources(GetResourcesRequest().withRestApiId(restApiId)))
					.thenReturn(getResourcesResult)

		}

		lambdaSetup()
		gwSetup()

		val instances = this.ldc!!.getInstances("serviceId")
		Assertions.assertThat(instances.size).isEqualTo(1) // there is always one instance.

	}

	@Test
	fun config() {
		Assertions.assertThat(this.ldc).isNotNull()
	}

	@Test
	fun description() {
		Assertions.assertThat(this.ldc!!.description()).isNotBlank()
	}
}