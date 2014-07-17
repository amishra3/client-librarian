package com.citytechinc.cq.clientlibs.core.services.clientlibs.impl;

import com.google.common.base.Stopwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class BaseConcurrencyTest {
    protected static final int two_seconds = 2;
    protected static final int ten_seconds = 10;

    protected final SlowDown letsSlowDown(final int seconds) {
        return new SlowDown(seconds);
    }
    protected final class SlowDown {
        protected final int seconds;

        SlowDown(final int seconds) {
            this.seconds = seconds;
        }

        public void now() {
            try { Thread.sleep(this.seconds * 1000); }catch(final Exception e) {};
        }
    }

    protected abstract class NamedRunnable implements Runnable {
        protected String marker;

        public NamedRunnable(final String marker) {
            this.marker = marker;
        }

        public String getMarker() {
            return marker;
        }

    }

    protected final class StopwatchThread extends Thread {
        protected Stopwatch stopwatch;
        protected String marker;

        public StopwatchThread(final ThreadGroup group, final NamedRunnable namedRunnable) {
            super(group, namedRunnable);
            this.marker = namedRunnable.getMarker();
        }

        @Override
        public synchronized void start() {
            stopwatch = Stopwatch.createStarted();
            super.start();
        }

        public Stopwatch getStopwatch() {
            return stopwatch;
        }

        public String getMarker() {
            return marker;
        }
    }

    protected class Split {
        protected final List<StopwatchThread> threads;

        protected final ThreadGroup threadGroup;


        Split(final NamedRunnable ... namedRunnables) {
            this.threadGroup = new ThreadGroup("split");
            this.threads = new ArrayList<StopwatchThread>();

            for(final NamedRunnable namedRunnable : namedRunnables) {
                this.threads.add(new StopwatchThread(this.threadGroup, namedRunnable));
            };
        }

        protected void sleep(final int seconds) {
            try { Thread.sleep(seconds * 1000); }catch(final Exception e) {};
        }

        public Split execute() {
            for(final StopwatchThread stopwatchThread : threads) {
                stopwatchThread.start();
                sleep(1);
            }

            this.finish();
            return this;
        }

        public boolean isStillRunning() {
            return this.threadGroup.activeCount() != 0;
        }

        protected void finish() {
            while(this.isStillRunning()) {
                this.sleep(10);

                this.printReport();

            }
            this.printReport();
        }

        protected void printReport() {
            final StringBuffer report = new StringBuffer();
            String template = "%1$s[%2$s, %3$s sec] ";
            for(StopwatchThread stopwatchThread : this.threads) {
                final String message =
                    format(template,
                        stopwatchThread.getMarker(),
                        stopwatchThread.isAlive() ? "active" : "finished",
                        stopwatchThread.getStopwatch().elapsed(TimeUnit.SECONDS)
                    );
                report.append(message);
            }
            System.out.println(report);
        }
    }

    protected final Split split(final NamedRunnable ... namedRunnables) {
        return new Split(namedRunnables);
    }
}
