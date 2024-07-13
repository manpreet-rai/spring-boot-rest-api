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
