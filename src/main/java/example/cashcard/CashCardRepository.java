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