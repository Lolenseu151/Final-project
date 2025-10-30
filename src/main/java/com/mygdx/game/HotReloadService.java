package com.mygdx.game;

import com.badlogic.gdx.Gdx;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple file watcher for development-time hot reloading of UI assets.
 * Watches a directory (e.g., assets/ui) and toggles a dirty flag when files change.
 * Desktop-only; safe no-op on other platforms.
 */
public class HotReloadService implements AutoCloseable {
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private WatchService watchService;
    private Thread thread;

    public HotReloadService(String directoryToWatch) {
        try {
            Path dir = Paths.get(directoryToWatch);
            if (!Files.isDirectory(dir)) {
                // No directory; do nothing
                Gdx.app.log("HotReload", "Watch directory not found: " + directoryToWatch);
                return;
            }
            this.watchService = FileSystems.getDefault().newWatchService();
            dir.register(this.watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
            this.thread = new Thread(this::runWatchLoop, "ui-hot-reload");
            this.thread.setDaemon(true);
            this.thread.start();
        } catch (Exception e) {
            Gdx.app.error("HotReload", "Failed to start watcher: " + e.getMessage());
        }
    }

    private void runWatchLoop() {
        try {
            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    // Trigger reload on any change to typical UI asset extensions
                    Object ctx = event.context();
                    if (ctx != null) {
                        String name = ctx.toString().toLowerCase();
                        if (name.endsWith(".json") || name.endsWith(".atlas") || name.endsWith(".png")) {
                            dirty.set(true);
                        }
                    }
                }
                key.reset();
            }
        } catch (InterruptedException ignored) {
            // Thread interrupted on dispose
        }
    }

    /**
     * @return true once when a change is detected; resets the flag.
     */
    public boolean pollReload() {
        return dirty.getAndSet(false);
    }

    public void dispose() {
        try {
            close();
        } catch (Exception ignored) {}
    }

    @Override
    public void close() throws IOException {
        if (thread != null) thread.interrupt();
        if (watchService != null) watchService.close();
    }
}
