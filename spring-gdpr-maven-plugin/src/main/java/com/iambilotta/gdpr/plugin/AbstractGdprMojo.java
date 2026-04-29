package com.iambilotta.gdpr.plugin;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Shared parameter base for the standalone DPIA + ROPA mojos. The processor writes both
 * artifacts in the same directory, so each mojo only verifies and surfaces one of them.
 *
 * <p>The actual generation runs as part of {@code mvn compile} via the APT processor; these
 * mojos are convenience wrappers that locate the latest output and copy it to a stable path
 * so CI jobs can attach the file to a release artifact or a regulator submission package.
 */
public abstract class AbstractGdprMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/annotations/spring/gdpr", readonly = true)
    protected File processorOutputDir;

    @Parameter(defaultValue = "${project.build.directory}/spring-gdpr", readonly = true)
    protected File publishDir;

    protected File requireFile(String name) throws MojoExecutionException {
        File file = new File(processorOutputDir, name);
        if (!file.isFile()) {
            throw new MojoExecutionException(
                    "spring-gdpr: expected " + name + " at " + file.getAbsolutePath()
                            + ". Did the annotation processor run? "
                            + "Add spring-gdpr-processor as an annotationProcessorPath of maven-compiler-plugin.");
        }
        return file;
    }
}
