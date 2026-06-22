# extract currency code safely from the payload dictionary
def parse_currency(payload):
    return payload.get("currency", "USD").upper()