package de.mdguenther.gradle.javaindocker;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public abstract class JavaInDockerTask extends Exec {

  @Optional
  @Input()
  public abstract Property<String> getDockerComposeFile();

  @Input()
  public abstract Property<String> getServiceName();

  @Input()
  public abstract Property<String> getContainerName();

  @Input()
  public abstract Property<String> getMainClassName();

  @Input()
  public abstract Property<String> getContainerBuildDir();

  @Input()
  public abstract Property<String> getContainerGradleUserHome();

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

    args.add("--service-ports");

    final String buildDir = getProject().getLayout().getBuildDirectory().get().toString();
    args.add("-v");
    args.add("%s:%s".formatted(buildDir, getContainerBuildDir().get()));

    final String gradleHome = getProject().getGradle().getGradleUserHomeDir().toString();
    args.add("-v");
    args.add("%s:%s".formatted(gradleHome, getContainerGradleUserHome().get()));

    args.add(getServiceName().get());

    args.add("java");
    args.add("-cp");

    final String gradleDir = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath();

    final String classpathArg = Stream.concat(
      Stream.of(
        Paths.get(getContainerBuildDir().get(), "classes/java/main").toString(),
        Paths.get(getContainerBuildDir().get(), "resources/main").toString()
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
