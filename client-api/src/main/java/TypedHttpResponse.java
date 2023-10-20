import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.function.Consumer;


public interface TypedHttpResponse<T, E extends RuntimeException> {
    int statusCode();
    HttpHeaders headers();
    Either<T, E> body();
    URI uri();
    HttpRequest request();

    default boolean isSuccess() {
        return body().error == null;
    }


    class Either<T, E extends RuntimeException> {
        private final T value;
        private final E error;

        public static <T, E extends RuntimeException> Either<T, E> ok(T value) {
            return new Either<>(value, null);
        }

        public static <T, E extends RuntimeException> Either<T, E> fail(E error) {
            return new Either<>(null, error);
        }

        private Either(T value, E error) {
            this.value = value;
            this.error = error;
        }

        public Optional<T> result(Consumer<E> errorHandler) {
            if (error != null) {
                errorHandler.accept(error);
            }
            return Optional.ofNullable(value);
        }

        public Optional<E> error() {
            return Optional.ofNullable(error);
        }

        public T resultOrThrow() throws E {
            if (error != null) {
                throw error;
            }
            return value;
        }
    }
}
