package de.mdguenther.gradle.javaindocker;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * All configuration properties for Java in Docker.
 */
public interface JavaInDockerExtension {

  /**
   * The name of the Docker service of the application in the {@code docker-compose.yml} that is to
   * be replaced with the execution of the application run by {@code runInDocker}.
   *
   * @return the name of the service
   */
  @Input()
  Property<String> getServiceName();

  /**
   * The path to the {@code docker-compose.yml} file relative to the project path that contains
   * the application setup. <em>Default:</em> {@code docker-compose.yml}
   *
   * @return the path to the {@code docker-compose.yml}.
   */
  @Optional
  @Input()
  Property<String> getDockerComposeFile();

  /**
   * The name of the container that is removed prior to {@code dockerInRun} by the
   * {@link RemoveContainerTask} and then consequently given to the container run by
   * {@code runInDocker}.
   *
   * @return the container name
   */
  @Optional
  @Input()
  Property<String> getContainerName();

  /**
   * Any additional arguments that should be passed onto {@code docker compose run}.
   *
   * @return the additional {@code docker compose run} arguments
   */
  @Optional
  @Input()
  ListProperty<String> getAdditionalDockerRunArgs();

  /**
   * Any additional args that should be passed to the {@code java} executable. Default is
   * {@code -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005} for debugging on
   * port {@code 5005}.
   *
   * @return the additional args
   */
  @Input()
  ListProperty<String> getAdditionalJavaArgs();
}
