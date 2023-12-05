# Bus Notifier Project

A tool for receiving notifications when your bus is due to arrive soon.

## Setup

VAPID keys for testing purposes are provided in the `.env` file included in the repository.
If using intellij, add these to your run configuration:

![img](images/intellij-1.png)
![img](images/intellij-2.png)

There are many ways to create fresh VAPID keys, such as via [web-push](https://www.npmjs.com/package/web-push) and [webpush-java](https://github.com/web-push-libs/webpush-java).

Similarly, a JWT signing key is provided for testing.
A new key can be generated as follows:

```bash
head -c 256 /dev/random | hexdump -v -e '/1 "%02X"'
```
# Running Postgres

You can either run an instance of postgres locally and manage databases with pgAdmin, or you can use the docker configuration as follows:

```bash
docker run --rm -it \
  -p 54321:5432 \
  -e POSTGRES_USER=spring-boot \
  -e POSTGRES_PASSWORD=spring-boot \
  -e POSTGRES_DB=bus_notifier \
  postgres
```