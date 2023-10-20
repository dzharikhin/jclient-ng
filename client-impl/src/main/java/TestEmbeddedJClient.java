import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

//ну типа как жклиент
public class TestEmbeddedJClient {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService virtualThreadFactory;
    private final HttpClient httpClient;
    private final WireMockServer wireMock;

    public TestEmbeddedJClient(ExecutorService virtualThreadFactory, HttpClient httpClient, WireMockServer wireMock) {
        this.virtualThreadFactory = virtualThreadFactory;
        this.httpClient = httpClient;
        this.wireMock = wireMock;
    }

    public TypedHttpResponse<ExampleSuccessEntity, ExampleWebException> getFoo() {
        return HttpClientUtil.convertResponse(
                HttpClientUtil.convertToRawResponse(
                        handleFuture(
                                virtualThreadFactory.submit(() -> handleHttpCall(() -> httpClient.send(
                                        HttpRequest.newBuilder().GET().uri(URI.create(wireMock.url("foo"))).build(),
                                        BodyHandlers.ofInputStream()
                                )))
                        )
                ),
                TypedHttpCall.OK_STATUS_SUCCESS_CONDITION,
                response -> getEntity(response, ExampleSuccessEntity.class),
                response -> getEntity(response, ExampleWebException.class)
        );
    }

    private static <T> T handleHttpCall(ThrowableSupplier<T> call) {
        try {
            return call.get();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
    }

    private static <T> T getEntity(HttpResponse response, Class<T> cls) {
        try (var is = response.body()){
            return mapper.readValue(is, cls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T handleFuture(Future<T> future) {
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
    }

    public interface ThrowableSupplier<T> {
        T get() throws IOException, InterruptedException;
    }
}
