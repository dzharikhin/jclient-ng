import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;

public class TypedHttpCall<T, E extends RuntimeException> {

    public static Predicate<HttpResponse> OK_STATUS_SUCCESS_CONDITION = response -> 200 == response.statusCode();
    private final HttpCall httpCall;
    private final Predicate<HttpResponse> successCondition;
    private final Function<HttpResponse, T> successBodyHandler;
    private final Function<HttpResponse, E> errorBodyHandler;

    public TypedHttpCall(HttpCall httpCall, Predicate<HttpResponse> successCondition, Function<HttpResponse, T> successBodyHandler, Function<HttpResponse, E> errorBodyHandler) {
        this.httpCall = httpCall;
        this.successCondition = successCondition;
        this.successBodyHandler = successBodyHandler;
        this.errorBodyHandler = errorBodyHandler;
    }

    public TypedHttpCall<T, E> withBody(byte[] body, String bodyContentType) {
        httpCall.withBody(body, bodyContentType);
        return this;
    }

    public TypedHttpCall<T, E> withAdditionalHeaders() {
        return this;
    }

    public TypedHttpCall<T, E> withAdditionalQuery() {
        return this;
    }

    public TypedHttpCall<T, E> withCustomTimeout() {
        return this;
    }

    public Instant getTimeoutEnd() {
        return httpCall.getTimeoutEnd();
    }

    public final Callable<TypedHttpResponse<T, E>> asCallable(HttpClient client) {
        return () -> execute(client);
    }

    public final TypedHttpResponse<T, E> execute(HttpClient client) throws E, IOException {
        return HttpClientUtil.convertResponse(
                httpCall.execute(client),
                successCondition,
                successBodyHandler,
                errorBodyHandler
        );
    }

}
