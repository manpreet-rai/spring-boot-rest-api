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
