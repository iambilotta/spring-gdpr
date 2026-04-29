package com.iambilotta.gdpr.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Copies the generated {@code ropa.csv} to {@code target/spring-gdpr/ropa.csv}.
 */
@Mojo(name = "ropa", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class RopaMojo extends AbstractGdprMojo {

    @Override
    public void execute() throws MojoExecutionException {
        File source = requireFile("ropa.csv");
        if (!publishDir.exists() && !publishDir.mkdirs()) {
            throw new MojoExecutionException("spring-gdpr: cannot create " + publishDir.getAbsolutePath());
        }
        File target = new File(publishDir, "ropa.csv");
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new MojoExecutionException("spring-gdpr: failed to copy ROPA: " + ex.getMessage(), ex);
        }
        getLog().info("spring-gdpr ropa: " + target.getAbsolutePath());
    }
}
