package com.gfrison.easyweb

import groovy.json.JsonSlurper
import org.vertx.groovy.core.http.RouteMatcher

/**
 * User: gfrison
 */
class JsonMatcher {

    def log

    @Delegate
    RouteMatcher route = new RouteMatcher()
    def parser = new JsonSlurper();

    private void body(req, handler) {
        if (req.headers['Content-Type'] =~ /application/) {
            req.bodyHandler { body ->
                try {
                    req.metaClass.body = parser.parseText(body.toString())
                } catch (Exception e) {
                    log.warn('error parsing request json:' + e.message + ', message:' + body.toString())
                    req.metaClass.body = [:]
                }
                handler(req)
            }
        } else {
            handler(req)
        }

    }

    public void post(String pattern, Closure handler) {
        route.post(pattern, { req ->
            body(req, handler)
        })
    }

    public void put(String pattern, Closure handler) {
        route.put(pattern, { req ->
            body(req, handler)
        })
    }

    public void patch(String pattern, Closure handler) {
        route.patch(pattern, { req ->
            body(req, handler)
        })
    }

    public void get(String pattern, Closure handler) {
        route.get(pattern, { req ->
            www(req, handler)
        })
    }

    public void noMatch(Closure handler) {
        route.noMatch { req ->
            www(req, handler)
        }
    }

    private void www(req, Closure handler) {
        def url = req.headers['Host']
        if (url ==~ /(https?:\/\/)?www.*/) {
            log.debug 'uri con www:' + url
            req.response.statusCode = 301
            req.response.putHeader 'Location', 'http://' + url.replaceFirst('www.', '') + req.uri
            req.response.end()
        } else {
            handler(req)
        }
    }
}
