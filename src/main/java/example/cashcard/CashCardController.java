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
