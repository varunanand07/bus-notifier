. .env
docker run --rm -it \
       -p 54321:5432 \
       -v $(pwd)/transposed:/transposed \
       -e POSTGRES_USER=spring-boot \
       -e POSTGRES_PASSWORD=spring-boot \
       -e POSTGRES_DB=bus_notifier \
       postgres
