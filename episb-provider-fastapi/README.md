# FastAPI implementation of episb-provider #

The same APi calls implemented in Scalatra/elastic are now implemented in [FastAPI](https://fastapi.tiangolo.com/) and [PostgreSQL](https://www.postgresql.org/).

We run everything in Docker but if you have a local PostgreSQL instance, you can just run it locally (just load the data as below). The episb-provider can be run in test mode buy doing the following:

``
git clone https://github.com/databio/episb-provider
cd episb-provider/episb-provider-fastapi/app
uvicorn main:app --reload
``

You will need Python 3.6 (at least), and uvicorn, fastapi, aiofiles and psycopg2 Python modules (available via Pip). Otherwise, see below on how to run things in Docker.

## Run PostgreSQL in Docker ##

```
docker run --rm --name pg-docker -e POSTGRES_PASSWORD=episb123 -d -p 5432:5432 -v /mnt/elastic/postgres-data:/var/lib/postgresql/data -v /var/run/postgresql:/var/run/postgresql -v /tmp:/tmp postgres
```

The password for the postgres database above is "episb123", it is running on standard postgres port 5432 and it is mounting a few local directories via Docker bind. These may or may not be necessary for your particular configuration. For the most part you may be able to "ditch" the use of the shared directories and just run

```
docker run --rm --name pg-docker -e POSTGRES_PASSWORD=episb123 -d -p 5432:5432 postgres
```

## Populate the Postgres database ##

Make sure you have a local installation of PostgreSQL on your machine, particularly the postgresql-client software (which gives you the "psql" executable). Then,

```
git clone https://github.com/databio/episb-provider
cd episb-provider
git checkout dev
cd psql
chmod u+x load_all.sh
wget http://big.databio.org/papers/RED/supplement/dhs112_v3.bed.gz
gunzip dhs112_v3.bed.gz
psql -h localhost -U postgres -d episb -a -f episb_setup.psql
python load_segments.py dhs112_v3.bed DHS
./load_all.sh dhs112_v3.bed DHS
```

## Run Episb-provider in Docker ##


```
cd episb-provider/episb-provider-fastapi
docker build -t provider-fastapi .
docker run --net host -p 8000:8000 -e PORT="8000" -e GUNICORN_CONF="/app/gunicorn_conf.py" -m=24g -v /var/run/postgresql:/var/run/postgresql -d provider-fastapi
```

We are sharing the same directories via Docker with the PostgreSQL docker container in the above instance. You may be just able to run:

```
docker run --net host -p 8000:8000 -e PORT="8000" -e GUNICORN_CONF="/app/gunicorn_conf.py" -m=24g -d provider-fastapi
```

After this, you can issue calls such as:
```
curl -X GET "http://localhost:8000/experiments/get/BySegmentationName/:segName?segName=DHS" -H  "accept: application/json"
```

Auto-generated (via Swagger) documentation is at [http://localhost:8000/docs](http://localhost:8000/docs)
