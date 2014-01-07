package com.gfrison.easyweb

import groovy.util.logging.Slf4j
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.platform.Container
import org.vertx.java.core.logging.Logger
import org.vertx.java.core.logging.impl.SLF4JLogDelegate
import spock.lang.Specification

/**
 * User: gfrison
 */
@Slf4j
class SpringVerticleTest extends Specification {

    def verticle

    def setup() {
        def container = Mock(Container)
        container.logger >> new Logger(new SLF4JLogDelegate('test'))
        verticle = new TestVerticle(log: log, vertx: Vertx.newVertx(), container: container)
    }

    def cleanup() {

    }

    void 'test start'() {
        when: 'start'
        verticle.start()

        then: 'ok'
    }

    class TestVerticle extends SpringVerticle {

        @Override
        void init() {
            session.ifSession(Mock(HttpServerRequest)) { session ->
                log.info 'ok'
            }

        }
    }
}
