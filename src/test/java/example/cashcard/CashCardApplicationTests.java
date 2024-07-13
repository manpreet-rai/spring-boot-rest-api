package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * This will start our Spring Boot application and make it available for our test to perform requests to it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {

	/**
	 * We've asked Spring to inject a test helper that’ll allow us to make
	 * HTTP requests to the locally running application.
	 */
	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		/**
		 * Here we use restTemplate to make an HTTP GET request to our application endpoint /cashcards/99.
		 * restTemplate will return a ResponseEntity, which we've captured in a variable we've named response.
		 * ResponseEntity is another helpful Spring object that provides valuable information about what happened with our request.
		 * We'll use this information throughout our tests in this course.
		 */
		ResponseEntity<String> response = restTemplate.getForEntity("/cashcards/99", String.class);

		/**
		 * We can inspect many aspects of the response, including the HTTP Response Status code,
		 * which we expect to be 200 OK.
		 */
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		/**
		 * This converts the response String into a JSON-aware object with lots of helper methods.
		 */
		DocumentContext documentContext = JsonPath.parse(response.getBody());

		/**
		 * We expect that when we request a Cash Card with id of 99 and amount of 123.45
		 * a JSON object will be returned with something in the id field.
		 * Assert that the id, and amount are valid.
		 */
		Number id = (Number) documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Double amount = (Double) documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	/**
	 * Let's write a new test that expects to ignore Cash Cards that do not have an id of 99.
	 * Use 1000, as we have in previous tests.
	 */
	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		ResponseEntity<String> response = restTemplate.getForEntity("/cashcards/1000", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}
}
