package com.aris.probe;
import jakarta.persistence.*;

/** One probe of one monitor. Public fields keep this compact. ts = epoch millis. */
@Entity
@Table(name = "probe_results")
public class ProbeResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long monitorId;
    public long ts;
    public int statusCode;      // 0 = no response (timeout / connection error)
    public long latencyMs;
    public long responseSize;
    @Column(length = 500) public String error;
    public ProbeResult() {}
}
