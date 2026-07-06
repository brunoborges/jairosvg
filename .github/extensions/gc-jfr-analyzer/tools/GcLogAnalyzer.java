///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.microsoft.gctoolkit:gctoolkit-api:3.7.0
//DEPS com.microsoft.gctoolkit:gctoolkit-parser:3.7.0
//DEPS com.microsoft.gctoolkit:gctoolkit-vertx:3.7.0

// GcLogAnalyzer: parse a HotSpot unified/legacy GC log with Microsoft GCToolkit
// and emit a single JSON report on stdout describing every collection cycle
// (timestamp, duration, type, cause, heap occupancy before/after) plus summary
// statistics (throughput, pause percentiles, cause/type breakdowns).
//
// Usage: jbang GcLogAnalyzer.java <path-to-gc.log>

import com.microsoft.gctoolkit.GCToolKit;
import com.microsoft.gctoolkit.aggregator.Aggregates;
import com.microsoft.gctoolkit.aggregator.Aggregation;
import com.microsoft.gctoolkit.aggregator.Aggregator;
import com.microsoft.gctoolkit.aggregator.Collates;
import com.microsoft.gctoolkit.aggregator.EventSource;
import com.microsoft.gctoolkit.event.MemoryPoolSummary;
import com.microsoft.gctoolkit.event.g1gc.G1GCPauseEvent;
import com.microsoft.gctoolkit.event.generational.GenerationalGCPauseEvent;
import com.microsoft.gctoolkit.io.GCLogFile;
import com.microsoft.gctoolkit.io.SingleGCLogFile;
import com.microsoft.gctoolkit.jvm.JavaVirtualMachine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GcLogAnalyzer {

    // ---- One data point per garbage collection cycle -----------------------
    record GcRecord(double timeStamp, double durationMs, String type, String cause,
                    long heapBeforeKb, long heapAfterKb, long heapSizeKb) {}

    // ---- Aggregation SPI ---------------------------------------------------
    // The abstract Aggregation declares what the concrete summary records and is
    // linked (@Collates) to the Aggregator that feeds it. The Aggregator
    // (@Aggregates) subscribes to the pause events for the collectors we support.
    @Collates(GcEventCollector.class)
    public static abstract class GcEventAggregation extends Aggregation {
        public abstract void record(GcRecord r);
    }

    @Aggregates({EventSource.G1GC, EventSource.GENERATIONAL})
    public static class GcEventCollector extends Aggregator<GcEventAggregation> {
        public GcEventCollector(GcEventAggregation aggregation) {
            super(aggregation);
            register(G1GCPauseEvent.class, this::process);
            register(GenerationalGCPauseEvent.class, this::process);
        }

        private void process(G1GCPauseEvent e) {
            aggregation().record(toRecord(e.getDateTimeStamp().getTimeStamp(), e.getDuration(),
                    label(e.getGarbageCollectionType()), label(e.getGCCause()), e.getHeap()));
        }

        private void process(GenerationalGCPauseEvent e) {
            aggregation().record(toRecord(e.getDateTimeStamp().getTimeStamp(), e.getDuration(),
                    label(e.getGarbageCollectionType()), label(e.getGCCause()), e.getHeap()));
        }

        private static String label(Object enumValue) {
            return enumValue == null ? "Unknown" : enumValue.toString();
        }

        private static GcRecord toRecord(double ts, double durationSec, String type, String cause,
                                         MemoryPoolSummary heap) {
            long before = -1, after = -1, size = -1;
            if (heap != null) {
                before = heap.getOccupancyBeforeCollection();
                after = heap.getOccupancyAfterCollection();
                size = heap.getSizeAfterCollection();
            }
            return new GcRecord(ts, durationSec < 0 ? 0.0 : durationSec * 1000.0, type, cause, before, after, size);
        }
    }

    public static class GcEventSummary extends GcEventAggregation {
        private final List<GcRecord> records = new ArrayList<>();
        @Override public void record(GcRecord r) { records.add(r); }
        @Override public boolean hasWarning() { return false; }
        @Override public boolean isEmpty() { return records.isEmpty(); }
        public List<GcRecord> records() { return records; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: GcLogAnalyzer <path-to-gc.log>");
            System.exit(2);
        }
        Path logPath = Path.of(args[0]);

        GCLogFile logFile = new SingleGCLogFile(logPath);
        GCToolKit gcToolKit = new GCToolKit();
        GcEventSummary summary = new GcEventSummary();
        gcToolKit.loadAggregation(summary);
        JavaVirtualMachine machine = gcToolKit.analyze(logFile);

        List<GcRecord> records = summary.records();
        records.sort((a, b) -> Double.compare(a.timeStamp(), b.timeStamp()));

        double firstTs = records.isEmpty() ? 0.0 : records.get(0).timeStamp();
        double lastTs = records.isEmpty() ? 0.0 : records.get(records.size() - 1).timeStamp();

        // Prefer the runtime duration reported by the model; fall back to span.
        double runtimeSec = 0.0;
        try { runtimeSec = machine.getRuntimeDuration(); } catch (Throwable ignore) {}
        if (runtimeSec <= 0.0) runtimeSec = Math.max(lastTs - firstTs, 0.0);

        double totalPauseMs = records.stream().mapToDouble(GcRecord::durationMs).sum();
        double maxPauseMs = records.stream().mapToDouble(GcRecord::durationMs).max().orElse(0.0);
        double avgPauseMs = records.isEmpty() ? 0.0 : totalPauseMs / records.size();
        double[] sortedPauses = records.stream().mapToDouble(GcRecord::durationMs).sorted().toArray();
        double p95 = percentile(sortedPauses, 0.95);
        double p99 = percentile(sortedPauses, 0.99);

        double totalPauseSec = totalPauseMs / 1000.0;
        double percentPaused = runtimeSec > 0 ? (totalPauseSec / runtimeSec) * 100.0 : 0.0;
        double throughput = Math.max(0.0, 100.0 - percentPaused);

        long peakHeapKb = records.stream().mapToLong(r -> Math.max(r.heapBeforeKb(), r.heapAfterKb())).max().orElse(0);
        long maxHeapSizeKb = records.stream().mapToLong(GcRecord::heapSizeKb).max().orElse(0);

        // Allocation rate: bytes allocated between collections / wall time.
        double allocatedKb = 0.0;
        long prevAfter = -1;
        for (GcRecord r : records) {
            if (prevAfter >= 0 && r.heapBeforeKb() >= 0 && r.heapBeforeKb() > prevAfter) {
                allocatedKb += (r.heapBeforeKb() - prevAfter);
            }
            if (r.heapAfterKb() >= 0) prevAfter = r.heapAfterKb();
        }
        double allocRateMbPerSec = runtimeSec > 0 ? (allocatedKb / 1024.0) / runtimeSec : 0.0;

        Map<String, Integer> causes = new LinkedHashMap<>();
        Map<String, Integer> types = new LinkedHashMap<>();
        for (GcRecord r : records) {
            causes.merge(r.cause(), 1, Integer::sum);
            types.merge(r.type(), 1, Integer::sum);
        }

        // ---- Emit JSON -----------------------------------------------------
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"analyzer\": \"gctoolkit-3.7.0\",\n");
        sb.append("  \"summary\": {\n");
        sb.append("    \"eventCount\": ").append(records.size()).append(",\n");
        sb.append("    \"runtimeSec\": ").append(round(runtimeSec, 3)).append(",\n");
        sb.append("    \"totalPauseMs\": ").append(round(totalPauseMs, 3)).append(",\n");
        sb.append("    \"avgPauseMs\": ").append(round(avgPauseMs, 4)).append(",\n");
        sb.append("    \"maxPauseMs\": ").append(round(maxPauseMs, 4)).append(",\n");
        sb.append("    \"p95PauseMs\": ").append(round(p95, 4)).append(",\n");
        sb.append("    \"p99PauseMs\": ").append(round(p99, 4)).append(",\n");
        sb.append("    \"percentPaused\": ").append(round(percentPaused, 4)).append(",\n");
        sb.append("    \"throughputPercent\": ").append(round(throughput, 4)).append(",\n");
        sb.append("    \"peakHeapKb\": ").append(peakHeapKb).append(",\n");
        sb.append("    \"maxHeapSizeKb\": ").append(maxHeapSizeKb).append(",\n");
        sb.append("    \"allocRateMbPerSec\": ").append(round(allocRateMbPerSec, 3)).append("\n");
        sb.append("  },\n");
        sb.append("  \"causes\": ").append(mapToJson(causes)).append(",\n");
        sb.append("  \"types\": ").append(mapToJson(types)).append(",\n");
        sb.append("  \"events\": [\n");
        for (int i = 0; i < records.size(); i++) {
            GcRecord r = records.get(i);
            sb.append("    {\"t\": ").append(round(r.timeStamp() - firstTs, 4))
              .append(", \"durationMs\": ").append(round(r.durationMs(), 4))
              .append(", \"type\": ").append(quote(r.type()))
              .append(", \"cause\": ").append(quote(r.cause()))
              .append(", \"heapBeforeKb\": ").append(r.heapBeforeKb())
              .append(", \"heapAfterKb\": ").append(r.heapAfterKb())
              .append(", \"heapSizeKb\": ").append(r.heapSizeKb())
              .append("}");
            sb.append(i < records.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        System.out.print(sb);
    }

    private static double percentile(double[] sorted, double q) {
        if (sorted.length == 0) return 0.0;
        int idx = (int) Math.ceil(q * sorted.length) - 1;
        idx = Math.max(0, Math.min(sorted.length - 1, idx));
        return sorted[idx];
    }

    private static double round(double v, int places) {
        double f = Math.pow(10, places);
        return Math.round(v * f) / f;
    }

    private static String mapToJson(Map<String, Integer> m) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (i++ > 0) sb.append(", ");
            sb.append(quote(e.getKey())).append(": ").append(e.getValue());
        }
        return sb.append("}").toString();
    }

    private static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }
}
