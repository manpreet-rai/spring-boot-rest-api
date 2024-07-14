package example.cashcard;

// Add this import
import org.springframework.data.annotation.Id;

/**
 * id field is marked with @Id annotation
 * This let Spring know that id is primary key, to be used by CrudRepository.
 */
record CashCard(@Id Long id, Double amount) {
}
