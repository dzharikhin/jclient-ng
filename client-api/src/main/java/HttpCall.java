import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

public class HttpCall {

    private final String method;
    private final String url;
    final Multimap<String, String> headers;
    private byte[] body = null;
    private String bodyContentType = null;

    public HttpCall(String url, String method) {
        this.url = url;
        this.method = method;
        this.headers = Multimaps.newMultimap(Map.of(), HashSet::new);
    }

    public HttpCall(String url, String method, Multimap<String, String> headers) {
        this(url, method);
        this.headers.putAll(headers);
    }

    public HttpCall withBody(byte[] body, String bodyContentType) {
        return this;
    }

    public HttpCall withAdditionalHeaders() {
        return this;
    }

    public HttpCall withAdditionalQuery() {
        return this;
    }

    public HttpCall withCustomTimeout() {
        return this;
    }

    public Instant getTimeoutEnd() {
        return null;
    }

    public final Callable<HttpResponse> asCallable(HttpClient client) {
        return () -> execute(client);
    }

    //можно стрим сделать лучше, чем InputStream?
    public final HttpResponse execute(HttpClient client) throws IOException {
        try {
            //код вызова максимально упрощается
            //технически еще надо сделать ветвление для пустого ответа, но это можно просто решить флагом emptyResponseBody
            return HttpClientUtil.convertToRawResponse(client.send(HttpClientUtil.getRequestBuilderForMethod(method).uri(URI.create(url)).build(), BodyHandlers.ofInputStream()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    //альтернативный метод: можно еще лучше отделить ту часть, которая про i/o, но за это платим протеканием абстракции
    public final HttpResponse execute(HttpClient client, StructuredTaskScope<Object> scope) throws IOException {
        try {
            var task = scope.fork(() -> client.send(HttpClientUtil.getRequestBuilderForMethod(method).uri(URI.create(url)).build(), BodyHandlers.ofInputStream()));
            scope.joinUntil(getTimeoutEnd());
            if (task.exception() != null) {
                if (task.exception() instanceof IOException ioe) {
                    throw ioe;
                }
                throw new IOException(task.exception());
            }
            java.net.http.HttpResponse<InputStream> response = task.get();
            return HttpClientUtil.convertToRawResponse(response);
        } catch (InterruptedException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IOException(e);
        }
    }


    //если клиент умеет в асинк, то скоуп не нужен и даже вреден
    public final CompletableFuture<HttpResponse> executeAsync(HttpClient client) {
        return client.sendAsync(HttpClientUtil.getRequestBuilderForMethod(method).uri(URI.create(url)).build(), BodyHandlers.ofInputStream())
                .thenApply(HttpClientUtil::convertToRawResponse);
    }
}
