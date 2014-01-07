package com.gfrison.easyweb

import org.vertx.groovy.core.http.HttpServerRequest

/**
 * User: gfrison
 */
public class Session implements IUtil {

    def log
    @Delegate
    private DB dbutil

    public void setDbutil(dbutil) {
        this.dbutil = dbutil
    }


    @Lazy
    TimerMap sessions = new TimerMap(timeout: container.config.sessionTimeout, vertx: vertx)
    def container
    def vertx


    def getSessionId = { HttpServerRequest req ->
        def map = req.headers['Cookie']?.split(';').inject([:]) { map, token ->
            token.trim().split('=').with {
                try {
                    map[it[0]] = it[1]

                } catch (Exception e) {
                    map[it[0]] = ''
                }
            }
            map
        }

        map.get('JSESSIONID')
    }

    def createNewSession = { req, after ->
        save('session', [:]) { cookie ->
            def session = [:]
            session._id = cookie
            req.response.putHeader("Set-Cookie", "JSESSIONID=" + cookie + '; Expires=Thu, 31 Dec 2020 21:47:38 GMT; path=/; ')
            sessions[cookie] = session
            after(session)
        }
    }

    def ifSession = { req, after ->
        def sessionId = getSessionId(req)
        if (!sessionId) {
            after(null)
            return
        }
        def session = sessions[sessionId]
        if (session) {
            after(session)
        } else {
            find1('session', ['_id': sessionId]) { storeSession ->
                if (storeSession) {
                    //session exists
                    //log.info 'startSession found session:' + storeSession
                    session = storeSession
                    sessions[sessionId] = session
                    if (storeSession.uid) {
                        find1('user', ['_id': storeSession.uid]) { user ->
                            if (user) {
                                session['user'] = user
                                after(session)

                            } else {
                                log.warn('not found uid:{} for sessionId:{}', storeSession.uid, sessionId)
                                after(session)
                            }
                        }
                    } else {
                        log.debug('no user defined for session:{}', sessionId)
                        after(session)
                    }
                } else {
                    log.debug('no stored session for sessionId:{}, pass null', sessionId)
                    after(null)
                    return
                }
            }
        }
    }


    def startSession = { req, after ->
        def sessionId = getSessionId(req)
        if (!sessionId) {
            log.debug('creo nuova session ')
            this.createNewSession(req, after)
            return
        }
        if (!sessions[sessionId]) {
            find1('session', ['_id': sessionId]) { storeSession ->
                if (storeSession) {
                    //session exists
                    //log.info 'startSession found session:' + storeSession
                    def session = storeSession
                    sessions[sessionId] = session
                    if (storeSession.uid) {
                        find1('user', ['_id': storeSession.uid]) { user ->
                            if (user) {
                                session['user'] = user
                                after(session)

                            } else {
                                log.warn('not found uid:{} for sessionId:{}', storeSession.uid, sessionId)
                                after(session)
                            }
                        }
                    } else {
                        log.debug('no user defined for session:{}', sessionId)
                        after(session)
                    }
                } else {
                    log.debug('no stored session for sessionId:{}, create new session', sessionId)
                    createNewSession(req, after)
                    return
                }
            }
        } else {
            def session = sessions[sessionId]
            log.debug "startSession - session present:${session}"
            after(session)
        }
    }

    def requireSession = { req, after ->
        def sessionId = getSessionId(req)
        if (!sessionId) {
            log.debug("non c'è sessionId:{}", sessionId)
            req.response.statusCode = 401
            req.response.end()
            return
        }
        def session = sessions[sessionId]
        if (!session || !session.user) {
            find1('session', ['_id': sessionId]) { dbsession ->
                if (!dbsession) {
                    log.debug("non c'è session in db. sessionId:{}", sessionId)
                    req.response.statusCode = 401
                    req.response.end()
                    return
                }
                if (dbsession.uid) {
                    find1('user', ['_id': dbsession.uid]) { user ->
                        if (!user) {
                            log.debug("non c'è utente. sessionId:{}, uid:{}", sessionId, dbsession.uid)
                            req.response.statusCode = 401
                            req.response.end()
                            return
                        }
                        dbsession.user = user
                        sessions[sessionId] = dbsession
                        if (!user.email) {
                            log.debug('user non registrato (no email), sessionid:{}, uid:{}', sessionId, user._id)
                            req.response.statusCode = 401
                            req.response.end()
                            return
                        }
                        after(sessions[sessionId])
                    }
                } else {
                    log.debug("non c'è uid sessionId:{}", sessionId)
                    req.response.statusCode = 401
                    req.response.end()
                }
            }
        } else {
            if (!session?.user?.email) {
                log.debug("session memory presente ma senza email sessionId:{}", sessionId)
                req.response.statusCode = 401
                req.response.end()
                return
            }
            after(session)
        }
    }


}