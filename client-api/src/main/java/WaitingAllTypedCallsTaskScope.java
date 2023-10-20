import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;

public class WaitingAllTypedCallsTaskScope extends StructuredTaskScope<Object> {
    @Override
    protected void handleComplete(Subtask<?> subtask) {
    }
}
