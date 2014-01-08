package com.gfrison.easyweb
/**
 * User: gfrison
 */
public class Session implements IUtil {

    def log
    @Delegate
    private DB dbutil
    def timeout = 1000 * 60 //1min


    @Lazy
    TimerMap sessions = new TimerMap(timeout: timeout, vertx: vertx)
    def vertx


    def getSessionId = { def req ->
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
                    session = storeSession
                    sessions[sessionId] = session
                    if (storeSession.uid) {
                        find1('user', ['_id': storeSession.uid]) { user ->
                            if (user) {
                                session['user'] = user
                                after(session)

                            } else {
                                log.warn("not found uid:${storeSession.uid} for sessionId:${sessionId}")
                                after(session)
                            }
                        }
                    } else {
                        log.debug("no user defined for session:${sessionId}")
                        after(session)
                    }
                } else {
                    log.debug("no stored session for sessionId:${sessionId}, pass null")
                    after(null)
                    return
                }
            }
        }
    }


    def startSession = { req, after ->
        def sessionId = getSessionId(req)
        if (!sessionId) {
            log.debug('creating new session ')
            this.createNewSession(req, after)
            return
        }
        if (!sessions[sessionId]) {
            find1('session', ['_id': sessionId]) { storeSession ->
                if (storeSession) {
                    //session exists
                    def session = storeSession
                    sessions[sessionId] = session
                    if (storeSession.uid) {
                        find1('user', ['_id': storeSession.uid]) { user ->
                            if (user) {
                                session['user'] = user
                                after(session)

                            } else {
                                log.warn("not found uid:${storeSession.uid} for sessionId:${sessionId}")
                                after(session)
                            }
                        }
                    } else {
                        log.debug("no user defined for session:${sessionId}")
                        after(session)
                    }
                } else {
                    log.debug("no stored session for sessionId:${sessionId}, create new session")
                    createNewSession(req, after)
                    return
                }
            }
        } else {
            def session = sessions[sessionId]
            log.debug "startSession - session present"
            after(session)
        }
    }

    def requireSession = { req, after ->
        def sessionId = getSessionId(req)
        if (!sessionId) {
            log.debug("without sessionId:${sessionId}")
            req.response.statusCode = 401
            req.response.end()
            return
        }
        def session = sessions[sessionId]
        if (!session || !session.user) {
            find1('session', ['_id': sessionId]) { dbsession ->
                if (!dbsession) {
                    log.debug("no session in db. sessionId:${sessionId}")
                    req.response.statusCode = 401
                    req.response.end()
                    return
                }
                if (dbsession.uid) {
                    find1('user', ['_id': dbsession.uid]) { user ->
                        if (!user) {
                            log.debug("no user in db. sessionId:${sessionId}, uid:${dbsession.uid}")
                            req.response.statusCode = 401
                            req.response.end()
                            return
                        }
                        dbsession.user = user
                        sessions[sessionId] = dbsession
                        after(sessions[sessionId])
                    }
                } else {
                    log.debug("no uid for sessionId:${sessionId}")
                    req.response.statusCode = 401
                    req.response.end()
                }
            }
        } else {
            after(session)
        }
    }

    public void setDbutil(dbutil) {
        this.dbutil = dbutil
    }

    public void setLog(log) {
        this.log = log
    }

    def getLog() {
        return log
    }


}