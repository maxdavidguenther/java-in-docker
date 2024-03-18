package de.mdguenther.gradle.javaindocker;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Provides the Java in Docker Gradle plugin.
 *
 * <p>
 *   Three tasks are supplied:
 *   <ul>
 *     <li>{@code removeContainer} - Removes the container named
 *     {@code javaInDocker.containerName}</li>
 *     <li>{@code stopService} - Stops the docker compose service with the name
 *     {@code javaInDocker.serviceName}</li>
 *     <li>{@code runInDocker} - Executes the above two tasks and then runs the application
 *     identified by {@code mainClassName} or by the file {@code resolvedMainClassName} in the
 *     Gradle build directory (supplied by the Gradle plugin {@code org.springframework.boot}).</li>
 *   </ul>
 */
public class JavaInDockerPlugin implements Plugin<Project> {
  private static final String TASK_GROUP = "java in docker";
  private static final String TASK_REMOVE_CONTAINER = "removeContainer";
  private static final String TASK_STOP_SERVICE = "stopService";
  private static final String TASK_RUN_IN_DOCKER = "runInDocker";
  private static final String TASK_COMPILE_JAVA = "compileJava";
  private static final String TASK_PROCESS_RESOURCES = "processResources";

  /**
   * Create the plugin.
   */
  public JavaInDockerPlugin() { }

  @Override
  public void apply(final Project target) {
    final JavaInDockerExtension extension = target.getExtensions()
      .create("javaInDocker", JavaInDockerExtension.class);
    extension.getDockerComposeFile().convention("docker-compose.yml");
    extension.getAdditionalDockerRunArgs().convention(List.of("--service-ports"));
    if (!extension.getContainerName().isPresent()) {
      extension.getContainerName().set(extension.getServiceName());
    }

    target.getTasks().register(TASK_REMOVE_CONTAINER,
        RemoveContainerTask.class).configure(task -> {
      task.setGroup(TASK_GROUP);
      task.getContainerName().set(extension.getContainerName());
    });

    target.getTasks().register(TASK_STOP_SERVICE,
        StopServiceTask.class).configure(task -> {
      task.setGroup(TASK_GROUP);
      task.getDockerComposeFile().set(extension.getDockerComposeFile());
      task.getServiceName().set(extension.getServiceName());
    });

    target.getTasks().register(TASK_RUN_IN_DOCKER, JavaInDockerTask.class).configure(task -> {
      task.setGroup(TASK_GROUP);
      task.dependsOn(
        target.getTasks().getByName(TASK_COMPILE_JAVA),
        target.getTasks().getByName(TASK_PROCESS_RESOURCES),
        target.getTasks().getByName(TASK_REMOVE_CONTAINER),
        target.getTasks().getByName(TASK_STOP_SERVICE)
      );
      task.getDockerComposeFile().set(extension.getDockerComposeFile());
      task.getServiceName().set(extension.getServiceName());
      task.getContainerName().set(extension.getContainerName());
      task.getAdditionalDockerRunArgs().set(extension.getAdditionalDockerRunArgs());
      task.getContainerBuildDir().convention("/build");
      task.getContainerGradleUserHome().convention("/gradle");

      if (!task.getMainClassName().isPresent()) {
        final String mainClassName = tryDetectMainClassName(target);
        if (mainClassName != null) {
          task.getMainClassName().set(mainClassName);
        }
      }
    });
  }

  private String tryDetectMainClassName(final Project target) {
    final var resolvedMainClassNameFile = target.getLayout().getBuildDirectory().file("resolvedMainClassName");
    if (resolvedMainClassNameFile.isPresent()) {
      try {
        return Files.readString(resolvedMainClassNameFile.get().getAsFile().toPath()).trim();
      } catch (IOException ex) {
        final String message = "failed to read %s".formatted(
          resolvedMainClassNameFile.get().getAsFile());
        throw new JavaInDockerException(message, ex);
      }
    } else {
      return (String) target.getProperties().get("mainClassName");
    }
  }
}
