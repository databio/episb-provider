# Episb api

This folder contains code for an API using fastAPI, a python web framework for asynchronous web APIs. It will autodocument the API using openAPI standards.

## Building container

1. In the same directory as the `Dockerfile`:

```
docker build -t fastapi .
```

## Running container for development:

```
docker run --rm -d -p 80:80 --name fastapi -v $(pwd):/app fastapi /start-reload.sh
```

Terminate container when finished:

```
docker stop fastapi
```

## Running container for production:

2. Run the container from the image you just built:

```
docker run -d -p 80:80 --rm --name fastapi fastapi
```


## Interacting with the API web server

Navigate to [http://localhost/](http://localhost/) to see the server in action.

You can see the automatic docs and interactive swagger openAPI interface at [http://localhost/docs](http://localhost/docs). That will also tell you all the endpoints, etc.


## Monitoring for errors

Attach to container to see debug output:
```
docker attach fastapi
```

Grab errors:

```
docker events | grep -oP "(?<=die )[^ ]+"
```

View those error codes:

```
docker logs <error_code>
```

