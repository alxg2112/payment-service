# Build and Deploy

* Build a fat jar using `mvn clean package` command.
* Launch the application using `java -jar payment-service-1.0-SNAPSHOT.jar`.
* You also can override default properties defined in `resources` by placing them into `config` subdirectory at application jar's  location.

# Running API tests

API tests are provided via Postman Collection (`postman-tests` directory). 

To run them open the collection in Postman via [Collection Runner](https://learning.getpostman.com/docs/postman/collection_runs/starting_a_collection_run/).

**NB:** API tests depend on default data and configuration. Also, test runs are **NOT** idempotent, so they would pass only if application in it's initial state.
 
