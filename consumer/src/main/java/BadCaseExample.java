import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

//    private CompletableFuture<Prompt> getSearchPrompt(Integer vacancyId, TopicCountsByState topicCountsByState) {
//        SearchPromptParams searchPromptParams = new SearchPromptParams();
//        searchPromptParams.viewThreshold = settingsClient.getInteger("prompt.view.threshold").orElseThrow();
//        searchPromptParams.totalTopicsCount = getTotalTopicsCount(topicCountsByState);
//        searchPromptParams.unreadResponsesCount = getUnreadTopicsCount(topicCountsByState);
//
//        var usePredictor = settingsClient.getBoolean("prompt.use.predictor", false);
//
//        return vacancyVisitorsJClient.getVacancyVisitorsCount(vacancyId)
//                .thenApply(rws -> check(rws, "Failed to get vacancy visitors ids").returnDefault(VacancyVisitorsCountDto::new).onAnyError())
//                .thenApply(VacancyVisitorsCountDto::getVacancyVisitorsCount)
//                .thenAccept(viewCount -> searchPromptParams.viewCount = viewCount)
//                .thenCompose(v -> {
//                    if (usePredictor && searchPromptParams.viewCount < searchPromptParams.viewThreshold) {
//                        return vacancyClient.getFullJson(vacancyId)
//                                .thenApply(rws -> check(rws, "Failed to get vacancy full json").proxyStatusCode().onAnyError())
//                                .thenCompose(json -> pretrainedFeatureClient.getVacancyPretrainedFeatures(List.of(LPV_MODEL_NAME), json))
//                                .thenApply(rws -> check(rws, "Failed to get responses predictor").proxyStatusCode().onAnyError())
//                                .thenApply(pretrainedFeaturesMap -> (Double) pretrainedFeaturesMap.get(LPV_MODEL_NAME))
//                                .thenApply(lpvProbability -> lpvProbability > LPV_THRESHOLD ? "1" : "0");
//                    } else {
//                        return CompletableFuture.completedFuture("0");
//                    }
//                })
//                .thenAccept(predictResponse -> searchPromptParams.predictResponse = predictResponse)
//                .thenApply(v -> getSearchPromptFromViewCount(searchPromptParams));
//    }
public class BadCaseExample {
    private static final double LPV_THRESHOLD = 0.35;
    private static final String LPV_MODEL_NAME = "vac.lpv_pred";

    public static void main(String[] args) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        VacancyVisitorsEndpoints vacancyVisitorsEndpoints = new VacancyVisitorsEndpoints();
        VacancyEndpoints vacancyEndpoints = new VacancyEndpoints();
        PretrainedFeatureClient pretrainedFeatureClient = new PretrainedFeatureClient();


        SearchPromptParams searchPromptParams = new SearchPromptParams();
        int vacancyId = 1;

        try (var scope = new WaitingAllTypedCallsTaskScope()) {
            var vacancyVisitorsCountCall = vacancyVisitorsEndpoints.getVacancyVisitorsCount(vacancyId, response -> new RuntimeException());
            var vacancyVisitorsCountDto = getSingleResponse(scope, vacancyVisitorsCountCall, httpClient, getDefaultValueOnError(() -> new VacancyVisitorsEndpoints.VacancyVisitorsCountDto(0), e -> {}));
            if (vacancyVisitorsCountDto.vacancyVisitorsCount() < searchPromptParams.viewThreshold) {
                var vacancyJsonCall = vacancyEndpoints.getFullJson(vacancyId, response -> new RuntimeException(String.valueOf(response.statusCode()))); //ну типа пропагейтим код
                String vacancyJson = getSingleResponse(scope, vacancyJsonCall, httpClient, TypedHttpResponse.Either::resultOrThrow);
                var pretrainedFeaturesMapCall = pretrainedFeatureClient.getVacancyPretrainedFeatures(List.of(LPV_MODEL_NAME), vacancyJson, response -> new RuntimeException(String.valueOf(response.statusCode()))); //ну типа пропагейтим код
                var pretrainedFeaturesMap = getSingleResponse(scope, pretrainedFeaturesMapCall, httpClient, TypedHttpResponse.Either::resultOrThrow);
                searchPromptParams.predictResponse = (Double) pretrainedFeaturesMap.get(LPV_MODEL_NAME) > LPV_THRESHOLD ? "1" : "0";
            } else {
                searchPromptParams.predictResponse = "0";
            }
        } catch (InterruptedException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
        //convert params to prompt
    }

    private static <T>ThrowingResultMapper<T, ?> getDefaultValueOnError(Supplier<T> defaultValue, Consumer<RuntimeException> errorConsumer) {
        return either -> either
                .result(errorConsumer)
                .orElseGet(defaultValue);
    }

    private static <T, E extends RuntimeException> T getSingleResponse(
            WaitingAllTypedCallsTaskScope scope,
            TypedHttpCall<T, E> vacancyVisitorsCountCall,
            HttpClient httpClient,
            ThrowingResultMapper<T, E> bodyHandler
    ) throws InterruptedException, TimeoutException, E {
        var vacancyVisitorsCountTask = scope.fork(vacancyVisitorsCountCall.asCallable(httpClient));
        scope.joinUntil(vacancyVisitorsCountCall.getTimeoutEnd());
        return bodyHandler.apply(vacancyVisitorsCountTask.get().body());
    }

    @FunctionalInterface
    interface ThrowingResultMapper<T, E extends RuntimeException> {
        T apply(TypedHttpResponse.Either<T, E> either) throws E;
    }

    private static class SearchPromptParams {
        //        private int viewCount;
        private final int viewThreshold = 2;
        public String predictResponse;
    }
}
