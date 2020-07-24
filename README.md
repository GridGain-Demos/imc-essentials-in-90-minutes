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

Open your browser and navigate to [http://localhost:8443](http://localhost:8443) to confirm that the tool is up and running.
[Sign up](https://www.gridgain.com/docs/control-center/latest/creating-account) with the tool by creating an account.
(Note, the account is stored in your local Control Center container and not shared with anybody else).

Lastly, you need to interconnect the Ignite cluster with Control Center to get metrics and execute various commands:

* Connect to any of the docker containers running an Ignite cluster node: `docker exec -it ignite-server-node-1 bash`
* Go to the Ignite `bin` folder: `cd apache-ignite/bin/`
* Instruct the cluster to work with your local instance of Control Center: `./management.sh --uri http://control-center-frontend:8443`

Go back to Control Center opened in your browser and [register the cluster](https://www.gridgain.com/docs/control-center/latest/clusters#adding-clusters).
Look for the line as follows in the logs of the cluster nodes that prints out an ID of your cluster:

```
[23:18:10] If you are already using Control Center, you can add the cluster manually by its ID:
[23:18:10]  52b2fb3b-dbb7-45af-add3-ef37eeaf4759
```

## Create Media Store Schema and Load Data

Now you need to create a Media Store schema and load the cluster with sample data (go to step 3 if you are already in the 
`bin` folder of an Ignite container):

1. Connect to any of the docker containers with a command line shell: `docker exec -it ignite-server-node-1 bash`
2. Go to the folder with default Ignite shell scripts: `cd apache-ignite/bin/`
3. Connect to the cluster with SQLLine tool: `./sqlline.sh --verbose=true -u jdbc:ignite:thin://127.0.0.1/`
4. Create the schema and load data: `!run /opt/ignite/ext-config/media_store.sql`

Close connection to the container following these steps:

* Close the SQLLine connection: `!q`
* Type `exit` to exit from `bash`.


## Stopping Cluster

Shut down the cluster and remove associated containers: `docker-compose -f docker/ignite-cluster.yaml rm -s -v`

