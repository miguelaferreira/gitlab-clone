module gitlab.clone {
    requires static lombok;
    requires logback.classic;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires io.vavr;
    requires io.micronaut.http_client;
    requires io.micronaut.runtime;
    requires io.micronaut.inject;
    requires io.micronaut.http;
    requires io.micronaut.http_client_core;
    requires io.reactivex.rxjava2;
    requires javax.inject;
    requires info.picocli;
    requires org.eclipse.jgit;
    // merged with org.eclipse.jgit to prevent split package
    // requires org.eclipse.jgit.ssh.jsch;
    requires jsch;
    requires io.micronaut.core;

    exports gitlab.clone;
}
