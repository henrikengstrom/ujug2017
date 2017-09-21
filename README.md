# ujug2017

Source code and slides for Utah JUG presentation.

The intention of the source code and slides is to show some part of Akka Actors and Streams.

It also showcases the power of the Akka Streams API and how it can be used to simplify a lot of logic that you have to hand craft when using Akka Actors.

## The demo code

This demo uses a naive implementation of two services: A and B. Both services are implemented with Akka Http.
A consumes data from B utilizing HTTP calls.

## To run

To drive the different scenarios you should always have Service B running. There is no need to restart it, as its business logic remains the same throughout the demo. 

### Run Service B
Open up a Terminal window:

```
> sbt
> run

Multiple main classes detected, select one to run:

 [1] example.RSExample1
 [2] sample.service_a.ActorMain
 [3] sample.service_a.StreamMain
 [4] sample.service_b.Main

Enter number: 4

[info] Running sample.service_b.Main
Server online at http://localhost:8081/
Press RETURN to stop...
```

### Run demo 0 - plain vanilla Akka app

Run in a Terminal:

```
> sbt
> run 0
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] example.RSExample1
 [2] sample.service_a.ActorMain
 [3] sample.service_a.StreamMain
 [4] sample.service_b.Main

Enter number: 2

[info] Running sample.service_a.ActorMain 0
Server online at http://localhost:8080/
Press RETURN to stop...
```

### Run demo 1 - batch the calls

Run in a Terminal:

```
> sbt
> run 1
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] example.RSExample1
 [2] sample.service_a.ActorMain
 [3] sample.service_a.StreamMain
 [4] sample.service_b.Main

Enter number: 2

[info] Running sample.service_a.ActorMain 0
Server online at http://localhost:8080/
Press RETURN to stop...
```

### Run demo 2 - batch and schedule

Run in a Terminal:

```
> sbt
> run 2
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] example.RSExample1
 [2] sample.service_a.ActorMain
 [3] sample.service_a.StreamMain
 [4] sample.service_b.Main

Enter number: 2

[info] Running sample.service_a.ActorMain 0
Server online at http://localhost:8080/
Press RETURN to stop...
```

### Run demo 3 - limit in-flight calls

Run in a Terminal:

```
> sbt
> run 3
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] example.RSExample1
 [2] sample.service_a.ActorMain
 [3] sample.service_a.StreamMain
 [4] sample.service_b.Main

Enter number: 2

[info] Running sample.service_a.ActorMain 0
Server online at http://localhost:8080/
Press RETURN to stop...
```

### Run the Akka Streams implementation

Run in a Terminal:

```
> sbt
> run
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] example.RSExample1
 [2] sample.service_a.ActorMain
 [3] sample.service_a.StreamMain
 [4] sample.service_b.Main

Enter number: 3

[info] Running sample.service_a.StreamMain
Server online at http://localhost:8080/
Press RETURN to stop...
```

## Credits and insiration

Make sure to check out Colin Breck's [awesome blog post]() about Akka Streams.

Thanks to my colleagues for your help with all my silly questions:
* Johan Andrén
* Björn Antonsson
* Konrad Malawski
* Peter Vlugter
