/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.yardstick.examples.echo;

import org.yardstick.*;
import org.yardstick.util.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Echo server benchmark. This benchmark has client and server counterparts.
 */
public class EchoServerBenchmark extends BenchmarkDriverAdapter {
    /** Counter. */
    private final AtomicInteger cntr = new AtomicInteger();

    /** Thread to socket map. */
    private final ConcurrentMap<Thread, Socket> sockMap = new ConcurrentHashMap<>();

    /** Arguments. */
    private EchoServerBenchmarkArguments args;

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        args = new EchoServerBenchmarkArguments();

        BenchmarkUtils.jcommander(cfg.commandLineArguments(), args, "<echo-driver>");
    }

    /** {@inheritDoc} */
    @Override public void tearDown() throws Exception {
        for (Socket sock : sockMap.values())
            sock.close();
    }

    /** {@inheritDoc} */
    @Override public void test() throws Exception {
        Socket sock = socket(args);

        String req = "ping-" + cntr.incrementAndGet();

        byte[] reqBytes = req.getBytes();

        sock.getOutputStream().write(reqBytes);

        byte[] resBytes = new byte[reqBytes.length];

        InputStream in = sock.getInputStream();

        int read = 0;

        while (read < resBytes.length) {
            int b = in.read(resBytes, read, resBytes.length - read);

            if (b < 0)
                break;

            read += b;
        }

        String res = new String(resBytes);

        if (!req.equals(res))
            throw new Exception("Invalid echo response [req=" + req + ", res=" + res + ']');
    }

    /**
     * Initialize socket per thread.
     *
     * @param args Echo server arguments.
     * @return Socket for this thread.
     * @throws Exception If failed.
     */
    private Socket socket(EchoServerBenchmarkArguments args) throws Exception {
        Socket sock = sockMap.get(Thread.currentThread());

        if (sock == null) {
            Socket old = sockMap.putIfAbsent(Thread.currentThread(), sock = new Socket(args.host(), args.port()));

            if (old != null)
                sock = old;
        }

        return sock;
    }
}
