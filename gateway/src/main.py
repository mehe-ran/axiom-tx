from fastapi import FastAPI

app = FastAPI(title="AxiomTx Gateway")

@app.get("/health")
async def health_check():
    return {"status": "ok"}