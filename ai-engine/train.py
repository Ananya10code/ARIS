"""
ARIS ML Anomaly Detection Engine
Trains Isolation Forest models on historical application behavior.

Architecture Flow:
Historical Telemetry → Feature Engineering → Isolation Forest → Anomaly Score → Incident → AI Root Cause → Fix Recommendation

Telemetry Features Evaluated:
1. Latency (mean_lat, max_lat, last_lat)
2. Request Rate (req_rate)
3. Error Rate (error_rate)
4. Host CPU (cpu_pct)
5. Host Memory (ram_pct)
6. Timeouts (timeout_count)
7. Rate-limit Counts (rate_limit_count)
"""
import pathlib
import joblib
import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.metrics import f1_score, precision_score, recall_score

MODELS = pathlib.Path(__file__).parent / "models"
API_FEATURES = [
    "mean_lat",          # Latency: mean response time (ms)
    "max_lat",           # Latency: peak response time (ms)
    "last_lat",          # Latency: instantaneous latency (ms)
    "req_rate",          # Request Rate: requests per second (req/s)
    "error_rate",        # Error Rate: ratio of 4xx/5xx errors (0.0 - 1.0)
    "cpu_pct",           # Host CPU utilization (%)
    "ram_pct",           # Host Memory utilization (%)
    "timeout_count",     # Timeouts: connection drops / socket timeouts
    "rate_limit_count"   # Rate-limit: HTTP 429 count
]
HOST_FEATURES = ["cpu_last", "cpu_mean", "cpu_max", "ram_pct"]
rng = np.random.default_rng(42)


def api_windows(n, anomaly_frac):
    """
    Simulates historical telemetry windows.
    Normal baseline:
      - Latency ~120-180ms
      - Request rate ~1.5 - 4.0 req/s
      - Error rate ~0.0
      - CPU ~20-35%
      - RAM ~50-60%
      - Timeouts = 0
      - Rate-limit counts = 0
    Anomalies:
      - Latency spikes / degradation
      - Traffic bursts / request rate surges
      - Error rate bursts (5xx / 4xx)
      - Host CPU/memory saturation
      - Timeout storms
      - Rate limit 429 quota exhaustion
    """
    X, y = [], []
    for _ in range(n):
        # Baseline normal telemetry
        lat = np.clip(rng.normal(rng.normal(150, 30), 20, 10), 10, None)
        req_rate = float(rng.uniform(1.5, 4.0))
        err = 0.05 if rng.random() < 0.02 else 0.0
        cpu = float(np.clip(rng.normal(25, 5), 5, 50))
        ram = float(np.clip(rng.normal(55, 6), 30, 75))
        timeouts = 0
        rate_limits = 0

        a = rng.random() < anomaly_frac
        if a:
            kind = rng.integers(6)
            if kind == 0:
                # Latency spike
                lat[-rng.integers(1, 5):] *= rng.uniform(6, 18)
            elif kind == 1:
                # Error rate burst (5xx server errors)
                err = float(rng.uniform(0.4, 1.0))
            elif kind == 2:
                # Traffic / Request rate spike
                req_rate *= rng.uniform(8, 25)
            elif kind == 3:
                # CPU / Memory spike
                cpu = float(rng.uniform(85, 99))
                ram = float(rng.uniform(88, 98))
            elif kind == 4:
                # Timeout storm (network drops)
                timeouts = int(rng.integers(2, 7))
                err = float(rng.uniform(0.3, 0.8))
            elif kind == 5:
                # Rate limit exhaustion (HTTP 429)
                rate_limits = int(rng.integers(3, 9))
                err = float(rng.uniform(0.5, 1.0))

        X.append([
            float(lat.mean()),
            float(lat.max()),
            float(lat[-1]),
            req_rate,
            err,
            cpu,
            ram,
            float(timeouts),
            float(rate_limits)
        ])
        y.append(int(a))
    return np.array(X), np.array(y)


def host_windows(n, anomaly_frac):
    """Windows of host samples. Normal CPU ~25%, RAM ~55%; anomalies = CPU spike, RAM pressure."""
    X, y = [], []
    for _ in range(n):
        cpu = np.clip(rng.normal(rng.normal(25, 8), 4, 10), 1, 100)
        ram = rng.normal(55, 8)
        a = rng.random() < anomaly_frac
        if a:
            cpu[-rng.integers(2, 6):] = rng.uniform(85, 100)
            if rng.random() < 0.5:
                ram = rng.uniform(88, 98)
        X.append([cpu[-1], cpu.mean(), cpu.max(), ram])
        y.append(int(a))
    return np.array(X), np.array(y)


def fit(name, gen, features):
    Xtr, _ = gen(6000, 0.02)          # training data: mostly normal historical telemetry
    model = IsolationForest(n_estimators=200, contamination=0.03, random_state=42).fit(Xtr)
    Xte, yte = gen(2000, 0.15)        # labelled test set to verify detection accuracy
    pred = (model.predict(Xte) == -1).astype(int)
    print(f"{name}: precision={precision_score(yte, pred):.2f} recall={recall_score(yte, pred):.2f} "
          f"f1={f1_score(yte, pred):.2f}  (synthetic evaluation)")
    joblib.dump({"model": model, "features": features, "mean": Xtr.mean(0), "std": Xtr.std(0) + 1e-9},
                MODELS / f"{name}.joblib")


def main():
    MODELS.mkdir(exist_ok=True)
    fit("api", api_windows, API_FEATURES)
    fit("host", host_windows, HOST_FEATURES)


if __name__ == "__main__":
    main()
