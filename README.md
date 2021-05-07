# How to run
You will need maven and jdk installed to run test application.
To run, execute from project folder:
                                                                            
mvn exec:java -Dexec.mainClass="TestApp" -Dexec.args="-n 1000 -cn 10 -runs 2"

Arguments:
 - -n - initial replica size
 - -cn - number of clients
 - -runs - number of test runs
 - -rt - cycle run time in seconds

# Implementation details
Implementation is based on concept of [Operational Transformation](https://en.wikipedia.org/wiki/Operational_transformation).

TestApp runs server and clients in a single thread and simulates random updates to target 5 operations p/client p/second.

The test runs multiple test cycles (set by -runs parameter), each simulating seconds of runtime set by -rt parameter (10 by default), 
i.e. simulates number of operations expected to happen in given period.

Operations are executed without delay as they generated, therefore a cycle run time can be different. 
E.g. for parameters: -n=10000000,-cn=20,rt=10 cycle time is usually less than 4 seconds when run on dev laptop, 
i.e. replication lag for these parameters is confidently less than 1 second.

At the end of each cycle, state for each of the parties is verified to be in sync.
 
