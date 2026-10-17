#!/bin/bash
#Arranca rabbit2 y lo une al clúster de rabbit1.
#Es seguro ejecutarlo en cada arranque: si ya es miembro, no hace nada.

#Arranca RabbitMQ con el entrypoint oficial de la imagen (escribe la cookie de
#Erlang, crea el usuario por defecto, etc.) y lo deja corriendo en segundo plano.
docker-entrypoint.sh rabbitmq-server &
SERVER_PID=$!

#Al detener el contenedor, apaga RabbitMQ limpiamente.
trap 'kill -TERM "$SERVER_PID" 2>/dev/null; wait "$SERVER_PID"; exit 0' TERM INT

#rabbitmqctl se ejecuta como el usuario "rabbitmq" (el dueño de la cookie).
ctl() { gosu rabbitmq rabbitmqctl "$@"; }

#Espera a que el nodo local esté listo.
for i in $(seq 1 60); do
  ctl -t 10 await_startup >/dev/null 2>&1 && break
  sleep 2
done

#Se une solo si todavía no pertenece al clúster.
if ctl cluster_status 2>/dev/null | grep -q "rabbit@rabbit1"; then
  echo "[join-cluster] rabbit2 ya es parte del clúster."
else
  echo "[join-cluster] Uniendo rabbit2 al clúster de rabbit1..."
  ctl stop_app
  ctl reset
  ctl join_cluster rabbit@rabbit1
  ctl start_app
  echo "[join-cluster] Listo."
fi

#Mantiene el contenedor vivo mientras RabbitMQ corra.
wait "$SERVER_PID"
