package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final String CONFIG_FILE = "config.yml";

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration config = loadFromFile(CONFIG_FILE);
        this.model = new DrawNumberImpl(config.getMin(), config.getMax(), config.getAttempts());
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(new DrawNumberViewImpl());
    }

    private Configuration loadFromFile(final String filePath) {
        Configuration.Builder configBuilder = new Configuration.Builder();
        try (
            final BufferedReader in = new BufferedReader(
                                          new InputStreamReader(ClassLoader.getSystemResourceAsStream(filePath))) 
        ) {
            configBuilder.setMin(Integer.parseInt(new StringTokenizer(in.readLine(), "minimum: ").nextToken()));
            configBuilder.setMax(Integer.parseInt(new StringTokenizer(in.readLine(), "maximum: ").nextToken()));
            configBuilder.setAttempts(Integer.parseInt(new StringTokenizer(in.readLine(), "attempts: ").nextToken()));
        } catch (Exception e) {
            configBuilder = new Configuration.Builder();
            for (final DrawNumberView view: views) {
                view.displayError("Error reading config file, using default values. " + e);
            }
        }
        final Configuration config = configBuilder.build();
        if (config.isConsistent()) {
            return config;
        } else {
            for (final DrawNumberView view: views) {
                view.displayError("The configurations read from the file are invalid, using default values.");
            }
            return new Configuration.Builder().build();
        }
    }
}
