# Installation

## Build from source code
1. Install [lein] (https://github.com/technomancy/leiningen) build tool.
2. Download source code from repository:
<pre>
<code>
$ git clone http://github.com/vdim/yz
$ cd yz
</code>
</pre>

3. Download dependencies:
<pre><code>$ lein deps</code></pre>

4. Compile code:
<pre><code>$ lein compile</code></pre>

5. Create jar file:
<pre><code>$ lein jar</code></pre>

## Install from repository
YZ is distributed due to [clojars.org] (http://clojars.org), so
if you use some dependency manager ([ivy] (http://ant.apache.org/ivy/), 
[maven] (http://maven.apache.org/), [leiningen] (https://github.com/technomancy/leiningen) and so on) you
can use it for getting YZ. For example, you can write for ivy 
something like this:
	
	<dependency org="ru.petrsu.nest" name="yz" rev="0.0.1-alpha13"/>

Direct link: [http://clojars.org/ru.petrsu.nest/yz] (http://clojars.org/ru.petrsu.nest/yz)

