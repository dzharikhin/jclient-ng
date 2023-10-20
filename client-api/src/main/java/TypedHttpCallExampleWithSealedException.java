import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;

//то же, что и TypedHttpCall с поправками на вариант с sealed исключениями
public class TypedHttpCallExampleWithSealedException<T, EI extends RuntimeException, EO extends RuntimeException> {
    private final HttpCall httpCall;
    private final Predicate<HttpResponse> successCondition;
    private final Function<HttpResponse, T> successBodyHandler;
    private final Function<HttpResponse, EO> errorBodyHandler;

    public TypedHttpCallExampleWithSealedException(HttpCall httpCall, Predicate<HttpResponse> successCondition, Function<HttpResponse, T> successBodyHandler, Function<EI, EO> errorBodyHandler, Function<HttpResponse, EI> errorBodyMapper) {
        this.httpCall = httpCall;
        this.successCondition = successCondition;
        this.successBodyHandler = successBodyHandler;
        this.errorBodyHandler = errorBodyHandler.compose(errorBodyMapper);
    }

    public TypedHttpCallExampleWithSealedException<T, EI, EO> withBody(byte[] body, String bodyContentType) {
        httpCall.withBody(body, bodyContentType);
        return this;
    }

    public Instant getTimeoutEnd() {
        return httpCall.getTimeoutEnd();
    }

    public final Callable<TypedHttpResponseExampleWithSealedException<T, EO>> asCallable(HttpClient client) {
        return () -> execute(client);
    }

    public final TypedHttpResponseExampleWithSealedException<T, EO> execute(HttpClient client) throws Exception {
        HttpResponse rawResponse = httpCall.execute(client);
        return new TypedHttpResponseExampleWithSealedException<>() {

            @Override
            public int statusCode() {
                return rawResponse.statusCode();
            }

            @Override
            public HttpHeaders headers() {
                return rawResponse.headers();
            }

            @Override
            public Either<T, EO> body() {
                if (successCondition.test(rawResponse)) {
                    return TypedHttpResponseExampleWithSealedException.Either.ok(successBodyHandler.apply(rawResponse));
                } else {
                    return TypedHttpResponseExampleWithSealedException.Either.fail(errorBodyHandler.apply(rawResponse));
                }
            }

            @Override
            public URI uri() {
                return rawResponse.uri();
            }

            @Override
            public HttpRequest request() {
                return rawResponse.request();
            }
        };
    }

}
