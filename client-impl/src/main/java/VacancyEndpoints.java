import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.function.Function;

// часть примера BadCaseExample
public class VacancyEndpoints {

    private WireMockServer wireMock;

    public <E extends RuntimeException> TypedHttpCall<String, E> getFullJson(
            int vacancyId,
            Function<HttpResponse, E> errorBodyHandler
    ) {
        Function<HttpResponse, String> successBodyHandler = null;
        return new TypedHttpCall<>(new HttpCall("GET", wireMock.url("getFullJson" + vacancyId)), TypedHttpCall.OK_STATUS_SUCCESS_CONDITION, successBodyHandler, errorBodyHandler);
    }
}
