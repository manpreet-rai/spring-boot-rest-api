<img width="555" alt="image" src="https://github.com/user-attachments/assets/921ceeea-04eb-41fa-b758-21750ae8c3ee">

# REST API with Spring Boot

Use spring initializr to with following settings for now.

<img width="555" alt="image" src="https://github.com/user-attachments/assets/344f4272-063f-4601-a98e-523302a4971c">

Add Spring Web as dependency

<img width="555" alt="image" src="https://github.com/user-attachments/assets/d5d19114-9d31-43c6-8cca-f3c52e39864b">

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

Although we've helped Spring Data create a test database by un-commenting `schema.sql`, it's still an **empty database**.

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

<img width="1131" alt="image" src="https://github.com/user-attachments/assets/1997973d-dd4e-409c-9de9-964addad867f">
