package org.infospray.peonsimulator.supervisor.adapter.secondary.process;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

final class ProcessExecution {
    private ProcessExecution() {
    }

    static String run(Path directory, Duration timeout, String standardInput, List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
        try {
            Process process = builder.start();
            if (standardInput != null) {
                process.getOutputStream().write(standardInput.getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().close();
            }
            CompletableFuture<byte[]> outputReader = new CompletableFuture<>();
            Thread.ofVirtual().start(() -> {
                try { outputReader.complete(readOutput(process)); } catch (RuntimeException exception) { outputReader.completeExceptionally(exception); }
            });
            boolean completed = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!completed) { process.destroyForcibly(); throw new IllegalStateException("Commande expirée après " + timeout.toMinutes() + " minutes: " + command.getFirst()); }
            String output = new String(outputReader.get(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) { throw new IllegalStateException("Commande en échec (" + process.exitValue() + "): " + output.strip()); }
            return output.strip();
        } catch (IOException exception) { throw new IllegalStateException("Impossible de lancer la commande " + command.getFirst(), exception); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("Commande interrompue", exception); } catch (ExecutionException exception) { throw new IllegalStateException("Impossible de lire la sortie de la commande", exception.getCause()); }
    }

    private static byte[] readOutput(Process process) {
        try { return process.getInputStream().readAllBytes(); } catch (IOException exception) { throw new IllegalStateException("Impossible de lire la sortie du processus", exception); }
    }
}
