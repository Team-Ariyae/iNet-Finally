package sp.hamrahvpn.handler;

import static sp.hamrahvpn.data.GlobalData.ApiGithub;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncIPFetcher {
    public static final AtomicReference<String> IP_ADDRESS = new AtomicReference<>();
    private static final AtomicBoolean isFetching = new AtomicBoolean(false);
    private static CompletableFuture<Void> fetchTask = null;
    private static final int MAX_RETRIES = 3;

    public static String getIPAddress() throws InterruptedException, ExecutionException {
        if (isFetching.compareAndSet(false, true)) {
            fetchTask = CompletableFuture.runAsync(() -> {
                int retries = 0;
                boolean success = false;

                while (retries < MAX_RETRIES && !success) {
                    try {
                        URL url = new URL(ApiGithub); // Example URL
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(5000); // 5 seconds timeout
                        connection.setReadTimeout(5000);    // 5 seconds timeout

                        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                            String ip = in.readLine();
                            if (ip != null && !ip.isEmpty()) {
                                IP_ADDRESS.set(ip);
                                success = true; // Successfully fetched IP
                            } else {
                                System.err.println("Empty response received.");
                            }
                        }

                    } catch (Exception e) {
                        retries++;
                        System.err.println("Error fetching IP, retrying... (" + retries + "/" + MAX_RETRIES + ")");
                        e.printStackTrace();
                    }

                    if (!success) {
                        try {
                            Thread.sleep(1000); // Wait 1 second before retrying
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                if (!success) {
                    System.err.println("Failed to fetch IP after " + MAX_RETRIES + " retries.");
                }
            });
        }

        if (fetchTask != null) {
            fetchTask.get();
        }

        return IP_ADDRESS.get();
    }
}

//public class AsyncIPFetcher {
//    // Static final variable to store the IP, initialized as an empty reference
//    public static final AtomicReference<String> IP_ADDRESS = new AtomicReference<>();
//    // AtomicBoolean to ensure that the fetch is done only once
//    private static final AtomicBoolean isFetching = new AtomicBoolean(false);
//
//    // CompletableFuture to handle the asynchronous fetching
//    private static CompletableFuture<Void> fetchTask = null;
//
//    public static String getIPAddress() throws InterruptedException, ExecutionException {
//        // Check if fetching is not already in progress and if not, start fetching
//        if (isFetching.compareAndSet(false, true)) {
//            // Start fetching the IP address
//            fetchTask = CompletableFuture.runAsync(() -> {
////                try {
////                    URL url = new URL(ApiGithub);
////                    try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
////                        String ip = in.readLine();
////                        IP_ADDRESS.set(ip);  // Set the IP address
////                    }
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
//                try {
//                    URL url = new URL(ApiGithub); // یک URL معتبر برای آزمایش
//                    try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
//                        String ip = in.readLine();
//                        if (ip != null && !ip.isEmpty()) {
//                            IP_ADDRESS.set(ip);  // Set the IP address
//                        } else {
//                            System.err.println("Failed to retrieve IP address, received empty response.");
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//            });
//        }
//
//        // Wait for the fetch task to complete and get the IP address
//        if (fetchTask != null) {
//            fetchTask.get();  // This will block until the CompletableFuture is complete
//        }
//
//        return IP_ADDRESS.get();
//    }
//}
