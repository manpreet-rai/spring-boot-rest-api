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

### REST, CRUD, and HTTP
Let’s start with a concise definition of REST: Representational State Transfer. In a RESTful system, data objects are called Resource Representations. The purpose of a RESTful API (Application Programming Interface) is to manage the state of these Resources.

Said another way, you can think of “state” being “value” and “Resource Representation” being an “object” or "thing". Therefore, REST is just a way to manage the values of things. Those things might be accessed via an API, and are often stored in a persistent data store, such as a database.

<img width="555" alt="image" src="https://github.com/user-attachments/assets/6c0b14fe-7172-42b5-9536-f5d2e46e4641">

A frequently mentioned concept when speaking about REST is CRUD. CRUD stands for “Create, Read, Update, and Delete”. These are the four basic operations that can be performed on objects in a data store. We’ll learn that REST has specific guidelines for implementing each one.

Another common concept associated with REST is the Hypertext Transfer Protocol. In HTTP, a caller sends a Request to a URI. A web server receives the request, and routes it to a request handler. The handler creates a Response, which is then sent back to the caller.

The components of the Request and Response are:

Request
- Method (also called Verb)
- URI (also called Endpoint)
- Body

Response
- Status Code
- Body

The power of REST lies in the way it references a Resource, and what the Request and Response look like for each CRUD operation. Let’s take a look at what our API will look like when we're done with this course:
- For CREATE: use HTTP method POST.
- For READ: use HTTP method GET.
- For UPDATE: use HTTP method PUT.
- For DELETE: use HTTP method DELETE.

The endpoint URI for Cash Card objects begins with the `/cashcards` keyword. READ, UPDATE, and DELETE operations require we provide the unique identifier of the target resource. The application needs this unique identifier in order to perform the correct action on exactly the correct resource. For example, to READ, UPDATE, or DELETE a Cash Card with the identifier of "42", the endpoint would be `/cashcards/42`.

Notice that we do not provide a unique identifier for the CREATE operation. As we'll learn in more detail in future lessons, CREATE will have the side effect of creating a new Cash Card with a new unique ID. No identifier should be provided when creating a new Cash Card because the application will create a new unique identifier for us.

The chart below has more details about RESTful CRUD operations.
| Operation |	API Endpoint | HTTP Method	Response Status |
|-----------|--------------|------------------------------|
|Create	| /`cashcards` | POST	201 (CREATED) |
|Read	| /`cashcards/{id}` | GET	200 (OK) |
|Update	| `/cashcards/{id}` | PUT	204 (NO CONTENT) |
|Delete	| `/cashcards/{id}` | DELETE	204 (NO CONTENT) |

The Request Body:
When following REST conventions to create or update a resource, we need to submit data to the API. This is often referred to as the request body. The CREATE and UPDATE operations require that a request body contain the data needed to properly create or update the resource. For example, a new Cash Card might have a beginning cash value amount, and an UPDATE operation might change that amount.

Cash Card Example:
Let’s use the example of a Read endpoint. For the Read operation, the URI (endpoint) path is `/cashcards/{id}`, where `{id}` is replaced by an actual Cash Card identifier, without the curly braces, and the HTTP method is GET.

In GET requests, the body is empty. So, the request to read the Cash Card with an id of 123 would be:

```yaml
Request:
  Method: GET
  URL: `http://cashcard.example.com/cashcards/123`
  Body: (empty)
The response to a successful Read request has a body containing the JSON representation of the requested Resource, with a Response Status Code of 200 (OK). Therefore, the response to the above Read request would look like this:

Response:
  Status Code: 200
  Body:
  {
    "id": 123,
    "amount": 25.00
  }
```
As we progress through this course, you’ll learn how to implement all of the remaining CRUD operations as well.

### REST in Spring Boot
Now that we’ve discussed REST in general, let’s look at the parts of Spring Boot that we’ll use to implement REST. Let’s start by discussing Spring’s IoC container.

**Spring Annotations and Component Scan**
One of the main things Spring does is to configure and instantiate objects. These objects are called Spring Beans, and are usually created by Spring (as opposed to using the Java new keyword). You can direct Spring to create Beans in several ways.

In this lesson, you’ll annotate a class with a Spring Annotation, which directs Spring to create an instance of the class during Spring’s Component Scan phase. This happens at application startup. The Bean is stored in Spring’s IoC container. From here, the bean can be injected into any code that requests it.

**Spring Web Controllers**
In Spring Web, Requests are handled by Controllers. In this lesson, you’ll use the more specific RestController:

```java
@RestController
  class CashCardController {
}
```
That’s all it takes to tell Spring: **“create a REST Controller”**. The Controller gets injected into Spring Web, which routes API requests (handled by the Controller) to the correct method.