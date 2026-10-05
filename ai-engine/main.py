"""Run: uvicorn main:app --port 8000"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import engine

app = FastAPI(title="ARIS AI Engine")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


@app.get("/api/analysis")
def analysis(project_id: int = 1):
    try:
        return {"ok": True, "incidents": engine.analyse(project_id)}
    except Exception as e:
        return {"ok": False, "error": f"Cannot read the ARIS backend: {e}", "incidents": []}


@app.get("/api/history")
def history(project_id: int = 1):
    return [h for h in engine.HISTORY if h["project"] == project_id]


@app.post("/api/retrain")
def retrain():
    return engine.retrain()
