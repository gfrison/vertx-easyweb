package com.gfrison.easyweb

import com.gmongo.GMongo
import com.mongodb.ServerAddress

import javax.annotation.PostConstruct

/**
 * User: gfrison
 */
class EnvironmentVersioning {
    def log
    def db
    def container
    def env
    def mongoConf


    @PostConstruct
    def updateVersion() {
        def mongo
        if (mongoConf.host) {
            mongo = new GMongo(new ServerAddress(mongoConf.host, mongoConf.port))
        } else {
            mongo = new GMongo()
        }
        db = mongo.getDB(mongoConf.db_name)
        if (mongoConf.username) {
            db.authenticate(mongoConf.username, mongoConf.password?.toCharArray())
        }
        def that = this
        def versioning = db.versioning.findOne([_id: 1])

        def version = versioning ? versioning.version : 0
        def closurePropNames = properties.findResults { name, value ->
            (value instanceof Closure && name.startsWith(env)) ? name.substring(env.length()) : null
        }.sort()
        closurePropNames.each {
            if (it > version) {
                log.info("upgrading db version to:${it}")
                that."${env}${it}"()
                db.versioning.update([_id: 1], [_id: 1, version: it], true)

            }
        }


    }

}
