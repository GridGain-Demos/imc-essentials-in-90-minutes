# Code Samples for O'Reilly Training, In-Memory Computing Essentials in 90 minutes

This project is for the live [O'Reilly Training - In-Memory Computing Essentials in 90 minutes](https://learning.oreilly.com/live-training/courses/in-memory-computing-essentials-in-90-minutes/0636920455431/).
The code samples demonstrate the essential capabilities of in-memory computing platforms such as Apache Ignite in practice.

You can study the samples by following the instructor during the training or completing this guide on your own.

## Setting Up Environment

* Java Developer Kit, version 8 or later
* Apache Maven 3.0 or later
* Your favorite IDE, such as IntelliJ IDEA, or Eclipse, or a simple text editor.

## Starting Ignite Cluster

Start a two-node Ignite cluster:

1. Open a terminal window and navigate to the root directory of this project.

2. Use Maven to create a core executable JAR with all the dependencies:
    ```bash
    mvn clean package -P core
    ```
3. Start the first cluster node:
    ```bash
    java -cp libs/core.jar training.ServerStartup
    ```

4. Open another terminal window and start the second node:
    ```bash
    java -cp libs/core.jar training.ServerStartup
    ```

Both nodes auto-discover each other and you'll have a two-nodes cluster ready for exercises.
 
## Creating Media Store Schema and Loading Data

Now you need to create a Media Store schema and load the cluster with sample data. Use SQLLine tool to achieve that:

1. Launch a SQLLine process:
    ```bash
    java -cp libs/core.jar sqlline.SqlLine
    ```
   
2. Connect to the cluster:
    ```bash
    !connect jdbc:ignite:thin://127.0.0.1/ ignite ignite
    ```

3. Load the Media Store database:
    ```bash
    !run config/media_store.sql
    ```

4. List all the tables of the database:
    ```bash
    !tables
    ```

Keep the connection open as you'll use it for following exercises.

## Data Partitioning - Checking Data Distribution

In this section you'll learn how to use key-value APIs for data processing and how to print partitions
distribution across the cluster nodes:

1. Check the source code of `training.KeyValueApp` to see how key-value APIs are used to get Artists' records from
the cluster.

2. Build an executable JAR with the applications' classes:
    ```bash
    mvn clean package -P apps
    ```
   
3. Run the application to see what result it produces: 
    ```bash
    java -cp libs/apps.jar training.KeyValueApp
    ```

4. Improve the application by implementing the logic that prints out the current partitions distribution
(Refer to the TODO item for details).

Optional, scale out the cluster by the third node and run the application again. You'll see that some partitions were
moved to the new node.

## Affinity Co-location - Optimizing Complex SQL Queries With JOINs

Ignite supports SQL for data processing including distributed joins, grouping and sorting. In this section, you're 
going to run basic SQL operations as well as more advanced ones.

### Querying Single Table

Run the following query to find top-20 longest tracks:

```
SELECT trackid, name, MAX(milliseconds / (1000 * 60)) as duration FROM track
WHERE genreId < 17
GROUP BY trackid, name ORDER BY duration DESC LIMIT 20;
```

### Joining Partitioned and Replicated Tables

The next query is a modification of the first one but with details about musical genres of those top-20 longest tracks:

```
SELECT track.trackid, track.name, genre.name, MAX(milliseconds / (1000 * 60)) as duration FROM track
JOIN genre ON track.genreId = genre.genreId
WHERE track.genreId < 17
GROUP BY track.trackid, track.name, genre.name ORDER BY duration DESC LIMIT 20;
``` 

Since `Genres` table is fully replicated across all the cluster nodes, the join between the two tables is safe and
Ignite always return a correct result set.

### Joining Two Non-Colocated Tables

Modify the last query by adding information about an author who is in the top-20 longest. You do this by doing a LEFT
JOIN with `Artist` table:

```sql
SELECT track.trackId, track.name as track_name, genre.name as genre, artist.name as artist,
MAX(milliseconds / (1000 * 60)) as duration FROM track
LEFT JOIN artist ON track.artistId = artist.artistId
JOIN genre ON track.genreId = genre.genreId
WHERE track.genreId < 17
GROUP BY track.trackId, track.name, genre.name, artist.name ORDER BY duration DESC LIMIT 20;
```

You can see that the `artist` column is blank for some records. That's because `Track` and `Artist` tables are not co-located
and the nodes don't have all data available locally during the join phase. 

Open an SQLLine connection with the non-colocated joins:
```bash
!connect jdbc:ignite:thin://127.0.0.1?distributedJoins=true ignite ignite
```
Re-execute the query to see all the columns filled in.

### Joining Two Co-located Tables

The non-colocated joins used above come with a performance penalty, i.e., if the nodes are shuffling large data sets
during the join phase, your performance will suffer. However, it's possible to co-locate `Track` and `Artist` tables, and
avoid using the non-colocated joins:

1. Search for the `CREATE TABLE Track` command in the `media_store.sql` file
2. Replace `PRIMARY KEY (TrackId)` with `PRIMARY KEY (TrackId, ArtistId)`
3. Co-located tracks with artist by adding `affinityKey=ArtistId` to the parameters list of the `WITH ...` operator
4. Clean the Ignite work directory `${project}/ignite/work`
5. Restart the cluster nodes
6. Reconnect with SQLLine 
    ```bash
    !connect jdbc:ignite:thin://127.0.0.1 ignite ignite
    ```
7. Run that query once again and you'll see that all the `artist` columns are filled in because now all the tracks are
stored together with their artists on the same cluster node.

## Running Co-located Compute Tasks

Run `training.ComputeApp` that uses Apache Ignite compute capabilities for a calculation of the top 5 paying customers.
The compute task executes on every cluster node, iterates through local records and responds to the application that merges partial
results.

Run the app to see how it works:
```bash
java -cp libs/apps.jar training.ComputeApp
```

The computation doesn't produce a correct result, thus, you need to do the following:

1. Fix the issue by addressing the TODO left in the code.

2. Build an executable JAR with the applications' classes:
    ```bash
    mvn clean package -P apps
    ```
3. Run the app again:
    ```bash
    java -cp libs/apps.jar training.ComputeApp
    ```

