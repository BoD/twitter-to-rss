A bridge to transform a Twitter list into an RSS feed.

## Docker instructions

### Building and pushing the image to Docker Hub

```
docker image rm bodlulu/twitter-to-rss:latest
DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage
```

### Running the image

```
docker pull bodlulu/twitter-to-rss
docker run -p <PORT TO LISTEN TO>:8080 bodlulu/twitter-to-rss
```
