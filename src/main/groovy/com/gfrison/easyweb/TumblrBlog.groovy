package com.gfrison.easyweb

import groovy.json.JsonSlurper
import org.vertx.groovy.core.http.HttpClient

import javax.annotation.PostConstruct
import java.text.BreakIterator
import java.text.DateFormat

/**
 * User: gfrison
 */
public class TumblrBlog {

    def log

    def vertx

    @Delegate
    DB dbutil

    def conf
    def client
    def posts = []

    @PostConstruct
    def init() {
        log.info("tumblr conf:${conf}")
        refresh()
        if (conf?.refresh) {
            log.info("refresh blog in ${conf.refresh / 1000} secs")
            vertx.setPeriodic(conf.refresh) {
                refresh()

            }
        } else {
            log.info('no refreshing blog posts')
        }
    }

    def refresh = { cls ->
        log.debug('refresh tumblr')
        HttpClient client = vertx.createHttpClient(host: 'api.tumblr.com')
        client.getNow("/v2/blog/${conf.blog_name}.tumblr.com/posts?api_key=hF8i9hNNjlzslkbVArsS12YcXjYkGq0l9BrTpgKETix3ZH5ZL7") { resp ->
            resp.bodyHandler { body ->
                def slurper = new JsonSlurper()
                def result = slurper.parseText(body?.toString())
                def tumblr = result.response.posts
                tumblr.each { post ->
                    try {
                        post.date = new java.sql.Date(post.timestamp.toLong() * 1000)
                        Calendar c = post.date.toCalendar()
                        def year = c.get(Calendar.YEAR)
                        def month = c.get(Calendar.MONTH) + 1
                        post.link = post.url
                        post.url = "/blog/" + (post.title ? "${year}/${month}/" + AsciiUtils.convertNonAscii(post.title.replaceAll("[\\W]+", "-").toLowerCase()) : post.id)
                        if (post.url.endsWith('-')) {
                            post.url = post.url.substring(0, post.url.length() - 1)
                        }
                        post.short_url = "/blog/" + post.id
                        if (post.description) {
                            post.preview = truncate(post.description.replaceAll("<(.|\\n)*?>", ""), 300)
                        } else if (post.caption) {
                            post.preview = truncate(post.caption.replaceAll("<(.|\\n)*?>", ""), 300);
                        } else {
                            post.preview = truncate(post?.body.replaceAll("<(.|\\n)*?>", ""), 300);
                        }
                        def extractImg = { text ->
                            if (!text) {
                                return []
                            }
                            (text =~ /<img[^>]+src="([^">]+)"/).collect { it[1] }
                        }
                        def images = extractImg(post.descrition)
                        images.addAll(extractImg(post.caption))
                        images.addAll(extractImg(post.body))
                        log.debug("post images:${images}")

                        post.images = images
                        findById('post', post.id.toString()) { dbpost ->
                            def set = [:]
                            if (!dbpost || !dbpost.url) {
                                set.url = post.url
                            }
                            set.date = post.date.time
                            set.title = post.title
                            set.link = post.link
                            set.short_url = post.short_url
                            set.preview = post.preview
                            set.description = post.description
                            set.body = post.body
                            set.caption = post.caption
                            set.images = post.images
                            set.tags = post.tags
                            set.type = post.type
                            update('post', ['_id': post.id.toString()], [$set: set], true)
                        }
                    } catch (Exception e) {
                        log.error("error parsing blog:${post}", e)
                    }

                }
                def ids = tumblr.collect { it.id.toString() }
                removeBy('post', [$nin: ids])
                refreshFromDB(ids)
                if (cls) {
                    cls()
                }
            }
        }
    }

    private refreshFromDB(ids) {
        findAllById('post', ids, null) { dbposts ->
            if (dbposts.find { !it.date }) {
                refreshFromDB(ids)
            } else {
                dbposts.each {
                    it.id = it._id.toLong();
                    it.datePretty = DateFormat.getDateInstance(DateFormat.FULL, Locale.ITALY).format(new Date(it?.date))
                    findAllById('user', it.recommendedBy, null) { recommendList ->
                        log.debug("post id:${it.id}, raccomandato da:${recommendList}, in lista:${it.recommendedBy}")
                        if (recommendList) {
                            recommendList.each { rec ->
                                updateRecommendList(it, rec)
                            }
                        }
                    }
                }
                posts = dbposts.sort { -it?.date }

            }
        }

    }

    private String truncate(String text, int maxLength) {
        if (text != null && text.length() > maxLength) {
            BreakIterator bi = BreakIterator.getWordInstance();
            bi.setText(text);

            if (bi.isBoundary(maxLength - 1)) {
                return text.substring(0, maxLength - 2) + '...';
            } else {
                int preceding = bi.preceding(maxLength - 1);
                return text.substring(0, preceding - 1) + '...';
            }
        } else {
            return text;
        }
    }

    def recommend(String id, def user) {
        update('post', ['_id': id], ['$addToSet': ['recommendedBy': user._id]], true, false)
        update('user', ['_id': user._id], ['$addToSet': ['recommendPost': id]], true, false)
        def post = posts.find { it.id == id.toLong() }
        updateRecommendList(post, user)
        if (!user.recommendPost) {
            user.recommendPost = []
        }
        user.recommendPost << id

    }

    def unrecommend(String id, def user) {
        update('post', ['_id': id], ['$pull': ['recommendedBy': user._id]], true, false)
        update('user', ['_id': user._id], ['$pull': ['recommendPost': id]], true, false)
        def post = posts.find { it.id == id.toLong() }
        if (post?.recommended) {
            def rimosso = post.recommended.remove(post.recommended.find { it._id == user._id })
            log.debug("rimosso recommended:${user._id} dal post:${id}, ${rimosso}")
        }
        user.recommendPost.remove(id)

    }

    private void updateRecommendList(post, user) {
        if (!post.recommended) {
            post.recommended = []
        }
        if (!post.recommended.find { it._id == user._id }) {
            post.recommended << [_id: user._id, name: user.name, img: user.img]
        }
    }

    public void setLog(log) {
        this.log = log
    }

    public void setVertx(vertx) {
        this.vertx = vertx
    }

}
