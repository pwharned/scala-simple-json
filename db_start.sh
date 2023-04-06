podman machine set --rootful=true
podman ps --all | grep db2  | awk '{ print $1}' | xargs -I {} podman rm {}

podman pod create --name=db2 -p 50000:50000
podman run --pod=db2 -itd --name mydb2 --privileged=true  -e LICENSE=accept -e DB2INST1_PASSWORD=password -e DBNAME=bludb ibmcom/db2