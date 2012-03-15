# Testing

For testing separate project is used (it is within the test directory). So for running tests you should:

1. Download source code from github:
<pre><code>vdim:~/$ git clone http://github.com/vdim/yz</code></pre>

2. Download dependencies for YZ project:
<pre><code>vdim:~/yz$ lein deps</code></pre>

3. Compile java sources from YZ project and run its aot compilation:
<pre><code>vdim:~/yz$ lein compile </code></pre>

4. Cd to the test directory with test project:
<pre><code>vdim:~/yz$ cd yz/test</code></pre>

5. Download dependencies for test project:
<pre><code>vdim:~/yz/test$ lein deps</code></pre>

6. Hack for generating classes of the Nest model:
<pre><code>vdim:~/yz/test$ ant </code></pre>

7. Compile test project:
<pre><code>vdim:~/yz/test$ lein compile </code></pre>

8. Run tests:
<pre><code>vdim:~/yz/test$ lein test </code></pre>

Some comments:

1. The SON model (authors are Mikhail Kryshen and Alexander Kolosov) from Nest project is used:
<img src="https://github.com/vdim/yz/raw/master/doc/son.png" alt="Son model"/>

2. The idea of tests is:
    * Creates specified model with fixed objects, its links and properties (usually separated model is used
for each test's namespace). 
    * Runs specified queries.
    * Checks whether result of a query corresponds to a real.

3. So changing test models may lead to incorrect tests.

4. It is no concern of the ru.petrsu.nest.yz.test-parsing/qlist 
([source] (https://github.com/vdim/yz/blob/master/test/src/ru/petrsu/nest/yz/test_parsing.clj)) vector. 
You may add to this vector any queries without any troubles.

# Benchmarking
There is system for benchmarking the YZ language. 
See script [bench_indq.sh] (https://github.com/vdim/yz/blob/master/test/bench_indq.sh) for more details:
<pre><code>$ ./bench_indq.sh -h  </code></pre>

Some notes about benchmark's system:

1. Model is generated. See script [cr_db.sh] (https://github.com/vdim/yz/blob/master/test/cr_db.sh) for more details:
<pre><code>$ ./cr_db.sh -h </code></pre>

2. Amount of elements into model is specified.
2. Supported languages: YZ, HQL.
3. Supported databases: H2, Derby, HSQLDB.
4. Supported measurement: 
    * time (difference between system time before execution query and after execution query)
    * memory (amount of memory after execution query and running gc, [this][memory] is used) 
    * thread cpu time ([this][cpu] is used)
    * thread user time ([this][user] is used).

5. We compare queries from vector ru.petrsu.nest.yz.benchmark.yz/individual-queries-jpa 
([source] (https://github.com/vdim/yz/blob/master/test/src/ru/petrsu/nest/yz/benchmark/yz.clj)) and 
vector ru.petrsu.nest.yz.benchmark.hql/individual-queries 
([source] (https://github.com/vdim/yz/blob/master/test/src/ru/petrsu/nest/yz/benchmark/hql.clj)) which are correspond
each other.

Example of a scenario may be something like this:

1. Execute first seven steps from testing section.

2. Generate H2 databases with 10000 and 20000 elements:
<pre><code>vdim:~/yz/test$ ./cr_db.sh -d h2 -n "10000 20000" </code></pre>

3. Benchmark YZ language for simple selection with simple filtering (1 query (started with 0)):
<pre><code>vdim:~/yz/test$ ./bench_indq.sh -l yz -t hdd -d h2 -n "10000 20000" -q 1</code></pre>

4. Benchmark HQL language for simple selection with simple filtering (1 query (started with 0)):
<pre><code>vdim:~/yz/test$ ./bench_indq.sh -l hql -t hdd -d h2 -n "10000 20000" -q 1</code></pre>

So you should get file 1.txt with result of benchmark YZ and HQL simple selection with simple filtering query 
for H2 database with 10000 and 20000 amount of elements.

[memory]: http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/management/MemoryUsage.html#getUsed() "MemoryUsage/getUsed"
[cpu]: http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/management/ThreadMXBean.html#getCurrentThreadCpuTime() "getCurrentThreadCpuTime"
[user]: http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/management/ThreadMXBean.html#getCurrentThreadUserTime() "getCurrentThreadUserTime"

