import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.function.Function;

// okhttp-like вариант
public class TestBuilderEndpoints {

    private final WireMockServer wireMock;

    public TestBuilderEndpoints(WireMockServer wireMock) {
        this.wireMock = wireMock;
    }

    public HttpCall getFooRaw() {
        return new HttpCall("GET", wireMock.url("foo"));
    }

    public TypedHttpCall<ExampleSuccessEntity, ExampleWebException> getFoo() {
        Function<HttpResponse, ExampleSuccessEntity> successBodyHandler = null;
        Function<HttpResponse, ExampleWebException> errorBodyHandler = null;
        return new TypedHttpCall<>(getFooRaw(), TypedHttpCall.OK_STATUS_SUCCESS_CONDITION, successBodyHandler, errorBodyHandler);
    }

    //форсить наличие последнего обработчика можно каким-нить плагином, чтобы соблюдался контракт
    public <E extends RuntimeException> TypedHttpCall<ExampleSuccessEntity, E> getFoo(
            Function<HttpResponse, E> errorBodyHandler
    ) {
        Function<HttpResponse, ExampleSuccessEntity> successBodyHandler = null;
        return new TypedHttpCall<>(getFooRaw(), TypedHttpCall.OK_STATUS_SUCCESS_CONDITION, successBodyHandler, errorBodyHandler);
    }










    public HttpCall getBarRaw() {
        return new HttpCall("GET", wireMock.url("bar"));
    }

    public <E extends RuntimeException> TypedHttpCallExampleWithSealedException<ExampleSuccessEntity, BarException, E> getBarExampleWithSealedException(
            Function<BarException, E> errorBodyHandler
    ) {
        Function<HttpResponse, ExampleSuccessEntity> successBodyHandler = null;
        return new TypedHttpCallExampleWithSealedException<>(getBarRaw(), TypedHttpCall.OK_STATUS_SUCCESS_CONDITION, successBodyHandler, errorBodyHandler, rawResponse -> switch (rawResponse.statusCode()) {
            case 400 -> new BarException.BarBadRequestException();
            case 404 -> new BarException.BarNotFoundException();
            default -> new BarException();
        });
    }

    //писать под каждый метод такие классы
    public sealed static class BarException extends RuntimeException {
        public static final class BarBadRequestException extends BarException {}
        public static final class BarNotFoundException extends BarException {}
    }


}
