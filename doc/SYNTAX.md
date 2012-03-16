# Example
We believe that our explanation will be more efficient if we use some example model.
We decide to use the same model which we use for testing:

<img src="https://github.com/vdim/yz/raw/master/doc/son.png" alt="Son model"/>

(This model is developed for the Nest project and its authors are Mikhail Kryshen and Alexander Kolosov.)

Let's consider some snapshot of real data: 

(This figure is produced via the Nest visualization module which is based on indyvon library.)

## Using example
You can use this example and experiment with further on queries from YZ's repository source code.
For this you should: 
1. Execute first seven steps from [testing] (https://github.com/vdim/yz/blob/master/doc/TESTING.md) document.

2. Run REPL:
<pre><code>vdim:~/yz/test$ lein repl </code></pre>

3. Change namespace (if any):

	user=> (in-ns 'ru.petrsu.nest.yz.init)
	#<Namespace ru.petrsu.nest.yz.init>

4. Run query

	ru.petrsu.nest.yz.init=> (pquery "room" mem)

Note: mem is some instance of the ElementManager.


# YZ
At last let's begin to consider YZ.

## Selection
