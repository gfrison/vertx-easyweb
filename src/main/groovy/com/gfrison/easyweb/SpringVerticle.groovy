package com.gfrison.easyweb

import grails.spring.BeanBuilder
import org.springframework.context.support.AbstractApplicationContext
import org.vertx.groovy.platform.Verticle

/**
 * Dependency Injenction facility through SpringFramework.
 *
 * User: gfrison
 */
abstract class SpringVerticle extends Verticle {
    BeanBuilder bb
    def log
    def beansFileName = "classpath:beans.groovy"
    AbstractApplicationContext context

    def start() {
        log = container.logger
        try {
            bb = new grails.spring.BeanBuilder()
            def binding = new Binding()
            binding.vertx = vertx
            binding.config = container.config
            binding.container = container
            binding.log = container.logger
            bb.setBinding(binding)
            bb.setClassLoader(this.getClass().getClassLoader())
            bb.loadBeans(beansFileName)
            bb.activate()
            context = bb.createApplicationContext()

            context.registerShutdownHook()
            init()
            log.info "bootstrap completed"

        } catch (Throwable e) {
            log.error "error during startup", e
            if (context) {
                context.close()
            }
            container.exit()
            throw e
        }
    }

    def propertyMissing(String name) {
        log.debug("property missing:${name}")
        return context.getBean(name)
    }


    abstract void init()

    def stop() {
        if (context) {
            context.close()
        }

    }

}
