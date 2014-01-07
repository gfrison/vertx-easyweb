package com.gfrison.easyweb

import grails.spring.BeanBuilder
import org.springframework.context.support.AbstractApplicationContext
import org.vertx.groovy.platform.Verticle

/**
 * User: gfrison
 */
abstract class SpringVerticle extends Verticle {
    BeanBuilder bb
    def log
    AbstractApplicationContext context

    def start() {
        log = container.logger
        try {
            String appname = System.getProperty("app.name") ?: '', appversion = System.getProperty("app.version") ?: ''
            bb = new grails.spring.BeanBuilder()
            def binding = new Binding()
            binding.vertx = vertx
            binding.config = container.config
            binding.container = container
            binding.log = container.logger
            bb.setBinding(binding)
            bb.setClassLoader(this.getClass().getClassLoader())
            bb.loadBeans("classpath:beans.groovy")
            bb.activate()
            context = bb.createApplicationContext()

            context.registerShutdownHook()
            init()
            log.info 'environment:' + System.getProperty('environment') ?: 'development'
            log.info "init completed"

        } catch (Throwable e) {
            log.error "error during startup", e
            if (context) {
                context.close()
            }
            container.exit()
        }
    }

    def propertyMissing(String name) {
        return context.getBean(name)
    }


    abstract void init()

    def stop() {
        if (context) {
            context.close()
        }

    }

}
