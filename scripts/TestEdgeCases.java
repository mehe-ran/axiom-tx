// posts known-bad payloads and expects 422 for each: java scripts/TestEdgeCases.java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class TestEdgeCases {

    private static final URI TRANSFER_URL = URI.create("http://127.0.0.1:8000/transfer");

    record Case(String name, String body) {
    }

    public static void main(String[] args) throws Exception {
        List<Case> cases = List.of(
                new Case("same account", "{\"sender_id\": \"acc_01\", \"receiver_id\": \"acc_01\", \"amount\": 500.00}"),
                new Case("negative amount", "{\"sender_id\": \"acc_02\", \"receiver_id\": \"acc_03\", \"amount\": -150.00}"),
                new Case("zero amount", "{\"sender_id\": \"acc_04\", \"receiver_id\": \"acc_05\", \"amount\": 0.00}"));

        HttpClient client = HttpClient.newHttpClient();
        System.out.println("initiating edge case validation tests...\n");
        int failures = 0;
        for (Case c : cases) {
            HttpRequest request = HttpRequest.newBuilder(TRANSFER_URL)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(c.body()))
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                boolean rejected = response.statusCode() == 422;
                if (!rejected) {
                    failures++;
                }
                System.out.printf("%s %s -> HTTP %d | %s%n",
                        rejected ? "PASS" : "FAIL", c.name(), response.statusCode(), response.body());
            } catch (Exception e) {
                System.out.println("connection failed. is the gateway running? error: " + e);
                System.exit(2);
            }
        }
        System.exit(failures == 0 ? 0 : 1);
    }
}
