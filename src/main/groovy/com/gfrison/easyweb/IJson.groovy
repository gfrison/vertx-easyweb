package com.gfrison.easyweb

import groovy.json.JsonBuilder

/**
 * User: gfrison
 */
public interface IJson extends IUtil {


    def notFound = { req ->
        req.response.statusCode = 404
        req.response.putHeader 'Content-Type', 'application/json'
        req.response.end '{}'
    }

    def denied = { req ->
        req.response.statusCode = 403
        req.response.putHeader 'Content-Type', 'application/json'
        req.response.end '{}'
    }

    def ok = { req ->
        req.response.statusCode = 200
        req.response.putHeader 'Content-Type', 'application/json'
        req.response.end '{}'
    }

    def conflict = { req ->
        req.response.statusCode = 409
        req.response.putHeader 'Content-Type', 'application/json'
        req.response.end '{}'
    }


    def printJson = { map, def req ->
        req.response.putHeader 'Content-Type', 'application/json'
        def b = new JsonBuilder()
        b(map)
        req.response.end b.toString()
    }

}