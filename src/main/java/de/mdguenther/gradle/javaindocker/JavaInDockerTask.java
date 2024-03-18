package de.mdguenther.gradle.javaindocker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;

/**
 * Runs Java in a Docker Compose container via {@code docker compose run}.
 */
public abstract class JavaInDockerTask extends Exec {

  /**
   * The path to the {@code docker-compose.yml} relative to the project path.
   *
   * @return the path to {@code docker-compose.yml}
   */
  @Optional
  @Input()
  public abstract Property<String> getDockerComposeFile();

  /**
   * The name of the application service in the {@code docker-compose.yml} that is to be replaced by
   * Java in Docker.
   *
   * @return the name of the application service
   */
  @Input()
  public abstract Property<String> getServiceName();

  /**
   * The name of the Docker container in the {@code docker-compose.yml} that is given to the
   * container.
   *
   * @return the name of the docker container
   */
  @Input()
  public abstract Property<String> getContainerName();

  /**
   * Any additional arguments that to be supplied to {@code docker compose run}.
   *
   * @return the additional {@code docker compose run} arguments
   */
  @Optional
  @Input()
  public abstract ListProperty<String> getAdditionalDockerRunArgs();

  /**
   * The name of the main class that is run {@code java -cp <classpath> <mainClassName>}. Must be
   * a fully qualified name of the class, of course!
   *
   * @return the name of the main class.
   */
  @Input()
  public abstract Property<String> getMainClassName();

  /**
   * The path of the directory inside the Docker container where the Gradle build directory
   * is mounted to as a volume. <em>Default:</em> {@code /gradle}
   *
   * @return the path to the build directory inside the Docker container
   */
  @Input()
  public abstract Property<String> getContainerBuildDir();

  /**
   * The path of the directory inside the Docker container where the Gradle user home directory
   * is mounted to as a volume. <em>Default:</em> {@code /build}
   *
   * @return the path to the build directory inside the Docker container
   */
  @Input()
  public abstract Property<String> getContainerGradleUserHome();

  /**
   * Any additional args that should be passed to the {@code java} executable.
   *
   * @return the additional args
   */
  @Input()
  public abstract ListProperty<String> getAdditionalJavaArgs();

  /**
   * Create the task.
   */
  @Inject
  public JavaInDockerTask() { }

  @Override
  public void exec() {
    final List<String> args = new ArrayList<>();
    args.add("compose");
    if (getDockerComposeFile().isPresent()) {
      args.add("-f");
      args.add(getDockerComposeFile().get());
    }

    args.add("run");
    args.add("--rm");

    args.add("--name");
    args.add(getContainerName().get());

    args.addAll(getAdditionalDockerRunArgs().get());

    final String buildDir = getProject().getLayout().getBuildDirectory().get().toString();
    args.add("-v");
    args.add("%s:%s".formatted(buildDir, getContainerBuildDir().get()));

    final String gradleHome = getProject().getGradle().getGradleUserHomeDir().toString();
    args.add("-v");
    args.add("%s:%s".formatted(gradleHome, getContainerGradleUserHome().get()));

    args.add(getServiceName().get());

    args.add("java");
    args.addAll(getAdditionalJavaArgs().get());
    args.add("-cp");

    final String gradleDir = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath();

    final JavaPluginExtension javaExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
    final SourceSet mainSourceSet = javaExtension.getSourceSets().stream()
        .filter(SourceSet::isMain).findAny()
        .orElseThrow(() -> new JavaInDockerException("main source set is missing"));
    final Path buildPath = getProject().getLayout().getBuildDirectory().getLocationOnly()
      .map(d -> d.getAsFile().toPath()).get();
    final Stream<Path> classesPaths = mainSourceSet.getOutput().getClassesDirs().getFiles().stream()
      .map(f -> buildPath.relativize(f.toPath()))
      .map(p -> Paths.get(getContainerBuildDir().get()).resolve(p));
    final Stream<Path> resourcePaths = java.util.Optional.ofNullable(
      mainSourceSet.getOutput().getResourcesDir()
    ).stream()
      .map(f -> buildPath.relativize(f.toPath()))
      .map(p -> Paths.get(getContainerBuildDir().get()).resolve(p));

    final String classpathArg = Stream.concat(
      Stream.concat(
        classesPaths.map(Path::toString),
        resourcePaths.map(Path::toString)
      ),
      getProject().getConfigurations()
        .getByName("runtimeClasspath")
        .getFiles()
        .stream()
        .map(path -> {
          if (path.getAbsolutePath().startsWith(gradleDir)) {
            return Paths.get(getContainerGradleUserHome().get(),
              path.getAbsolutePath().substring(gradleDir.length())
            ).toString();
          } else {
            getLogger().warn("cannot map classpath for: {}", path);
            return null;
          }
        })
        .filter(Objects::nonNull)
    ).collect(Collectors.joining(":"));
    args.add(classpathArg);

    args.add(getMainClassName().get());

    setCommandLine("docker");
    setArgs(args);
    super.exec();
  }
}
