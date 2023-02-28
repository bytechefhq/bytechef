package org.springframework.cloud.function.discovery.aws

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.invoke.LambdaFunction
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory
import org.assertj.core.api.Assertions
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(classes = arrayOf(FunctionInvokerTest.Config::class))
@RunWith(SpringRunner::class)
class FunctionInvokerTest {

	@SpringBootApplication
	class Config

	@Autowired
	val amazonLambda: AWSLambda? = null

	interface UppercaseService {

		@LambdaFunction
		fun uppercase(request: UppercaseRequest): UppercaseResponse
	}

	data class UppercaseRequest(var incoming: String? = null)
	data class UppercaseResponse(var outgoing: String? = null)

	@Test
 	@Ignore
	fun invokeFunction() {
		val uppercaseService = LambdaInvokerFactory
				.builder()
				.lambdaClient(this.amazonLambda)
				.build(UppercaseService::class.java)
		val message = uppercaseService.uppercase(UppercaseRequest(incoming = "hello"))
		Assertions.assertThat(message.outgoing).isEqualTo("hello".toUpperCase())
	}
}