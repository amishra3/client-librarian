package com.citytechinc.cq.clientlibs.core.services.clientlibs.impl;

import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceDependencyProvider;
import com.google.common.base.Stopwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class BaseConcurrencyTest {
    protected static final int two_seconds = 2000;
    protected static final int ten_seconds = 10000;

    protected final SlowDown letsSlowDown(final int milliseconds) {
        return new SlowDown(milliseconds);
    }

    protected final class SlowDown {
        protected final String template = "Slowing %1$s %2$s ms";

        protected final int milliseconds;

        SlowDown(final int milliseconds) {
            this.milliseconds = milliseconds;
        }

        public void on(final String methodName) {


            System.out.println(String.format(template, methodName, this.milliseconds));

            try { Thread.sleep(this.milliseconds); }catch(final Exception e) {};
        }
    }

    protected final class SlowList<T> extends ArrayList<T> {
        protected List listDelegate;
        protected int readSlowDownInSeconds, writeSlowDownInSeconds;

        public SlowList(int readSlowDownInSeconds, int writeSlowDownInSeconds, List listDelegate) {
            this.listDelegate = listDelegate;
            this.readSlowDownInSeconds = readSlowDownInSeconds;
            this.writeSlowDownInSeconds = writeSlowDownInSeconds;
        }

        @Override
        public boolean contains(Object o) {
            letsSlowDown(this.readSlowDownInSeconds * 1000).on("contains");
            return this.listDelegate.contains(o);
        }

        @Override
        public boolean add(Object e) {
            letsSlowDown(this.writeSlowDownInSeconds * 1000).on("add");
            return this.listDelegate.add(e);
        };

        @Override
        public boolean remove(Object o) {
            letsSlowDown(this.writeSlowDownInSeconds * 1000).on("remove");
            return this.listDelegate.remove(o);
        }
    };

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
            this.stopwatch = Stopwatch.createStarted();
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

        protected void sleep(final int milliseconds) {
            try { Thread.sleep(milliseconds); }catch(final Exception e) {};
        }

        public Split execute() {
            for(final StopwatchThread stopwatchThread : threads) {
                stopwatchThread.start();
                this.sleep(1000);
            }

            this.finish();
            return this;
        }

        public boolean isStillRunning() {
            return this.threadGroup.activeCount() != 0;
        }

        protected void finish() {
            while(this.isStillRunning()) {
                this.sleep(100);

                this.printReport();

            }
            this.printReport();
        }

        protected void printReport() {
            final StringBuffer report = new StringBuffer();
            final String template = "%1$s[%2$s, %3$s ms] ";

            for(StopwatchThread stopwatchThread : this.threads) {
                final String threadState;

                if(stopwatchThread.isAlive()) {
                    threadState = "active";
                }else {
                    if(stopwatchThread.getStopwatch().isRunning()) stopwatchThread.getStopwatch().stop();

                    threadState = "finished";
                }

                final String message =
                    format(template,
                        stopwatchThread.getMarker(),
                        threadState,
                        stopwatchThread.getStopwatch().elapsed(TimeUnit.MILLISECONDS)
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
