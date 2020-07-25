# imc-essentials-in-90-minutes
O'Reilly Course, In-Memory Computing Essentials in 90 minutes


## Starting Ignite Cluster

Start a single-node Ignite cluster running in Docker:

1. Open a terminal window and navigate to the root directory of this project.
2. Start a single-node Ignite cluster in Docker:
`docker-compose -f docker/ignite-cluster.yaml up -d` 
3. Observe the logs of the node:
`docker container logs --follow ignite-server-node-1`

## Monitoring Cluster With GridGain Control Center

GridGain Control Center is a management and monitoring tool for Ignite deployments. In this section you need to deploy
and register it with the cluster.

Deploy the tool with Docker on your local machine:

1. Run the tool in Docker: `docker-compose -f docker/control-center.yaml up -d`
2. Open your browser and navigate to [http://localhost:8443](http://localhost:8443) to confirm that the tool is up and running.
3. [Sign up](https://www.gridgain.com/docs/control-center/latest/creating-account) with the tool by creating an account.
(Note, the account is stored in your local Control Center container and not shared with anybody else).

Next, you need to register the cluster with Control Center to get metrics and execute various commands:

1. Connect to the Ignite node's container: `docker exec -it ignite-server-node-1 bash`
2. Go to the Ignite `bin` folder: `cd apache-ignite/bin/`
3. Tell the cluster to work with your local instance of Control Center: `./management.sh --uri http://localhost:8443,http://control-center-frontend:8443`
4. Go back to Control Center opened in your browser and [register the cluster](https://www.gridgain.com/docs/control-center/latest/clusters#adding-clusters).
Look for the line as follows in the logs of the cluster nodes that prints out an ID of your cluster:
```
[23:18:10] If you are already using Control Center, you can add the cluster manually by its ID:
[23:18:10]  52b2fb3b-dbb7-45af-add3-ef37eeaf4759
```

On [Control Center's Dashboard screen](https://www.gridgain.com/docs/control-center/latest/monitoring/dashboard-overview),
add `Metrics (Table)` widget to [monitor the usage of the off-heap memory](https://www.gridgain.com/docs/control-center/latest/monitoring/metrics#off-heap-memory).
 
## Creating Media Store Schema and Loading Data

Now you need to create a Media Store schema and load the cluster with sample data (go to step 3 if you are already in the 
`bin` folder of an Ignite container):

1. Connect to the Ignite node's container: `docker exec -it ignite-server-node-1 bash`
2. Go to the folder with default Ignite shell scripts: `cd apache-ignite/bin/`
3. Connect to the cluster with SQLLine tool: `./sqlline.sh --verbose=true -u jdbc:ignite:thin://127.0.0.1/`
4. Create the schema and load data: `!run /opt/ignite/ext-config/media_store.sql`

Close connection to the container following these steps:

* Close the SQLLine connection: `!q`
* Type `exit` to exit from `bash`.

## Checking Data Distribution

Having the cluster running and loaded with data, let's see how the records are distributed:
1. Open and run `oreilly.training.KeyValueSampleApp` to see how key-value APIs can be used to get Artists' records from
the cluster. 
2. Improve the application by implementing the logic that returns partitions and nodes the Artists' records
are mapped to (Refer to the TODO item for details). Once you do this, you'll see that all the records are spread across
many partitions that are located on a single cluster node.

Scale out the cluster by adding another node and start the application one more time:
1. Uncomment the `ignite-server-node-2` service in the `ignite-cluster.yaml` file
2. Bring up the second node: `docker-compose -f docker/control-center.yaml up -d`
3. Restart the application. You'll see that now partitions with the Artists' records are spread across the two cluster
nodes.

## Running SQL and Tapping Into Affinity Co-location

Ignite supports SQL for data processing including distributed joins, grouping and sorting. In this section, you're 
going to run basic SQL operations as well as more advanced ones that require data co-location to be set beforehand.

Open Control Center and [go to the SQL notebooks screen](https://www.gridgain.com/docs/control-center/latest/querying).

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

```
SELECT track.trackId, track.name as track_name, genre.name as genre, artist.name as artist,  MAX(milliseconds / (1000 * 60)) as duration FROM track
LEFT JOIN artist ON track.artistId = artist.artistId
JOIN genre ON track.genreId = genre.genreId
WHERE track.genreId < 17
GROUP BY track.trackId, track.name, genre.name, artist.name ORDER BY duration DESC LIMIT 20;
```

You can see that the `artist` column is blank for some records. That's because `Track` and `Artist` tables are not co-located
and the nodes don't have all data available locally during the join phase. Enable [non-colocated joins](https://www.gridgain.com/docs/control-center/latest/querying#non-colocated-joins)
to let the nodes shuffle records between each other during the join phase. Re-execute the query to see all the columns
filled in.

### Joining Two Co-located Tables

The non-colocated joins used above come with a performance penalty, i.e., if the nodes are shuffling large data sets
during the join phase, your performance will suffer. However, it's possible to co-locate `Track` and `Artist` tables, and
avoid using the non-colocated joins:

1. Search for the `CREATE TABLE Track` command in the `media_store.sql` file
2. Replace `PRIMARY KEY (TrackId)` with `PRIMARY KEY (TrackId, ArtistId)`
3. Co-located tracks with artist by adding `affinityKey=ArtistId` to the parameters list of the `WITH ...` operator
4. Stop and remove the cluster containers (you do this to fully clean Ignite metadata): `docker-compose -f docker/ignite-cluster.yaml rm -s -v`
5. Start the cluster back, register it with Control Center and reload the database following instructions of the first three 
sections.
6. Go to Control Center and disable [non-colocated joins](https://www.gridgain.com/docs/control-center/latest/querying#non-colocated-joins)
 flag for the query from the previous section _Joining Two Non-Colocated Tables_.
7. Run that query once again and you'll see that all the `artist` columns are filled in because now all the tracks are
stored together with their artists on the same cluster node.

## Stopping Cluster

Shut down the cluster and remove associated containers: `docker-compose -f docker/ignite-cluster.yaml rm -s -v`

