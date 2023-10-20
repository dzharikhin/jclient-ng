import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

public class Examples {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Throwable {
        checkIfScopeCanBeJoinedManyTimes();

        ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
        HttpClient httpClient = HttpClient.newBuilder().build();
        WireMockServer wireMock = new WireMockServer();
        TestEmbeddedJClient testEmbeddedJClient = new TestEmbeddedJClient(Executors.newVirtualThreadPerTaskExecutor(), httpClient, wireMock);
        TestRequestEndpoints testRequestEndpoints = new TestRequestEndpoints(wireMock);
        TestBuilderEndpoints testBuilderEndpoints = new TestBuilderEndpoints(wireMock);

        ScopedValue.where(REQUEST_ID, "123").run(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) { //как будто этого скоупа хватает в большинстве случаев, но можно и свой
////////////////вариант 1 - все в себе/////////////////////////////////////////////
                TypedHttpCall<ExampleSuccessEntity, ExampleWebException> fooCall = testBuilderEndpoints.getFoo().withAdditionalHeaders();
                var fooCallTask = scope.fork(fooCall.asCallable(httpClient));

                scope
                        .joinUntil(fooCall.getTimeoutEnd())
                        .throwIfFailed(RuntimeException::new);

                TypedHttpResponse<ExampleSuccessEntity, ExampleWebException> fooResponse = fooCallTask.get();
                if (fooResponse.isSuccess() || fooResponse.statusCode() == 204 || new ExampleSuccessEntity("foo").equals(fooResponse.body().result(e -> {}).orElse(null))) {
                    scope.fork(testBuilderEndpoints.getFoo().withAdditionalQuery().asCallable(httpClient));
                    scope
                            .joinUntil(fooCall.getTimeoutEnd())
                            .throwIfFailed(RuntimeException::new);
                }
////////////////вариант 2 - маппинг наружу/////////////////////////////////////////////
                TypedHttpCall<ExampleSuccessEntity, ExampleWebException> alternativeFooCall = testBuilderEndpoints.getFoo(
                        Examples::getError
                )
                        .withAdditionalHeaders();
                var alternativeFooCallTask = scope.fork(alternativeFooCall.asCallable(httpClient));

                scope
                        .joinUntil(alternativeFooCall.getTimeoutEnd())
                        .throwIfFailed(RuntimeException::new);

                TypedHttpResponse<ExampleSuccessEntity, ExampleWebException> alternativeFooResponse = alternativeFooCallTask.get();
                if (alternativeFooResponse.isSuccess() || alternativeFooResponse.statusCode() == 204 || new ExampleSuccessEntity("foo").equals(alternativeFooResponse.body().result(e -> {}).orElse(null))) {
                    scope.fork(testBuilderEndpoints.getFoo().withAdditionalQuery().asCallable(httpClient));
                    scope
                            .joinUntil(alternativeFooCall.getTimeoutEnd())
                            .throwIfFailed(RuntimeException::new);
                }
////////////////вариант 3 - сырой поход с инкапсуляцией виртуального треда и конвертацией в прикладном коде/////////////////////////////////////////////
                HttpCall fooLowLevelCall = testBuilderEndpoints.getFooRaw().withCustomTimeout();

                var fooLowLevelResponse = fooLowLevelCall.execute(httpClient, scope);
                if (fooLowLevelResponse.statusCode() == 200 && TypedHttpCall.OK_STATUS_SUCCESS_CONDITION.test(fooLowLevelResponse)) {
                    var resultTask = scope.fork(() -> getEntity(fooLowLevelResponse)); //не io-bound, поэтому нет смысла форкать
                    scope.throwIfFailed(RuntimeException::new);

                    ExampleSuccessEntity result = resultTask.get();

                    if (new ExampleSuccessEntity("foo").equals(result)) {
                        scope.fork(testBuilderEndpoints.getFoo().withAdditionalQuery().asCallable(httpClient));
                        scope
                                .joinUntil(fooLowLevelCall.getTimeoutEnd())
                                .throwIfFailed(RuntimeException::new);
                    }
                } else {
                    switch (fooLowLevelResponse.headers().firstValue("Content-Type").orElse("application/text")) {
                        case "application/text" -> {
                            String result = getErrorMsg(fooLowLevelResponse.body());
                            System.out.println(result);
                        }
                        default -> getErrorMsg(fooLowLevelResponse.body());
                    }
                }
////////////////вариант 4 - запечатанные ошибки/////////////////////////////////////////////
                var barCall = testBuilderEndpoints.getBarExampleWithSealedException(barException -> {
                    switch (barException) {
                        case TestBuilderEndpoints.BarException.BarBadRequestException barBadRequestException -> {
                            return new MyWebAppException.DetailedMyWebAppException("bad request");
                        }
                        case TestBuilderEndpoints.BarException.BarNotFoundException barNotFoundException -> {
                            return new MyWebAppException.DetailedMyWebAppException("bar not found");
                        }
                        case TestBuilderEndpoints.BarException defaultBarException -> {
                            return new MyWebAppException("internal server error");
                        }
                    }
                });
                var barCallTask = scope.fork(barCall.asCallable(httpClient));
                scope
                        .joinUntil(barCall.getTimeoutEnd())
                        .throwIfFailed(RuntimeException::new);

                //получаем ответ типизированный нашим имсключемнием
                TypedHttpResponseExampleWithSealedException<ExampleSuccessEntity, MyWebAppException> bar = barCallTask.get();

            } catch (InterruptedException | TimeoutException e) {
                if (e instanceof TimeoutException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static ExampleWebException getError(HttpResponse response) {
        try (var is = response.body()) {
            return new ExampleWebException(new String(is.readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getErrorMsg(InputStream inputStream) {
        try (var is = inputStream) {
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private static ExampleSuccessEntity getEntity(HttpResponse response) {
        try (var is = response.body()) {
            return mapper.readValue(is, ExampleSuccessEntity.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void checkIfScopeCanBeJoinedManyTimes() throws Throwable {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) { //ShutdownOnSuccess бесполезен для последовательных запросов, можно написать свой скоуп
            var task1 = scope.fork(() -> { Thread.sleep(Duration.ofSeconds(1)); return 1; });
            var task2 = scope.fork(() -> { Thread.sleep(Duration.ofSeconds(2)); return 2; });
            scope.joinUntil(Instant.now().plusMillis(2050));
            if (task1.get() == 1) {
                StructuredTaskScope.Subtask<Integer> task3 = scope.fork(() -> { Thread.sleep(Duration.ofSeconds(3)); throw new Exception("foo"); });
                scope.joinUntil(Instant.now().plusSeconds(4));
                if (task3.exception() != null) {
                    throw task3.exception();
                }
                System.out.println(task3.get());
            }
        }
    }
}
