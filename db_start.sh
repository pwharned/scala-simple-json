#podman machine set --rootful=trued

podman pod kill db2
podman ps --all | grep db2  | awk '{ print $1}' | xargs -I {} podman rm {}

podman pod create --name=db2 -p 50000:50000
#podman exec -it $(podman ps | grep db2 |awk '{ print $1}') /bin/bash
podman run \
  --pod=db2 -itd \
   --name mydb2 --privileged=true  \
   -e LICENSE=accept \
   -e DB2INST1_PASSWORD=password \
   -e DBNAME=bludb \
   -v /Users/pat/Projects/aletheutes/src/main/resources/db2_init.sh:/var/custom/db2_init.sh \
   ibmcom/db2