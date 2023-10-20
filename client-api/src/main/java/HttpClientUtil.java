import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.util.function.Function;
import java.util.function.Predicate;

//httpClient должен уметь возвращать RawHttpResponse, но я не хочу еще и клиент щас мокать
public class HttpClientUtil {

    public static HttpRequest.Builder getRequestBuilderForMethod(String method) {
        return switch (method) {
            case "GET" -> HttpRequest.newBuilder().GET();
            default -> throw new IllegalStateException("Unexpected value: " + method);
        };
    }

    //httpClient должен уметь возвращать RawHttpResponse, но я не хочу еще и клиент щас мокать
    public static HttpResponse convertToRawResponse(
            java.net.http.HttpResponse<InputStream> specificResponse
    ) {
        return new HttpResponse() {
            @Override
            public int statusCode() {
                return specificResponse.statusCode();
            }

            @Override
            public HttpHeaders headers() {
                return specificResponse.headers();
            }

            @Override
            public InputStream body() {
                return specificResponse.body();
            }

            @Override
            public URI uri() {
                return specificResponse.uri();
            }

            @Override
            public HttpRequest request() {
                return specificResponse.request();
            }
        };
    }

    public static <T, E extends RuntimeException> TypedHttpResponse<T, E> convertResponse(
            HttpResponse rawResponse,
            Predicate<HttpResponse> successCondition,
            Function<HttpResponse, T> successBodyHandler,
            Function<HttpResponse, E> errorBodyHandler
    ) {
        return new TypedHttpResponse<>() {
            @Override
            public int statusCode() {
                return rawResponse.statusCode();
            }

            @Override
            public HttpHeaders headers() {
                return rawResponse.headers();
            }

            @Override
            public Either<T, E> body() {
                if (successCondition.test(rawResponse)) {
                    return Either.ok(successBodyHandler.apply(rawResponse));
                } else {
                    return Either.fail(errorBodyHandler.apply(rawResponse));
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
