// load generator: java scripts/SeedData.java [count]
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class SeedData {

    private static final URI GATEWAY_URL = URI.create("http://localhost:8000/transfer");

    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        HttpClient client = HttpClient.newHttpClient();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        System.out.printf("initiating load test with %d transactions...%n", count);
        int success = 0;
        for (int i = 0; i < count; i++) {
            int sender = random.nextInt(1000, 10000);
            int receiver = random.nextInt(1000, 10000);
            while (receiver == sender) {
                receiver = random.nextInt(1000, 10000);
            }
            double amount = random.nextDouble(10.0, 5000.0);
            String body = String.format(Locale.ROOT,
                    "{\"sender_id\": \"acc_%d\", \"receiver_id\": \"acc_%d\", \"amount\": %.2f, \"currency\": \"USD\"}",
                    sender, receiver, amount);

            HttpRequest request = HttpRequest.newBuilder(GATEWAY_URL)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    success++;
                } else {
                    System.out.printf("HTTP %d | %s%n", response.statusCode(), response.body());
                }
            } catch (ConnectException e) {
                System.out.println("gateway unreachable.");
                break;
            } catch (IOException e) {
                System.out.println("request failed: " + e.getMessage());
            }

            // slight delay to simulate natural traffic
            Thread.sleep(50);
        }
        System.out.printf("load test complete. %d/%d successfully ingested.%n", success, count);
    }
}
