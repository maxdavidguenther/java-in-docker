package de.mdguenther.gradle.javaindocker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;

public abstract class JavaInDockerTask extends Exec {

  @Optional
  @Input()
  public abstract Property<String> getDockerComposeFile();

  @Input()
  public abstract Property<String> getServiceName();

  @Input()
  public abstract Property<String> getContainerName();

  @Optional
  @Input()
  public abstract ListProperty<String> getAdditionalDockerRunArgs();

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

    args.addAll(getAdditionalDockerRunArgs().get());

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
