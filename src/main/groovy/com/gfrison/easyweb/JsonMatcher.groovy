package com.gfrison.easyweb

import groovy.json.JsonSlurper
import org.vertx.groovy.core.http.RouteMatcher

/**
 * RouteMarcher extension for automatically parsing and injecting json object in case of
 * 'application/json' content-type.
 * The json object might be found on the brand-new 'body' property of request.
 *
 * It's possible to handle all request starting with 'www.' and redirecting them to the domain
 * with the property conf.removeWww = true
 *
 * User: gfrison
 */
class JsonMatcher {

    def log
    def conf

    @Delegate
    RouteMatcher route = new RouteMatcher()
    def parser = new JsonSlurper();

    private void body(req, handler) {
        if (req.headers['Content-Type'] =~ /application\/json/) {
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
            if (conf?.removeWww) {
                www(req, handler)
            } else {
                handler(req)
            }
        })
    }

    public void noMatch(Closure handler) {
        route.noMatch { req ->
            if (!conf?.removeWww) {
                www(req, handler)
            } else {
                handler(req)
            }
        }
    }

    /**
     * redirect all GET requests to the base domain without 'www'
     */
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
