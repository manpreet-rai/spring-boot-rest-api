# REST API with Spring Boot

Use spring initializr to with following settings for now.
![initializr-metadata](https://github.com/user-attachments/assets/344f4272-063f-4601-a98e-523302a4971c)

Add Spring Web as dependency
![initializr-dependencies](https://github.com/user-attachments/assets/d5d19114-9d31-43c6-8cca-f3c52e39864b)

Download and extract the code, and use `./gradlew build` to finish building the generated code.
We will be implementing the following API contract for now.
```Request
  URI: /cashcards/{id}
  HTTP Verb: GET
  Body: None

Response:
  HTTP Status:
    200 OK if the user is authorized and the Cash Card was successfully retrieved
    401 UNAUTHORIZED if the user is unauthenticated or unauthorized
    404 NOT FOUND if the user is authenticated and authorized but the Cash Card cannot be found
  Response Body Type: JSON
  Example Response Body:
    {
      "id": 99,
      "amount": 123.45
    }
```

### The Red, Green, Refactor Loop
Software development teams love to move fast. So how do you go fast forever? By continuously improving and simplifying your code–refactoring. One of the only ways you can safely refactor is when you have a trustworthy test suite. Thus, the best time to refactor the code you're currently focusing on is during the TDD cycle. This is called the Red, Green, Refactor development loop:
- Red: Write a failing test for the desired functionality.
- Green: Implement the simplest thing that can work to make the test pass.
- Refactor: Look for opportunities to simplify, reduce duplication, or otherwise improve the code without changing any behavior—to refactor.
- Repeat!
  Throughout the labs in this course, you'll practice the Red, Green, Refactor loop to develop the Family Cash Card REST API.

Create a Test file first, write some tests, let them fail. Then turn them right before we move on to code. Create a Json Test file at `src/test/java/example/cashcard/CashCardJsonTest.java`
You need to explicitly import statically org.assertj library’s assertThat method. Also some annotations are needed like @JsonTest above the class name.
Look at the following code for this class `CashCardJsonTest.java`
```java
package example.cashcard;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;


import java.io.IOException;


/*
* assertThat needs this library. See it is imported as a static one.
* Static is used when we don't need to create object to use a method.
*/
import static org.assertj.core.api.Assertions.assertThat;


/**
* The @JsonTest annotation marks the CashCardJsonTest as a test class
* which uses the Jackson framework (which is included as part of Spring).
* This provides extensive JSON testing and parsing support.
* It also establishes all the related behavior to test JSON objects.
* <p>
* A common convention (but not a requirement) is to always use the Test suffix for test classes.
*/
@JsonTest
public class CashCardJsonTest {


   /**
    * JacksonTester is a convenience wrapper to the Jackson JSON parsing library.
    * It handles serialization and deserialization of JSON objects.
    * <p>
    * Also, @Autowired is an annotation that directs Spring to create an object of the requested type.
    * That object is created at application's boot time and is passed on when needed anywhere.
    * This concept is similar to singleton in Laravel (Dependency Injection, here IoC)
    * <p>
    * CashCard is a record, we created in our src/main.
    * It behaves like a struct datatype, which is used to store data.
    */
   @Autowired
   JacksonTester<CashCard> json;


   /**
    *  The @Test annotation is part of the JUnit library,
    *  and the assertThat method is part of the AssertJ library.
    *  <p>
    *  Deserialization is the reverse process of serialization.
    *  It transforms data from a file or byte stream back into an object for your application.
    *  This makes it possible for an object serialized on one platform to be deserialized on a different platform.
    *  <p>
    * Serialization and deserialization work together to transform/recreate data objects to/from a portable format.
    * The most popular data format for serializing data is JSON.
    */
   @Test
   void cashCardSerializationTest() throws IOException {
       CashCard cashCard = new CashCard(99L, 123.45);
       assertThat(json.write(cashCard)).isStrictlyEqualToJson("expected.json");
       assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.id");
       assertThat(json.write(cashCard)).extractingJsonPathNumberValue("@.id").isEqualTo(99);
       assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.amount");
       assertThat(json.write(cashCard)).extractingJsonPathNumberValue("@.amount").isEqualTo(123.45);
   }


   @Test
   void cashCardDeserializationTest() throws IOException {
       String expected = """
               {
                   "id": 99,
                   "amount": 123.45
               }
           """;


       assertThat(json.parse(expected)).isEqualTo(new CashCard(99L, 123.45));
       assertThat(json.parseObject(expected).id()).isEqualTo(99);
       assertThat(json.parseObject(expected).amount()).isEqualTo(123.45);
   }
}
```

The contents of `src/main/java/example/cashcard/CashCard.java` which is a record are as follows:
```java
package example.cashcard;

record CashCard(Long id, Double amount) {
}
```

The contents of `/src/test/resources/example/cashcard/expected.json` file are as follows:
```json
{
  "id": 99,
  "amount": 123.45
}
```

You can verify the build is successful by using `./gradlew build` command.

At this moment, the directory structure looks like this.
<img width="1131" alt="image" src="https://github.com/user-attachments/assets/253a2a9f-5027-4dfc-a1d6-602c767925a3">