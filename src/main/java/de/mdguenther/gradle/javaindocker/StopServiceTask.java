package de.mdguenther.gradle.javaindocker;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;

/**
 * Service that stops a service defined by a {@code docker-compose.yml}.
 */
public abstract class StopServiceTask extends Exec {

  /**
   * The path to the {@code docker-compose.yml} relative to the project directory.
   *
   * @return the name of the {@code docker-compose.yml}
   */
  @Input()
  public abstract Property<String> getDockerComposeFile();

  /**
   * The name of the service to stop as defined in the {@code docker-compose.yml}.
   *
   * @return the name of the service
   */
  @Input()
  public abstract Property<String> getServiceName();

  /**
   * Create the task.
   */
  @Inject
  public StopServiceTask() {}

  @Override
  public void exec() {
    final List<String> args = new ArrayList<>();
    args.add("compose");
    if (getDockerComposeFile().isPresent()) {
      args.add("-f");
      args.add(getDockerComposeFile().get());
    }

    args.add("stop");
    args.add(getServiceName().get());

    setCommandLine("docker");
    setArgs(args);

    getLogger().info("Commandline: " + getCommandLine());

    final ByteArrayOutputStream captureErrorOutput = new ByteArrayOutputStream();
    setErrorOutput(captureErrorOutput);
    final ByteArrayOutputStream captureStandardOutput = new ByteArrayOutputStream();
    setStandardOutput(captureStandardOutput);

    super.exec();

    if (getExecutionResult().get().getExitValue() != 0 || !getLogger().isQuietEnabled()) {
      final Charset consoleCharset = System.console() != null && System.console().charset() != null
        ? System.console().charset()
        : StandardCharsets.UTF_8;
      final String capturedErrorOutput = captureErrorOutput.toString(consoleCharset);
      final String capturedStandardOutput = captureStandardOutput.toString(consoleCharset);
      getLogger().info(capturedStandardOutput);
      getLogger().error(capturedErrorOutput);
    }
  }
}
