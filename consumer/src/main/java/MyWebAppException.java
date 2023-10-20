public class MyWebAppException extends RuntimeException {
    public MyWebAppException(String message) {
        super(message);
    }

    public static class DetailedMyWebAppException extends MyWebAppException {

        public DetailedMyWebAppException(String message) {
            super(message);
        }
    }
}
