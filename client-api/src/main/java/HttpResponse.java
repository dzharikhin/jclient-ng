import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;

//плюс-минус скопипащено с java.net.http.HttpResponse
public interface HttpResponse {
    int statusCode();

    HttpHeaders headers();

    InputStream body();

    URI uri();

    HttpRequest request();
}
