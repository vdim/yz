# Using the collq function for testing queries

1. Install [lein] (https://github.com/technomancy/leiningen) build tool.

2. Download source code from github:
<pre><code>vdim:~/$ git clone http://github.com/vdim/yz</code></pre>

3. Cd to the yz directory:
<pre><code>vdim:~/$ cd yz</code></pre>

4. Download dependencies for YZ project:
<pre><code>vdim:~/yz$ lein deps</code></pre>

5. Compile java sources from YZ project and run its aot compilation:
<pre><code>vdim:~/yz$ lein compile </code></pre>

6. Cd to the test directory with test project:
<pre><code>vdim:~/yz$ cd test</code></pre>

7. Download dependencies for test project:
<pre><code>vdim:~/yz/test$ lein deps</code></pre>

8. Hack for generating some classes:
<pre><code>vdim:~/yz/test$ ant </code></pre>

9. Compile test project:
<pre><code>vdim:~/yz/test$ lein compile </code></pre>

10. Run REPL:
<pre><code>
    vdim:~/yz/test$ lein repl 
    REPL started; server listening on localhost:20688.
    user=>
</code></pre>

11. Change namespace (if any):
<pre><code>
    user=> (in-ns 'ru.petrsu.nest.yz.init)
    ru.petrsu.nest.yz.init=>
</code></pre>

12. Define collection with strings:
<pre><code>
    ru.petrsu.nest.yz.init=> (def names ["Bob" "Alice" "" "Marry" "Kris" "David" "Alexander"])
    #'ru.petrsu.nest.yz.init/names
    ru.petrsu.nest.yz.init=>
</code></pre>

13. At last try query:
<pre><code>
    ru.petrsu.nest.yz.init=> (collq "string" names)
    (["Bob"] ["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])
    ru.petrsu.nest.yz.init=>
</code></pre>
