import requests

BASE_URL = "http://127.0.0.1:8000"

bad_payloads = [
    {"sender_id": "acc_01", "receiver_id": "acc_01", "amount": 500.00}, # same account
    {"sender_id": "acc_02", "receiver_id": "acc_03", "amount": -150.00}, # negative amount
    {"sender_id": "acc_04", "receiver_id": "acc_05", "amount": 0.00} # zero amount
]

print("Initiating Edge Case Validation Tests...\n")
for payload in bad_payloads:
    try:
        resp = requests.post(f"{BASE_URL}/transfer", json=payload)
        print(f"Payload Amount: {payload['amount']} -> HTTP {resp.status_code} | {resp.text}")
    except Exception as e:
        print(f"Connection failed. Is the Gateway running? Error: {e}")