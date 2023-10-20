import com.github.tomakehurst.wiremock.WireMockServer;

import java.net.URI;
import java.net.http.HttpRequest;

//пример где мы совсем вываливаем специфику наружу
public class TestRequestEndpoints {

    private final WireMockServer wireMock;

    public TestRequestEndpoints(WireMockServer wireMock) {
        this.wireMock = wireMock;
    }

    public HttpRequest.Builder getFoo() {
        return HttpRequest.newBuilder().GET().uri(URI.create(wireMock.url("foo")));
    }
}
