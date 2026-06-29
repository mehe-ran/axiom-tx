import requests
import random
import time

GATEWAY_URL = "http://localhost:8000/transfer"


def blast_network(num_tx=50):
    print(f"Initiating load test with {num_tx} transactions...")
    success = 0
    for _ in range(num_tx):
        payload = {
            "sender_id": f"acc_{random.randint(1000, 9999)}",
            "receiver_id": f"acc_{random.randint(1000, 9999)}",
            "amount": round(random.uniform(10.0, 5000.0), 2),
            "currency": "USD"
        }
        try:
            res = requests.post(GATEWAY_URL, json=payload)
            if res.status_code == 200:
                success += 1
        except requests.exceptions.ConnectionError:
            print("Gateway unreachable.")
            break

        # slight delay to simulate natural traffic
        time.sleep(0.05)

    print(f"Load test complete. {success}/{num_tx} successfully ingested.")


if __name__ == "__main__":
    blast_network()