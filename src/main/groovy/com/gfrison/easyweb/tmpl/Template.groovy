package com.gfrison.easyweb.tmpl

import com.gfrison.GTemplateEngine
import com.gfrison.easyweb.StaticResources
import groovy.io.FileType
import groovy.text.SimpleTemplateEngine

import javax.annotation.PostConstruct

/**
 * User: gfrison
 */
class Template {

    def container
    def vertx
    def log
    protected def tmpls = [:]
    def cache

    StaticResources aStatic

    @PostConstruct
    def loadTemplates() {
        log.info('init tmpl')
        tmpls = [:]
        cache = container.config.tmpl.cache
        log.info('tmpl cache:' + cache)
        getClass().getClassLoader().getResources('tmpl').each {
            File f = new File(it.toURI())
            f.eachFileRecurse(FileType.FILES) { file ->
                def name = file.path.substring(it.file.indexOf('tmpl') + 5)
                log.info("tmpl name: ${name}")
                def engine = new SimpleTemplateEngine()
                tmpls[name] = engine.createTemplate(file.text)
            }
        }
    }


    def tmpl = { String name, binding = null ->
        groovy.text.Template template
        if (name.startsWith('static:')) {
            template = tmplStatic(name.substring(7))
        } else {
            template = tmplResources(name)
        }
        if (!template) {
            return ''
        }
        return template.make(binding ?: [:]).toString()
    }

    def tmplStatic = { name ->
        log.debug("tmplStatic name:${name}")
        StaticResources.FileCache filecache = aStatic.fileCache(name)
        if (!filecache) {
            log.warn('file non trovato in static:' + name);
            return null
        }
        if (!tmpls['static:' + name] || !cache) {
            log.debug("template static:${name} non in cache, creo istanza")
            def engine = new GTemplateEngine()
            tmpls['static:' + name] = engine.createTemplate(new String(filecache.content))
        }
        return tmpls['static:' + name]
    }

    def tmplResources = { name ->
        def ret
        if (cache) {
            ret = tmpls[name]
        } else {
            getClass().getClassLoader().getResources('tmpl').each {
                if (!it.path.contains('~')) {
                    log.info "load path: ${it.path + '/' + name}"
                    File f = new File(it.path + '/' + name)
                    def engine = new SimpleTemplateEngine()
                    ret = engine.createTemplate(f.text)
                }
            }
        }
        if (!ret) {
            log.error('not found:{}', name)
        }
        return ret

    }
}

