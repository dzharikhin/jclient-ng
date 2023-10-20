import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

// часть примера BadCaseExample
public class PretrainedFeatureClient {

    private WireMockServer wireMock;

    public <E extends RuntimeException> TypedHttpCall<Map<String, Object>, E> getVacancyPretrainedFeatures(
            List<String> featuresNames,
            String vacancyJson,
            Function<HttpResponse, E> errorBodyHandler
    ) {
        Function<HttpResponse, Map<String, Object>> successBodyHandler = null;
        return new TypedHttpCall<>(
                new HttpCall("GET", wireMock.url("getVacancyPretrainedFeatures" + featuresNames + vacancyJson)),
                TypedHttpCall.OK_STATUS_SUCCESS_CONDITION,
                successBodyHandler,
                errorBodyHandler
        );
    }
}
