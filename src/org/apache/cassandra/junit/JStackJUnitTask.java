package org.apache.cassandra.junit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTask;
import org.apache.tools.ant.util.Watchdog;

public class JStackJUnitTask extends JUnitTask
{
    private Integer timeout;

    public JStackJUnitTask() throws Exception
    {
    }

    @Override
    public void setTimeout(Integer timeout)
    {
        this.timeout = timeout;
        super.setTimeout(timeout);
    }

    @Override
    public ExecuteWatchdog createWatchdog() throws BuildException
    {
        return new JStackWatchDog(timeout);
    }

    private static class JStackWatchDog extends ExecuteWatchdog
    {
        private long pid;

        public JStackWatchDog(long timeout)
        {
            super(timeout);
        }

        public JStackWatchDog(int timeout)
        {
            super(timeout);
        }

        @Override
        public synchronized void start(Process process)
        {
            this.pid = getPid(process);
            super.start(process);
        }

        @Override
        public synchronized void timeoutOccured(Watchdog w)
        {
            if (pid > 0)
            {
                ProcessBuilder pb = new ProcessBuilder("jstack","-l", String.valueOf(pid));
                try
                {
                    Process p = pb.start();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())))
                    {
                        System.out.println(br.lines().collect(Collectors.joining("\n")));
                    }
                }
                catch (IOException e)
                {
                    System.err.println("Could not get stack for "+pid);
                    e.printStackTrace();
                }
            }
            super.timeoutOccured(w);
        }

        private long getPid(Process process)
        {
            if (process.getClass().getName().equals("java.lang.UNIXProcess"))
            {
                try
                {
                    Field f = process.getClass().getDeclaredField("pid");
                    f.setAccessible(true);
                    long pid = f.getLong(process);
                    f.setAccessible(false);
                    return pid;
                }
                catch (IllegalAccessException | NoSuchFieldException e)
                {
                    System.err.println("Could not get PID");
                }
            }
            return -1;
        }
    }

}
