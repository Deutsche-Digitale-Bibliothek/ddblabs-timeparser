public class ThreadSafeSmoke {
    public static void main(String[] args) throws Exception {
        var parser = de.ddb.labs.timeparser.TimeParser.getInstance();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        var inputs = java.util.List.of("Mai 2010", "2. Jahrhundert", "um 1900", "1945-03-12", "1. Hälfte 5. Jahrhundert");
        var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < 200; i++) {
            final String in = inputs.get(i % inputs.size());
            futures.add(pool.submit(() -> {
                var result = parser.parseTimeResult(in);
                if (result == null || result.getOutput() == null) {
                    throw new RuntimeException("unexpected null result");
                }
            }));
        }
        for (var future : futures) {
            future.get();
        }
        pool.shutdown();
        System.out.println("THREAD_SMOKE_OK");
    }
}
