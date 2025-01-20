<img width="555" alt="image" src="https://github.com/user-attachments/assets/921ceeea-04eb-41fa-b758-21750ae8c3ee">

# REST API with Spring Boot

Use spring initializr to with following settings for now.

<img width="555" alt="image" src="https://github.com/user-attachments/assets/344f4272-063f-4601-a98e-523302a4971c">

Add Spring Web as dependency

<img width="555" alt="image" src="https://github.com/user-attachments/assets/d5d19114-9d31-43c6-8cca-f3c52e39864b">

Download and extract the code, and use `./gradlew build` to finish building the generated code.
We will be implementing the following **API contract** for now.
```yaml
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
- **Red**: Write a failing test for the desired functionality.
- **Green**: Implement the simplest thing that can work to make the test pass.
- **Refactor**: Look for opportunities to simplify, reduce duplication, or otherwise improve the code without changing any behavior—to refactor.
- **Repeat**!

Throughout this course, you'll practice the Red, Green, Refactor loop to develop the Family Cash Card REST API.

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
- For **CREATE**: use HTTP method **POST**.
- For **READ**: use HTTP method **GET**.
- For **UPDATE**: use HTTP method **PUT**.
- For **DELETE**: use HTTP method **DELETE**.

The endpoint URI for Cash Card objects begins with the `/cashcards` keyword. READ, UPDATE, and DELETE operations require we provide the unique identifier of the target resource. The application needs this unique identifier in order to perform the correct action on exactly the correct resource. For example, to READ, UPDATE, or DELETE a Cash Card with the identifier of "42", the endpoint would be `/cashcards/42`.

Notice that we do not provide a unique identifier for the CREATE operation. As we'll learn in more detail in future lessons, CREATE will have the side effect of creating a new Cash Card with a new unique ID. No identifier should be provided when creating a new Cash Card because the application will create a new unique identifier for us.

The chart below has more details about RESTful CRUD operations.

| Operation | API Endpoint      | HTTP Method Response Status |
|-----------|-------------------|-----------------------------|
| Create    | /`cashcards`      | POST 201 (CREATED)          |
| Read      | /`cashcards/{id}` | GET 200 (OK)                |
| Update    | /`cashcards/{id}` | PUT 204 (NO CONTENT)        |
| Delete    | /`cashcards/{id}` | DELETE 204 (NO CONTENT)     |


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

<img width="553" alt="image" src="https://github.com/user-attachments/assets/7575f305-8c0a-441a-a52a-d703da3dc9f9">

A Controller method can be designated a handler method, to be called when a request that the method knows how to handle (called a “matching request”) is received. Let’s write a Read request handler method! Here’s a start:
```java
private CashCard findById(Long requestedId) {
}
```
Since REST says that Read endpoints should use the HTTP GET method, you need to tell Spring to route requests to the method only on GET requests. You can use @GetMapping annotation, which needs the URI Path:
```java
@GetMapping("/cashcards/{requestedId}")
private CashCard findById(Long requestedId) {
}
```
Spring needs to know how to get the value of the requestedId parameter. This is done using the @PathVariable annotation. The fact that the parameter name matches the {requestedId} text within the @GetMapping parameter allows Spring to assign (inject) the correct value to the requestedId variable:
```java
@GetMapping("/cashcards/{requestedId}")
private CashCard findById(@PathVariable Long requestedId) {
}
```
REST says that the Response needs to contain a Cash Card in its body, and a Response code of 200 (OK). Spring Web provides the ResponseEntity class for this purpose. It also provides several utility methods to produce Response Entities. Here, you can use ResponseEntity to create a ResponseEntity with code 200 (OK), and a body containing a CashCard. The final implementation looks like this:
```java
@RestController
class CashCardController {
  @GetMapping("/cashcards/{requestedId}")
  private ResponseEntity<CashCard> findById(@PathVariable Long requestedId) {
    CashCard cashCard = /* Here would be the code to retrieve the CashCard */;
    return ResponseEntity.ok(cashCard);
  }
}
```

Now look at the following files, it has everything explained:
`src/test/java/example/cashcard/CashCardApplicationTests.java`
```java
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
```

Also, the file at `src/main/java/example/cashcard/CashCardController.java`
```java
package example.cashcard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @RestController
 * This tells Spring that this class is a Component of type RestController and capable of handling HTTP requests.
 * <p>
 * @RequestMapping("/cashcards")
 * This is a companion to @RestController that indicates which address requests must have to access this Controller.
 */
@RestController
@RequestMapping("/cashcards")
public class CashCardController {

  /**
   * If @RequestMapping is not used, @GetMapping must mention whole path like /cashcards/{requestedId}
   * @GetMapping marks a method as a handler method.
   * GET requests that match /cashcards/{requestedID} will be handled by this method.
   * <p>
   * @PathVariable makes Spring Web aware of the requestedId supplied in the HTTP request.
   * Now it’s available for us to use in our handler method.
   */
  @GetMapping("/{requestedId}")
  private ResponseEntity<CashCard> findById(@PathVariable Long requestedId) {
    if (requestedId.equals(99L)) {
      CashCard cashCard = new CashCard(99L, 123.45);
      return ResponseEntity.ok(cashCard);
    }
    else {
      return ResponseEntity.notFound().build();
    }
  }
}
```

Now at this point, the directory structure looks like this:

<img width="1131" alt="image" src="https://github.com/user-attachments/assets/57fdbed3-cfed-421a-9acc-a00b21bc6b61">

### Repositories & Spring Data
At this point in our development journey, we’ve got a system which returns a hard-coded Cash Card record from our Controller. However, what we really want is to return real data, from a database. So, let’s continue our Steel Thread by switching our attention to the database!

Spring Data works with Spring Boot to make database integration simple. Before we jump in, let’s briefly talk about Spring Data’s architecture.

**Controller-Repository Architecture**

The Separation of Concerns principle states that well-designed software should be modular, with each module having distinct and separate concerns from any other module.

Up until now, our codebase only returns a hard-coded response from the Controller. This setup violates the Separation of Concerns principle by mixing the concerns of a Controller, which is an abstraction of a web interface, with the concerns of reading and writing data to a data store, such as a database. In order to solve this, we’ll use a common software architecture pattern to enforce data management separation via the Repository pattern.

A common architectural framework that divides these layers, typically by function or value, such as business, data, and presentation layers, is called Layered Architecture. In this regard, we can think of our Repository and Controller as two layers in a Layered Architecture. The Controller is in a layer near the Client (as it receives and responds to web requests) while the Repository is in a layer near the data store (as it reads from and writes to the data store). There may be intermediate layers as well, as dictated by business needs. We don't need any additional layers, at least not yet!

The Repository is the interface between the application and the database, and provides a common abstraction for any database, making it easier to switch to a different database when needed.

<img width="557" alt="image" src="https://github.com/user-attachments/assets/03d62ba4-a14f-4951-9dbb-bad498603b4f">

In good news, Spring Data provides a collection of robust data management tools, including implementations of the Repository pattern.

**Choosing a Database**

For our database selection, we’ll use an embedded, in-memory database. “Embedded” simply means that it’s a Java library, so it can be added to the project just like any other dependency. “In-memory” means that it stores data in memory only, as opposed to persisting data in permanent, durable storage. At the same time, our in-memory database is largely compatible with production-grade relational database management systems (RDBMS) like MySQL, SQL Server, and many others. Specifically, it uses JDBC (the standard Java library for database connectivity) and SQL (the standard database query language).

<img width="556" alt="image" src="https://github.com/user-attachments/assets/64fdad64-1f83-4f89-b050-8e0468738946">

There are tradeoffs to using an in-memory database instead of a persistent database. On one hand, in-memory allows you to develop without installing a separate RDBMS, and ensures that the database is in the same state (i.e., empty) on every test run. However, you do need a persistent database for the live "production" application. This leads to a Dev-Prod Parity mismatch: Your application might behave differently when running the in-memory database than when running in production.

The specific in-memory database we’ll use is H2. Fortunately, H2 is highly compatible with other relational databases, so dev-prod parity won’t be a big issue. We’ll use H2 for convenience for local development, but we want to recognize the tradeoffs.

**Auto Configuration**

In the lab, all we need for full database functionality is to add two dependencies. This wonderfully showcases one of the most powerful features of Spring Boot: Auto Configuration. Without Spring Boot, we’d have to configure Spring Data to speak to H2. However, because we’ve included the Spring Data dependency (and a specific data provider, H2), Spring Boot will automatically configure your application to communicate with H2.

**Spring Data’s CrudRepository**

For our Repository selection, we’ll use a specific type of Repository: Spring Data’s CrudRepository. At first glance, it’s slightly magical, but let’s unpack that magic.

The following is a complete implementation of all CRUD operations by extending CrudRepository:
```java
interface CashCardRepository extends CrudRepository<CashCard, Long> {
}
```

With just the above code, a caller can call any number of predefined CrudRepository methods, such as findById:
```java
cashCard = cashCardRepository.findById(99);
```
You might immediately wonder: Where is the implementation of the CashCardRepository.findById() method? CrudRepository and everything it inherits from is an Interface with no actual code! Well, based on the specific Spring Data framework used (which for us will be Spring Data JDBC) Spring Data takes care of this implementation for us during the IoC container startup time. The Spring runtime will then expose the repository as yet another bean that you can reference wherever needed in your application.

As we’ve learned, there are typically trade-offs. For example the CrudRepository generates SQL statements to read and write your data, which is useful for many cases, but sometimes you need to write your own custom SQL statements for specific use cases.

Look into the following files:
- `src/main/resources/schema.sql`
- `src/test/resources/data.sql`

**Add Spring Data Dependencies**

This project was originally created using the Spring Initializr, which allowed us to automatically add dependencies to our project. However, now we must manually add dependencies to our project.

Add dependencies for Spring Data and a database.

In `build.gradle`:
```groovy
dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-web'
  testImplementation 'org.springframework.boot:spring-boot-starter-test'

// Add the two dependencies below
  implementation 'org.springframework.data:spring-data-jdbc:3.3.2'
  implementation 'com.h2database:h2'
}
```
**Understand the dependencies**

The two dependencies we added are related, but different.

```groovy
implementation 'org.springframework.data:spring-data-jdbc:3.3.2'
```
Spring Data has many implementations for a variety of relational and non-relational database technologies. Spring Data also has several abstractions on top of those technologies. These are commonly called an Object-Relational Mapping framework, or ORM.

Here we'll elect to use Spring Data JDBC. From the Spring Data JDBC documentation:

> Spring Data JDBC aims at being conceptually easy...This makes Spring Data JDBC a simple, limited, opinionated ORM.

```groovy
implementation 'com.h2database:h2'
```
Database management frameworks only work if they have a linked database. H2 is a "very fast, open source, JDBC API" SQL database implemented in Java. It works seamlessly with Spring Data JDBC.

At this point, the contents of `build.gradle` are as follows:
```groovy
plugins {
  id 'java'
  id 'org.springframework.boot' version '3.3.1'
  id 'io.spring.dependency-management' version '1.1.5'
}

group = 'example'
version = '0.0.1-SNAPSHOT'

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(22)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-web'
  testImplementation 'org.springframework.boot:spring-boot-starter-test'
  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

  // Spring Data Dependencies
  implementation 'org.springframework.data:spring-data-jdbc:3.3.2'
  implementation 'com.h2database:h2'
}

tasks.named('test') {
  useJUnitPlatform()
}

// This section is optional. It helps show more information on debug
test {
  testLogging {
    events "passed", "skipped", "failed" //, "standardOut", "standardError"

    showExceptions true
    exceptionFormat "full"
    showCauses true
    showStackTraces true

    // Change from false to true
    showStandardStreams = false
  }
}
```

**Create the CashCardRepository**

Create `src/main/java/example/cashcard/CashCardRepository.java` **interface** and have it extend CrudRepository.
```java
package example.cashcard;

// This import uses org.springframework.data:spring-data-jdbc:3.3.2
import org.springframework.data.repository.CrudRepository;

/**
 * We do not need to provide implementation of CashCardRepository, Spring will take care of it
 * This is how Spring handles auto-implementation at boot time.
 * <p>
 * CrudRepository needs to be told the model name and primary key type, which helps it in
 * fetching the required data using that model and primary key.
 * <p>
 * Here we used a Record CashCard and in it, primary key is id which has a type of Long.
 */
interface CashCardRepository extends CrudRepository<CashCard, Long> {
}
```
**Understand extends CrudRepository**

This is where we tap into the magic of Spring Data and its data repository pattern.

CrudRepository is an interface supplied by Spring Data. When we extend it (or other sub-Interfaces of Spring Data's Repository), Spring Boot and Spring Data work together to automatically generate the CRUD methods that we need to interact with a database.

We'll use one of these CRUD methods, `findById`, later in this project.

> **Note:** When we configure the repository as `CrudRepository<CashCard, Long>` we indicate that the CashCard's ID is Long. However, we still need to tell Spring Data which field is the ID.

Edit the `CashCard.java` class to configure the **id** as the `@Id` for the CashCardRepository.
```java
package example.cashcard;

// Add this import
import org.springframework.data.annotation.Id;

/**
 * id field is marked with @Id annotation
 * This let Spring know that id is primary key, to be used by CrudRepository.
 */
record CashCard(@Id Long id, Double amount) {
}
```

**Inject the CashCardRepository**

Although we've configured our `CashCard` and `CashCardRepository` classes, we haven't utilized the new `CashCardRepository` to manage our `CashCard` data. Let's do that now.

Inject the CashCardRepository into `CashCardController.java` and use it.
Final contents of `CashCardController.java` are as follows:
```java
package example.cashcard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Add this import
import java.util.Optional;

/**
 * @RestController
 * This tells Spring that this class is a Component of type RestController and capable of handling HTTP requests.
 * <p>
 * @RequestMapping("/cashcards")
 * This is a companion to @RestController that indicates which address requests must have to access this Controller.
 */
@RestController
@RequestMapping("/cashcards")
public class CashCardController {

  /**
   * Declare private object for CashCardRepository
   */
  private final CashCardRepository cashCardRepository;

  /**
   * Let the autoconfiguration and constructor injection handle initialization.
   * The constructor is private not public. Spring can handle its auto-initialization.
   */
  private CashCardController(CashCardRepository cashCardRepository) {
    this.cashCardRepository = cashCardRepository;
  }

  /**
   * If @RequestMapping is not used, @GetMapping must mention whole path like /cashcards/{requestedId}
   * @GetMapping marks a method as a handler method.
   * GET requests that match /cashcards/{requestedID} will be handled by this method.
   * <p>
   * @PathVariable makes Spring Web aware of the requestedId supplied in the HTTP request.
   * Now it’s available for us to use in our handler method.
   */
  @GetMapping("/{requestedId}")
  private ResponseEntity<CashCard> findById(@PathVariable Long requestedId) {
    /**
     * Optional allows to fetch conditional data, which may or may not be there in database.
     * And this can be used further to get the data or just build an empty response.
     */
    Optional<CashCard> optionalCashCard = cashCardRepository.findById(requestedId);

    /**
     * If data based on requestedId exists, get the data otherwise build empty response.
     */
    if (optionalCashCard.isPresent()) {
      return ResponseEntity.ok(optionalCashCard.get());
    }
    else {
      return ResponseEntity.notFound().build();
    }
  }
}
```
**Behold Auto Configuration and Construction Injection!**

Spring's Auto Configuration is utilizing its dependency injection (DI) framework, specifically constructor injection, to supply CashCardController with the correct implementation of CashCardRepository at runtime.

Magical stuff!

> **Learning Moment**: We have just beheld the glory of DI autoconfiguration and constructor injection.

**Understand the changes**

We've just altered the `CashCardController.findById` in several important ways.

```java 
Optional<CashCard> cashCardOptional = cashCardRepository.findById(requestedId);
```
We're calling `CrudRepository.findById`, which returns an `Optional`. This smart object might or might not contain the CashCard for which we're searching.

`cashCardOptional.isPresent()` and `cashCardOptional.get()` determine if findById did or did not find the CashCard with the supplied id.

If `cashCardOptional.isPresent()` is `true`, then the repository successfully found the CashCard and we can retrieve it with `cashCardOptional.get()`. If not, the repository has not found the `CashCard`.

**Run the tests.**

We can see that the tests fail with a `500 INTERNAL_SERVER_ERROR`.
```shell
CashCardApplicationTests > shouldReturnACashCardWhenDataIsSaved() FAILED
org.opentest4j.AssertionFailedError:
expected: 200 OK
but was: 500 INTERNAL_SERVER_ERROR
```
This means the Cash Card API **"crashed"**.

We need a bit more information...

Let's temporarily update the test output section of build.gradle with `showStandardStreams = true`, so that our test runs will produce a lot more output.
```groovy
test {
  testLogging {
    events "passed", "skipped", "failed" //, "standardOut", "standardError"

    showExceptions true
    exceptionFormat "full"
    showCauses true
    showStackTraces true

    // Change from false to true
    showStandardStreams = true       // <-- here
  }
}
```

**Rerun the tests.**

Note that the test output is much more verbose.

Searching through the output we find these failures:
```shell
org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "CASH_CARD" not found (this database is empty); SQL statement:
SELECT "CASH_CARD"."ID" AS "ID", "CASH_CARD"."AMOUNT" AS "AMOUNT" FROM "CASH_CARD" WHERE "CASH_CARD"."ID" = ? [42104-214]
The cause of our test failures is clear: Table "CASH_CARD" not found means we don't have a database nor any data.
```

**Configure the Database**

Our tests expect the API to find and return a `CashCard` with id of `99`. However, we just removed the hard-coded `CashCard` data and replaced it with a call to `cashCardRepository.findById`.

Now our application is crashing, complaining about a missing database table named `CASH_CARD`:

```shell
org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "CASH_CARD" not found (this database is empty);
```

We need to help Spring Data configure the database and load some sample data, such as our friend, CashCard 99.

Spring Data and H2 can automatically create and populate the in-memory database we need for our test. We've provided these files for you, but you'll need to amend them: `schema.sql` and `data.sql`.

> **Note**: Providing `schema.sql` and `data.sql` is one of many ways Spring provides to easily initialize a database.

As mentioned above, Spring Data will automatically configure a database for tests if we provide the correct file in the correct location.

Create `src/main/resources/schema.sql` file with following contents:
```sql
CREATE TABLE cash_card
(
  ID     BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  AMOUNT NUMBER NOT NULL DEFAULT 0
);
```

Understand `schema.sql`.

> A database schema is a "blueprint" for how data is stored in a database.

Our database schema reflects the `CashCard` object that we understand, which contains an `id` and an `amount`.

**Rerun the tests.**

> **Note:** If the test output is too verbose, revert the change in `build.gradle` performed previously.

Our tests no longer crash with a `500 INTERNAL_SERVER_ERROR`. However, now we get a `404 NOT_FOUND`.
```shell
CashCardApplicationTests > shouldReturnACashCardWhenDataIsSaved() FAILED
org.opentest4j.AssertionFailedError:
expected: 200 OK
but was: 404 NOT_FOUND
```

> **Translation:** Our repository can't find CashCard with id of 99. So, why not?

Although we've helped Spring Data create a test database by creating `schema.sql`, it's still an **empty database**.

Let's go load some test data from `data.sql`.

Not only can Spring Data create our test database, it can also load data into it, which we can use in our tests.

Similar to `schema.sql`, create `src/test/resources/data.sql`, with following contents:
```sql
INSERT INTO CASH_CARD(ID, AMOUNT) VALUES (99, 123.45);
```

This SQL statement inserts a row into the `CASH_CARD` table with an `ID=99` and `AMOUNT=123.45`, which matches the values we expect in our tests.

**Rerun the tests.**
```shell
[~/cashcard] $ ./gradlew test
...
BUILD SUCCESSFUL in 7s
```

Success! We're now using real data in our API.

> **Learning Moment:** `main` vs `test` resources <br>
Notice that `src/main/resources/schema.sql` and `src/test/resources/data.sql` are in different resources locations.

Remember that our Cash Card with `ID 99` and `Amount 123.45` is a **fake**, made-up Cash Card that we only want to use in our tests.
We don't want our "real" or production system to load Cash Card 99 into the system.

Spring has provided a powerful feature for us: it allows us to separate our **test-only resources** from our **main resources** when needed.
Our scenario here is a common example of this: **our database schema is always the same, but our data is not!**

Finally, the directory and file structure looks like this:

<img width="1131" alt="image" src="https://github.com/user-attachments/assets/1997973d-dd4e-409c-9de9-964addad867f">

### Implementing POST
Our REST API can now fetch Cash Cards with a specific ID. In this lesson, you’ll add the Create endpoint to the API.

Four questions we’ll need to answer while doing this are:

Who specifies the ID - the client, or the server?
In the API Request, how do we represent the object to be created?
Which HTTP method should we use in the Request?
What does the API send as a Response?
Let’s start by answering the first question: “Who specifies the ID?” In reality, this is up to the API creator! REST is not exactly a standard; it’s merely a way to use HTTP to perform data operations. REST contains a number of guidelines, many of which we’re following in this course.

Here we’ll choose to let the server create the ID. Why? Because it’s the simplest solution, and databases are efficient at managing unique IDs. However, for completeness, let’s discuss our alternatives:

We could require the client to provide the ID. This might make sense if there were a pre-existing unique ID, but that’s not the case.
We could allow the client to provide the ID optionally (and create it on the server if the client does not supply it). However, we don’t have a requirement to do this, and it would complicate our application. If you think you might want to do this “just in case”, the Yagni article (link in the References section) might dissuade you.
Before answering the third question, “Which HTTP method should be used in the Request?”, let’s talk about the relevant concept of idempotence.

**Idempotence and HTTP**
An idempotent operation is defined as one which, if performed more than once, results in the same outcome. In a REST API, an idempotent operation is one that even if it were to be performed several times, the resulting data on the server would be the same as if it had been performed only once.

For each method, the HTTP standard specifies whether it is idempotent or not. GET, PUT, and DELETE are idempotent, whereas POST and PATCH are not.

Since we’ve decided that the server will create IDs for every Create operation, the Create operation in our API is NOT idempotent. Since the server will create a new ID (on every Create request), if you call Create twice - even with the same content - you’ll end up with two different objects with the same content, but with different IDs. That was a mouthful, so to summarize: Every Create request will generate a new ID, thus no idempotency.

![image](https://github.com/user-attachments/assets/3407e638-0d2b-469e-9ca6-fdd1f98c1be3)

This leaves us with the POST and PATCH options. As it turns out, REST permits POST as one of the proper methods to use for Create operations, so we'll use it. We’ll revisit PATCH in a later lesson.

**The POST Request and Response**

Now let’s talk about the content of the POST Request, and the Response.

`The Request`
The POST method allows a Body, so we'll use the Body to send a JSON representation of the object:

```
Request:

Method: POST
URI: /cashcards/
Body:
{
    amount: 123.45
}
```

In contrast, if you recall from a previous lesson, the GET operation includes the ID of the Cash Card in the URI, but not in the request Body.

So why is there no ID in the Request? Because we decided to allow the server to create the ID. Thus, the data contract for the Read operation is different from that of the Create operation.

`The Response`
Let's move on to the Response. On successful creation, what HTTP Response Status Code should be sent? We could use 200 OK (the response that Read returns), but there’s a more specific, more accurate code for REST APIs: 201 CREATED.

The fact that CREATED is the name of the code makes it seem intuitively appropriate, but there’s another, more technical reason to use it: A response code of 200 OK does not answer the question “Was there any change to the server data?”. By returning the 201 CREATED status, the API is specifically communicating that data was added to the data store on the server.

In a previous lesson you learned that an HTTP Response contains two things: a Status Code, and a Body. But that’s not all! A Response also contains Headers. Headers have a name and a value. The HTTP standard specifies that the Location header in a 201 CREATED response should contain the URI of the created resource. This is handy because it allows the caller to easily fetch the new resource using the GET endpoint (the one we implemented prior).

Here is the complete Response:

```
Response:

Status Code: 201 CREATED
Header: Location=/cashcards/42
```

**Spring Web Convenience Methods**

In the accompanying lab, you’ll see that Spring Web provides methods which are geared towards the recommended use of HTTP and REST.

For example, we’ll use the ResponseEntity.created(uriOfCashCard) method to create the above response. This method requires you to specify the location, ensures the Location URI is well-formed (by using the URI class), adds the Location header, and sets the Status Code for you. And by doing so, this saves us from using more verbose methods. For example, the following two code snippets are equivalent (as long as uriOfCashCard is not null):

```java
return  ResponseEntity
        .created(uriOfCashCard)
        .build();
```

Versus:
```java
return ResponseEntity
        .status(HttpStatus.CREATED)
        .header(HttpHeaders.LOCATION, uriOfCashCard.toASCIIString())
        .build();
```

Aren’t you glad Spring Web provides the .created() convenience method?

Let's get started with HTTP POST in Spring.

**Test the HTTP POST Endpoint**

Add a test for the POST endpoint.

The simplest example of success is a non-failing HTTP POST request to our Family Cash Card API. We'll test for a 200 OK response instead of a 201 CREATED for now. Don't worry, we'll change this soon.

Edit `src/test/java/example/cashcard/CashCardApplicationTests.java` and add the following test method.
```java
@Test
void shouldCreateANewCashCard() {
   CashCard newCashCard = new CashCard(null, 250.00);
   ResponseEntity<Void> createResponse = restTemplate.postForEntity("/cashcards", newCashCard, Void.class);
   assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

**Understand the test.**

```java
CashCard newCashCard = new CashCard(null, 250.00);
```

The database will create and manage all unique `CashCard.id` values for us. We shouldn't provide one.
```java
restTemplate.postForEntity("/cashcards", newCashCard, Void.class);
```
This is very similar to `restTemplate.getForEntity`, but we must also provide newCashCard data for the new CashCard.

In addition, and unlike `restTemplate.getForEntity`, we don't expect a `CashCard` to be returned to us, so we expect a `Void` response body.

Run the tests.

We'll always use `./gradlew test` to run our tests.

`[~/exercises] $ ./gradlew test`

What do you expect will happen?

```
CashCardApplicationTests > shouldCreateANewCashCard() FAILED
   org.opentest4j.AssertionFailedError:
   expected: 200 OK
   but was: 404 NOT_FOUND
```

We shouldn't be surprised by the `404 NOT_FOUND error`. We haven't added the POST endpoint yet!

**Add the POST endpoint**

The POST endpoint is similar to the GET endpoint in our CashCardController, but uses the `@PostMapping` annotation from Spring Web.

The POST endpoint must accept the data we are submitting for our new CashCard, specifically the amount.

But what happens if we don't accept the CashCard?

Add the POST endpoint without accepting CashCard data.

Edit `src/main/java/example/cashcard/CashCardController.java` and add the following method.

Don't forget to add the import for PostMapping.

```java
import org.springframework.web.bind.annotation.PostMapping;
...

@PostMapping
private ResponseEntity<Void> createCashCard() {
   return null;
}
```

Note that by returning nothing at all, Spring Web will automatically generate an HTTP Response Status code of 200 OK. But, this isn't very satisfying -- our POST endpoint does nothing!

So let's make our tests better.

**Testing based on semantic correctness**

We want our Cash Card API to behave as semantically correctly as possible. Meaning, users of our API shouldn't be surprised by how it behaves.

Let's refer to the official Request for Comments for HTTP Semantics and Content (RFC 9110) for guidance as to how our API should behave.

For our POST endpoint, review this section about HTTP POST; note that we've added emphasis:

If one or more resources has been created on the origin server as a result of successfully processing a POST request, the origin server SHOULD send a 201 (Created) response containing a Location header field that provides an identifier for the primary resource created ...

We'll explain more about this specification as we write our test.

Let's start by updating the POST test.

Update the shouldCreateANewCashCard test.

Here's how we'll encode the HTTP specification as expectations in our test. Be sure to add the additional import.

```java
import java.net.URI;
...

@Test
void shouldCreateANewCashCard() {
   CashCard newCashCard = new CashCard(null, 250.00);
   ResponseEntity<Void> createResponse = restTemplate.postForEntity("/cashcards", newCashCard, Void.class);
   assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

   URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
   ResponseEntity<String> getResponse = restTemplate.getForEntity(locationOfNewCashCard, String.class);
   assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

**Run the tests.**

Unsurprisingly, they fail on the first changed assertion.
```
expected: 201 CREATED
  but was: 200 OK
```
Let's start fixing stuff!

Update the `createCashCard` method to the following definition
```java
@PostMapping
private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb) {
   CashCard savedCashCard = cashCardRepository.save(newCashCardRequest);
   URI locationOfNewCashCard = ucb
            .path("cashcards/{id}")
            .buildAndExpand(savedCashCard.id())
            .toUri();
   return ResponseEntity.created(locationOfNewCashCard).build();
}
```

**Understand CrudRepository.save**

This line in `CashCardController.createCashCard` is deceptively simple:

```java
CashCard savedCashCard = cashCardRepository.save(newCashCardRequest);
```
As learned in previous lessons and labs, Spring Data's CrudRepository provides methods that support creating, reading, updating, and deleting data from a data store. cashCardRepository.save(newCashCardRequest) does just as it says: it saves a new CashCard for us, and returns the saved object with a unique id provided by the database. Amazing!

**Understand the other changes to CashCardController**

Our CashCardController now implements the expected input and results of an HTTP POST.

`createCashCard(@RequestBody CashCard newCashCardRequest, ...)`
Unlike the GET we added earlier, the POST expects a request "body". This contains the data submitted to the API. Spring Web will deserialize the data into a CashCard for us.

```java
URI locationOfNewCashCard = ucb
   .path("cashcards/{id}")
   .buildAndExpand(savedCashCard.id())
   .toUri();
```
This is constructing a URI to the newly created CashCard. This is the URI that the caller can then use to GET the newly-created CashCard.

Note that savedCashCard.id is used as the identifier, which matches the GET endpoint's specification of cashcards/<CashCard.id>.

Where did UriComponentsBuilder come from?

We were able to add UriComponentsBuilder ucb as a method argument to this POST handler method and it was automatically passed in. How so? It was injected from our now-familiar friend, Spring's IoC Container. Thanks, Spring Web!

`return ResponseEntity.created(locationOfNewCashCard).build();`
Finally, we return 201 CREATED with the correct Location header.

**Add more test assertions.**

If you'd like, add more test assertions for the new id and amount to solidify your learning.

```java
...
assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

// Add assertions such as these
DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
Number id = documentContext.read("$.id");
Double amount = documentContext.read("$.amount");

assertThat(id).isNotNull();
assertThat(amount).isEqualTo(250.00);
```

The additions verify that the new CashCard.id is not null, and the newly created CashCard.amount is 250.00, just as we specified at creation time.

### Returning a list with GET
Now that our API can create Cash Cards, it’s reasonable to learn how to fetch all (or some!) of the Cash Cards. In this lesson, we’ll implement the “Read Many” endpoint, and understand how this operation differs substantially from the Read endpoint that we previously created.

**Requesting a List of Cash Cards**

We can expect each of our Family Cash Card users to have a few cards: imagine one for each of their family members, and perhaps a few that they gave as gifts. The API should be able to return multiple Cash Cards in response to a single REST request.

When you make an API request for several Cash Cards, you’d ideally make a single request, which returns a list of Cash Cards. So, we’ll need a new data contract. Instead of a single Cash Card, the new contract should specify that the response is a JSON Array of Cash Card objects:

```json
[
  {
    "id": 1,
    "amount": 123.45
  },
  {
    "id": 2,
    "amount": 50.0
  }
]
```
It turns out that our old friend, CrudRepository, has a findAll method that we can use to easily fetch all the Cash Cards in the database. Let's go ahead and use that method. At first glance, it looks quite simple:

```java
@GetMapping()
private ResponseEntity<Iterable<CashCard>> findAll() {
   return ResponseEntity.ok(cashCardRepository.findAll());
}
```
However, it turns out there’s a lot more to this operation than just returning all the Cash Cards in the database. Some questions come to mind:

How do I return only the Cash Cards that the user owns? (Great question! We’ll discuss this in the upcoming Spring Security lesson).
What if there are hundreds (or thousands?!) of Cash Cards? Should the API return an unlimited number of results or return them in “chunks”? This is known as Pagination.
Should the Cash Cards be returned in a particular order (i.e., should they be sorted?)?
We’ll leave the first question for later, but answer the pagination and sorting questions in this lesson. Let’s press on!

**Pagination and Sorting**

To start our pagination and sorting work, we’ll use a specialized version of the CrudRepository, called the PagingAndSortingRepository. As you might guess, this does exactly what its name suggests. But first, let’s talk about the “Paging” functionality.

Even though we’re unlikely to have users with thousands of Cash Cards, we never know how users might use the product. Ideally, an API should not be able to produce a response with unlimited size, because this could overwhelm the client or server memory, not to mention taking quite a long time!

In order to ensure that an API response doesn’t include an astronomically large number of Cash Cards, let’s utilize Spring Data’s pagination functionality. Pagination in Spring (and many other frameworks) is to specify the page length (e.g. 10 items), and the page index (starting with 0). For example, if a user has 25 Cash Cards, and you elect to request the second page where each page has 10 items, you would request a page of size 10, and page index of 1.

Bingo! Right? But wait, this brings up another hurdle. In order for pagination to produce the correct page content, the items must be sorted in some specific order. Why? Well, let’s say we have a bunch of Cash Cards with the following amounts:

```
$0.19 (this one’s pretty much all gone, oh well!)
$1,000.00 (this one is for emergency purchases for a university student)
$50.00
$20.00
$10.00 (someone gifted this one to your niece for her birthday)
```
Now let’s go through an example using a page size of 3. Since there are 5 Cash Cards, we’d make two requests in order to return all of them. Page 1 (index 0) contains three items, and page 2 (index 1, the last page) contains 2 items. But which items go where? If you specify that the items should be sorted by amount in descending order, then this is how the data is paginated:

```
Page 1:
$1,000.00
$50.00
$20.00

Page 2:
$10.00
$0.19
```

**Regarding Unordered Queries**

Although Spring does provide an “unordered” sorting strategy, let’s be explicit when we select which fields for sorting. Why do this? Well, imagine you elect to use “unordered” pagination. In reality, the order is not random, but instead predictable; it never changes on subsequent requests. Let’s say you make a request, and Spring returns the following “unordered” results:

```
Page 1:
$0.19
$1,000.00
$50.00

Page 2:
$20.00
$10.00
```
Although they look random, every time you make the request, the cards will come back in exactly this order, so that each item is returned on exactly one page.

Now for the punchline: Imagine you now create a new Cash Card with an amount of $42.00. Which page do you think it will be on? As you might guess, there’s no way to know other than making the request and seeing where the new Cash Card lands.

So how can we make this a bit more useful? Let’s opt for ordering by a specific field. There are a few good reasons to do so, including:
- Minimize cognitive overhead: Other developers (not to mention users) will probably appreciate a thoughtful ordering when developing it.
- Minimize future errors: What happens when a new version of Spring, or Java, or the database, suddenly causes the “random” order to change overnight?

**Spring Data Pagination API**

Thankfully, Spring Data provides the PageRequest and Sort classes for pagination. Let’s look at a query to get page 2 with page size 10, sorting by amount in descending order (largest amounts first):

```java
Page<CashCard> page2 = cashCardRepository.findAll(
    PageRequest.of(
        1,  // page index for the second page - indexing starts at 0
        10, // page size (the last page might have fewer items)
        Sort.by(new Sort.Order(Sort.Direction.DESC, "amount"))));
```

**The Request and Response**

Now let’s use Spring Web to extract the data to feed the pagination functionality:
- Pagination: Spring can parse out the page and size parameters if you pass a Pageable object to a PagingAndSortingRepository find…()method.
- Sorting: Spring can parse out the sort parameter, consisting of the field name and the direction separated by a comma – but be careful, no space before or after the comma is allowed! Again, this data is part of the Pageable object.

**The URI**

Now let’s learn how we can compose a URI for the new endpoint, step-by-step (we've omitted the https://domain prefix in the following):
- Get the second page
```/cashcards?page=1```

- …where a page has length of 3
```/cashcards?page=1&size=3```

- …sorted by the current Cash Card balance
```/cashcards?page=1&size=3&sort=amount```

- …in descending order (highest balance first)
```/cashcards?page=1&size=3&sort=amount,desc```

**The Java Code**

Let’s go over the complete implementation of the Controller method for our new “get a page of Cash Cards” endpoint:
```java
@GetMapping
private ResponseEntity<List<CashCard>> findAll(Pageable pageable) {
   Page<CashCard> page = cashCardRepository.findAll(
           PageRequest.of(
                   pageable.getPageNumber(),
                   pageable.getPageSize(),
                   pageable.getSortOr(Sort.by(Sort.Direction.DESC, "amount"))));
   return ResponseEntity.ok(page.getContent());
}
```

Let’s dive into a bit more detail:
- First let’s parse the needed values out of the query string:
  - We use Pageable, which allows Spring to parse out the page number and size query string parameters.
    - Note: If the caller doesn’t provide the parameters, Spring provides defaults: page=0, size=20.
- We use getSortOr() so that even if the caller doesn’t supply the sort parameter, there is a default. Unlike the page and size parameters, for which it makes sense for Spring to supply a default, it wouldn’t make sense for Spring to arbitrarily pick a sort field and direction.
- We use the page.getContent() method to return the Cash Cards contained in the Page object to the caller.

So, what does the Page object contain besides the Cash Cards? Here's the Page object in JSON format. The Cash Cards are contained in the content. The rest of the fields contain information about how this Page is related to other Pages in the query.

```json
{
  "content": [
    {
      "id": 1,
      "amount": 10.0
    },
    {
      "id": 2,
      "amount": 0.19
    }
  ],
  "pageable": {
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 3,
    "pageNumber": 1,
    "pageSize": 3,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 5,
  "totalPages": 2,
  "first": false,
  "size": 3,
  "number": 1,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 2,
  "empty": false
}
```

Although we could return the entire Page object to the client, we don't need all that information. We'll define our data contract to only return the Cash Cards, not the rest of the Page data.

Test for an Additional GET Endpoint
Write a failing test for a new GET endpoint.

Let's add a new test method which expects a GET endpoint which returns multiple CashCard objects.

In CashCardApplicationTests.java, add a new test:

 @Test
 void shouldReturnAllCashCardsWhenListIsRequested() {
     ResponseEntity<String> response = restTemplate.getForEntity("/cashcards", String.class);
     assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
 }
Here we're making a request to the /cashcards endpoint. Since we're getting the entire list of cards, we don't need to specify any additional information in the request.

Run the tests and observe the failure.

The test should fail because we haven't implemented a Controller endpoint to handle this GET request.

How do you think it will fail? Perhaps a 404 NOT FOUND?

In a previous lesson we wrote a test that failed because no endpoint yet existed to match the route being requested. The result was a 404 NOT FOUND error. We might expect the same thing to happen when we run the new test, since we haven't added any code to the Controller.

Let's see what happens. Run the test and search for the following failure:

expected: 200 OK
 but was: 405 METHOD_NOT_ALLOWED
The error messages don't make it clear why we're receiving a 405 METHOD_NOT_ALLOWED error. The reason is a bit hard to discover, so we'll quickly summarize it: We've already implemented a /cashcards endpoint, but not for a GET verb.

This is Spring's process:

Spring receives a request to the /cashcards endpoint.
There's no mapping for the HTTP GET verb at that endpoint.
There is, however, a mapping to that endpoint for the HTTP POST verb. It's the endpoint for the Create operation that we implemented in a previous lesson!
Therefore, Spring reports a 405 METHOD_NOT_ALLOWED error instead of 404 NOT FOUND -- the route was indeed found, but it doesn't support the GET verb.
Implement the GET endpoint in the Controller.

To get past the 405 error, we need to implement the /cashcards endpoint in the Controller using a @GetMapping annotation:

@GetMapping()
private ResponseEntity<Iterable<CashCard>> findAll() {
   return ResponseEntity.ok(cashCardRepository.findAll());
}
Understand the handler method.

Once again we're using one of Spring Data's built-in implementations: CrudRepository.findAll(). Our implementing Repository, CashCardRepository, will automatically return all CashCard records from the database when findAll() is invoked.

Rerun the tests.

Enhance the List Test
As we've done in previous lessons, we've tested that our Cash Card API Controller is "listening" for our HTTP calls and does not crash when invoked, this time for a GET with no further parameters.

Let's enhance our tests and make sure the correct data is returned from our HTTP request.

Enhance the test.

First, let's fill out the test to assert on the expected data values:

 @Test
 void shouldReturnAllCashCardsWhenListIsRequested() {
     ResponseEntity<String> response = restTemplate.getForEntity("/cashcards", String.class);
     assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

     DocumentContext documentContext = JsonPath.parse(response.getBody());
     int cashCardCount = documentContext.read("$.length()");
     assertThat(cashCardCount).isEqualTo(3);

     JSONArray ids = documentContext.read("$..id");
     assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

     JSONArray amounts = documentContext.read("$..amount");
     assertThat(amounts).containsExactlyInAnyOrder(123.45, 100.0, 150.00);
 }
Understand the test.

documentContext.read("$.length()");
...
documentContext.read("$..id");
...
documentContext.read("$..amount");
Check out these new JsonPath expressions!

documentContext.read("$.length()") calculates the length of the array.

.read("$..id") retrieves the list of all id values returned, while .read("$..amount") collects all amounts returned.

To learn more about JsonPath, a good place to start is here in the JsonPath documentation.

assertThat(...).containsExactlyInAnyOrder(...)
We haven't guaranteed the order of the CashCard list -- they come out in whatever order the database chooses to return them. Since we don't specify the order, containsExactlyInAnyOrder(...) asserts that while the list must contain everything we assert, the order does not matter.

Run the tests.

6: Pagination
Let's now implement paging, starting with a test!

We have 3 CashCards in our database. Let's set up a test to fetch them one at a time (page size of 1), then have their amounts sorted from highest to lowest (descending).

Write the pagination test.

Add the following test to CashCardApplicationTests, and note that we are adding parameters to the HTTP request of ?page=0&size=1. We will handle these in our Controller later.

@Test
void shouldReturnAPageOfCashCards() {
    ResponseEntity<String> response = restTemplate.getForEntity("/cashcards?page=0&size=1", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    JSONArray page = documentContext.read("$[*]");
    assertThat(page.size()).isEqualTo(1);
}
Run the tests.

When we run the tests we shouldn't be surprised that all CashCards are returned.

expected: 1
but was: 3
Implement pagination in the CashCardController.

So, let's add our new endpoint to the Controller! Add the following method to the CashCardController (don't delete the existing findAll() method):

@GetMapping
private ResponseEntity<List<CashCard>> findAll(Pageable pageable) {
    Page<CashCard> page = cashCardRepository.findAll(
            PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize()
    ));
    return ResponseEntity.ok(page.getContent());
}
Understand the pagination code.

findAll(Pageable pageable)
Pageable is yet another object that Spring Web provides for us. Since we specified the URI parameters of page=0&size=1, pageable will contain the values we need.

PageRequest.of(
  pageable.getPageNumber(),
  pageable.getPageSize()
));
PageRequest is a basic Java Bean implementation of Pageable. Things that want paging and sorting implementation often support this, such as some types of Spring Data Repositories.

Does our CashCardRepository support Paging and Sorting yet? Let's find out.

Try to compile.

When we run the tests we discover that our code doesn't even compile!

[~/exercises] $ ./gradlew test
...
> Task :compileJava FAILED
exercises/src/main/java/example/cashcard/CashCardController.java:50: error: method findAll in interface CrudRepository<T,ID> cannot be applied to given types;
        Page<CashCard> page = cashCardRepository.findAll(
                                                ^
  required: no arguments
  found:    PageRequest
But of course! We haven't changed the Repository to extend the additional interface. So let's do that. In CashCardRepository.java, also extend PagingAndSortingRepository:

Extend PagingAndSortingRepository and rerun tests.

Update CashCardRepository to also extend PagingAndSortingRepository.

Don't forget to add the new import!

import org.springframework.data.repository.PagingAndSortingRepository;
...

interface CashCardRepository extends CrudRepository<CashCard, Long>, PagingAndSortingRepository<CashCard, Long> { ... }
Now our repository does support Paging and Sorting.

But our tests still fail! Search for the following failure:

[~/exercises] $ ./gradlew test
...
Failed to load ApplicationContext
java.lang.IllegalStateException: Failed to load ApplicationContext
...
Caused by: java.lang.IllegalStateException: Ambiguous mapping. Cannot map 'cashCardController' method
example.cashcard.CashCardController#findAll(Pageable)
to {GET [/cashcards]}: There is already 'cashCardController' bean method
example.cashcard.CashCardController#findAll() mapped.
(The actual output is immensely long. We've included the most helpful error message in the output above.)

Understand and resolve the failure.

So what happened?

We didn't remove the existing findAll() Controller method!

Why is this a problem? Don't we have unique method names and everything compiles?

The problem is that we have two methods mapped to the same endpoint. Spring detects this error at runtime, during the Spring startup process.

So let's remove the offending old findAll() method:

// Delete this one:
@GetMapping()
private ResponseEntity<Iterable<CashCard>> findAll() {
    return ResponseEntity.ok(cashCardRepository.findAll());
}
Run the tests and ensure that they pass.

BUILD SUCCESSFUL in 7s
Next, let's implement Sorting.

7: Sorting
We'd like the Cash Cards to come back in an order that makes sense to humans. So let's order them by amount in a descending order with the highest amounts first.

Write a test (which we expect to fail).

Add the following test to CashCardApplicationTests:

@Test
void shouldReturnASortedPageOfCashCards() {
    ResponseEntity<String> response = restTemplate.getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    JSONArray read = documentContext.read("$[*]");
    assertThat(read.size()).isEqualTo(1);

    double amount = documentContext.read("$[0].amount");
    assertThat(amount).isEqualTo(150.00);
}
Understand the test.

The URI we're requesting contains both pagination and sorting information: /cashcards?page=0&size=1&sort=amount,desc

page=0: Get the first page. Page indexes start at 0.
size=1: Each page has size 1.
sort=amount,desc
The extraction of data (using more JSONPath!) and accompanying assertions expect that the returned Cash Card is the $150.00 one.

Do you think the test will pass? Before running it, try to figure out whether it will or not. If you think it won't pass, where do you think the failure will be?

Run the test.

[~/exercises] $ ./gradlew test
...
org.opentest4j.AssertionFailedError:
 expected: 150.0
  but was: 123.45
The test expected to get the $150.00 Cash Card, but it got the $123.45 one. Why?

The reason is that since we didn't specify a sort order, the cards are returned in the order they are returned from the database. And this happens to be the same as the order in which they were inserted.

An important observation: Not all databases will act the same way. Now, it should make even more sense why we specify a sort order (instead of relying on the database's default order).

Implement sorting in the Controller.

Adding sorting to the Controller code is a super simple single line addition. In the CashCardController class, add an additional parameter to the PageRequest.of() call:

PageRequest.of(
     pageable.getPageNumber(),
     pageable.getPageSize(),
     pageable.getSort()
));
The getSort() method extracts the sort query parameter from the request URI.

Run the tests again. They pass!

CashCardApplicationTests > shouldReturnAllCashCardsWhenListIsRequested() PASSED
Learn by breaking things.

To get a little more confidence in the test, let's do an experiment.

In the test, change the sort order from descending to ascending:

ResponseEntity<String> response = restTemplate.getForEntity("/cashcards?page=0&size=1&sort=amount,asc", String.class);
This should cause the test to fail because the first Cash Card in ascending order should be the $1.00 card. Run the tests and observe the failure:

CashCardApplicationTests > shouldReturnASortedPageOfCashCards() FAILED
org.opentest4j.AssertionFailedError:
 expected: 150.0
  but was: 1.0
Correct! This result reinforces our confidence in the test. Instead of writing a whole new test, we used an existing one to run a little experiment.

Now let's change the test back to request descending sort order so that it passes again.

8: Paging and Sorting defaults
We now have an endpoint which requires the client to send four pieces of information: The page index and size, the sort order, and direction. This is a lot to ask, so let's make it easier on them.

Write a new test which doesn't send any pagination or sorting parameters.

We'll add a new test that expects reasonable defaults for the parameters.

The defaults will be:

Sort by amount ascending.
A page size of something larger than 3, so that all of our fixtures will be returned.
@Test
void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
    ResponseEntity<String> response = restTemplate.getForEntity("/cashcards", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    JSONArray page = documentContext.read("$[*]");
    assertThat(page.size()).isEqualTo(3);

    JSONArray amounts = documentContext.read("$..amount");
    assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
}
Run the tests.

[~/exercises] $ ./gradlew test
...
Actual and expected have the same elements but not in the same order, at index 0 actual element was:
  123.45
whereas expected element was:
  1.0
The test failure shows:

All the Cash Cards are being returned, since the (page.size()).isEqualTo(3) assertion succeeded.
BUT: They are not sorted since the (amounts).containsExactly(1.00, 123.45, 150.00) assertion fails:
Make the test pass.

Change the implementation by adding a single line to the Controller method:

...
PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
));
...
Understand the implementation.

So, what just happened?

The answer is that the getSortOr() method provides default values for the page, size, and sort parameters. The default values come from two different sources:

Spring provides the default page and size values (they are 0 and 20, respectively). A default of 20 for page size explains why all three of our Cash Cards were returned. Again: we didn't need to explicitly define these defaults. Spring provides them "out of the box".

We defined the default sort parameter in our own code, by passing a Sort object to getSortOr():

Sort.by(Sort.Direction.ASC, "amount")
The net result is that if any of the three required parameters are not passed to the application, then reasonable defaults will be provided.

Run the tests... again!

Congratulations!

Everything's passing now.

[~/exercises] $ ./gradlew test
...
BUILD SUCCESSFUL in 7s

1: Understand our Security Requirements
Who should be allowed to manage any given Cash Card?

In our simple domain, let's state that the user who created the Cash Card "owns" the Cash Card. Thus, they are the "card owner". Only the card owner can view or update a Cash Card.

The logic will be something like this:

IF the user is authenticated

... AND they are authorized as a "card owner"

... ... AND they own the requested Cash Card

THEN complete the users's request

BUT don't allow users to access Cash Cards they do not own.
