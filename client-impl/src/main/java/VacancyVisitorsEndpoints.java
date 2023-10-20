import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.function.Function;

// часть примера BadCaseExample
public class VacancyVisitorsEndpoints {

    private WireMockServer wireMock;

    public <E extends RuntimeException> TypedHttpCall<VacancyVisitorsCountDto, E> getVacancyVisitorsCount(
            int vacancyId,
            Function<HttpResponse, E> errorBodyHandler
    ) {
        Function<HttpResponse, VacancyVisitorsCountDto> successBodyHandler = null;
        return new TypedHttpCall<>(new HttpCall("GET", wireMock.url("getVacancyVisitorsCount" + vacancyId)), TypedHttpCall.OK_STATUS_SUCCESS_CONDITION, successBodyHandler, errorBodyHandler);
    }

    record VacancyVisitorsCountDto(int vacancyVisitorsCount){};
}
