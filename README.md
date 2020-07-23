# imc-essentials-in-90-minutes
O'Reilly Course, In-Memory Computing Essentials in 90 minutes


## Start Cluster Nodes

Open a terminal window and navigate to the root directory of this project.

Start a two-node Ignite cluster in Docker. Each Ignite node is deployed as a separate container:
`docker-compose -f docker/ignite-cluster.yaml up -d` 

Attach standard input, output and error streams to the first node:
`docker container logs --follow ignite-server-node-1`

Open a new terminal window and execute the command again to see the logs of the second node:
`docker container logs --follow ignite-server-node-2`

## Monitor Cluster With GridGain Control Center

GridGain Control Center is a management and monitoring tool for Ignite deployments. Deploy the tool with Docker on your 
local machine by running the following command in a terminal window:

`docker-compose -f docker/control-center.yaml up -d`

Connect to any of the docker containers with a command line shell:

`docker exec -it ignite-server-node-1 bash`

## Create Media Store Schema and Load Data

Connect to any of the docker containers with a command line shell:

`docker exec -it ignite-server-node-1 bash`

Go to the folder with default Ignite shell scripts:

`cd apache-ignite/bin/`

Connect to the cluster (via the node of the container you are connected to) with SQLLine tool:

`./sqlline.sh --verbose=true -u jdbc:ignite:thin://127.0.0.1/`

Create the Media Store schema and load the cluster with sample data by running the following command from the SQLLine
connection:

`!run /opt/ignite/ext-config/media_store.sql`

Close connection to the container following these steps:

* Close the SQLLine connection: `!q`
* Quit the container use `Ctr+C` sequence.


## Stopping Cluster

Shut down the cluster and remove associated containers: `docker-compose -f docker/ignite-cluster.yaml rm -s -v`

