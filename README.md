
Run a Java Project directly in a docker compose context. This is intended to be used in conjunction with `gradle build --continuously` and the Spring Boot Developer Tools, resulting in a smooth developer experience where the application is continuously built and reloaded.

### Minimal Configuration

`docker-compose.yml`:
```yml ()
services:
  my-application:
    image: 'my-application' # or through build
    ports:
      - 8080:8080
      - 5005:5005
```
Of course, you will likely have many more services, like your database. These all can still communicate with the application being run via `gradle runInDocker`.

`build.gradle`:
```gradle

plugins {
  id 'java'
  id 'de.mdguenther.gradle.java-in-docker' version '0.0.1'
}

javaInDocker {
  serviceName = 'my-application'
}
```
Only the plugin itself and `javaInDocker.serviceName` need to be configured, but more options are available (see below).

### Running
Run in separate terminals (or via your favorite IDE):
 * `./gradlew build --continuously`
 * `./gradlew runInDocker`

Once you are done developing you can simply:
 * `./gradlew stopService`
 * `docker compose up -d my-application`

### Configuration
 * `javaInDocker.serviceName` the name of the service in the `docker-compose.yml` that is to be replaced.
 * `javaInDocker.dockerComposeFile` (default: `docker-compose.yml`) the path to the docker compose file relative to the project directory.
 * `javaInDocker.containerName` (default: `${javaInDocker.serviceName}`) the name of the container. Any containers with this name are removed before the `javaInDocker` task is run.
 * `javaInDocker.additionalDockerRunArgs` (default: `--service-ports`) additional arguments passed via `docker compose run`.

All in all the commandline executed by the task `runInDocker` should look like this:
```shell
docker compose run \
  --rm \
  ${javaInDocker.additionalDockerRunArgs} \
  -v "build:/build" \
  -v "~/.gradle:/gradle" \
  run "${javaInDocker.serviceName}" \
  java \
  -cp "/build/classes/java:/build/resources:${classpath}" \
  "${mainClassName}"
```
