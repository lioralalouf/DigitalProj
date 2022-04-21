## ATTTE Setup

From project root run the following command: mvn clean install -DskipTests


### Running DHP tests 

Run against INT
mvn -pl DHP -Denv=int clean verify

Run Specific Test against INT 
mvn -pl DHP -Denv=int clean verify -Dtest=<className>

Run against TEST
mvn -pl DHP -Denv=test clean verify

Run Specific Test against TEST
mvn -pl DHP -Denv=test clean verify -Dtest=<className>

### Running DSS UI tests

Run against INT
mvn -pl DSSUI -Denv=int clean verify

Run Specific Test against INT
mvn -pl DSSUI -Denv=int clean verify -Dtest=<className>

Run against TEST
mvn -pl DSSUI -Denv=int clean verify

Run Specific Test against TEST
mvn -pl DSSUI -Denv=test clean verify -Dtest=<className>
