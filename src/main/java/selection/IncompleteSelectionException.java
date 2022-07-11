package selection;

/**
 * Threw when trying to do something that involves a selection, but the player has not selected a region yet.
 */
public class IncompleteSelectionException extends Exception {
    public IncompleteSelectionException() {
        super();
    }

    public IncompleteSelectionException(String message) {
        super(message);
    }

    public IncompleteSelectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompleteSelectionException(Throwable cause) {
        super(cause);
    }
}
