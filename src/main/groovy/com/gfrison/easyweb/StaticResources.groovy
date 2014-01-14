package com.gfrison.easyweb

import groovy.io.FileType
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.platform.Container

import javax.annotation.PostConstruct
import java.util.zip.GZIPOutputStream

/**
 * The class map on RouteMatcher all files indicated on 'configuration.folder'
 * or in the env property STATIC_FOLDER or, by default, in the 'static/' folder.
 * It's possibile to cache all resources (production environment) for performance reasons.
 *
 * The component leverages on HTTP caching with appropriate headers (ETag etc)
 * HTTP compression (gzip) is enabled
 * User: gfrison
 */
class StaticResources implements IUtil {


    Vertx vertx
    Container container
    def log
    boolean gzipFiles = false
    def folder = 'static/'

    def files = [:]
    boolean cache = true
    def conf
    def matcher

    class FileCache {
        def contentType
        def content
        def lastModified
        def path
        def gzip

    }

    @PostConstruct
    def init() {

        initMime()
        cache = conf?.cache ?: cache
        folder = container.env['STATIC_FOLDER'] ?: conf?.folder ?: folder
        if (!folder.endsWith('/')) {
            folder += '/'
        }
        log.info("init static file server on folder:" + folder)
        def staticFolder = new File(folder)
        if (!staticFolder.exists()) {
            log.warn("static folder do not exists")
            return
        }

        staticFolder.eachFileRecurse(FileType.FILES) { file ->
            def name = file.path.substring(folder.length())
            try {
                files[name] = mapFile(file)
                log.info('load file:' + name + ', contentType:' + files[name].contentType)
            } catch (Exception e) {
                log.warn('error on file ' + file.name)
            }
        }

        /*
          all no-matching GET requests are handled by this component
         */
        matcher.noMatch { req ->
            addIndex(req, req.path)
        }
        log.info("static loaded")


    }

    def mapFile = { File file ->
        def outstream = new ByteArrayOutputStream()
        def zip = new GZIPOutputStream(outstream)
        zip.write(file.bytes)
        zip.close()
        return new FileCache(contentType: mime[file.path.substring(file.path.lastIndexOf('.') + 1)],
                content: file.bytes, lastModified: file.lastModified() + '', path: file.absolutePath,
                gzip: outstream.toByteArray())
    }


    private void addIndex(req, String path) {
        if (req.method.equals('GET')) {
            if (!req.path.contains("..")) {
                log.debug('GET method:' + path)
                if (path.endsWith('/')) {
                    path += 'index.html'
                }
                transmit(fileCache(path[1..-1]), path[1..-1], req)
            }
        } else {
            log.debug('not GET method')
            notFound(req)

        }

    }

    def sendFile = { fileName, req ->
        transmit(fileCache(fileName), fileName, req)
    }


    def fileCache = { fileName ->
        if (cache) {
            return files[fileName]
        } else {
            FileCache fc = null
            File file = new File(folder + fileName);
            if (file.exists()) {
                fc = mapFile(file)
                log.debug("file exists:${fileName}, gzip lenght:${fc.gzip.length}")
            } else {
                log.warn('file non esiste:' + folder + fileName)
            }
            return fc
        }
    }

    private void transmit(FileCache fcache, fileName, req) {
        String acceptEncoding = req.headers.get("accept-encoding");
        boolean acceptEncodingGzip = acceptEncoding == null ? false : acceptEncoding.contains("gzip");
        if (!fcache) {
            log.warn(folder + fileName + ' do not exist')
            notFound(req)
        } else if (req.headers.get('If-None-Match')?.trim().equals(fcache.lastModified)) {
            req.response.statusCode = 304
            req.response.end()
        } else {
            req.response.putHeader('ETag', fcache.lastModified)
            req.response.putHeader('Content-Type', fcache.contentType)
            if (acceptEncodingGzip) {
                log.info("filename ${fileName}, contentType ${fcache.contentType} lenght ${fcache.gzip.length}")
                req.response.putHeader('Content-Length', fcache.gzip.length + "")
                req.response.putHeader("Content-Encoding", "gzip");
                req.response.write(new org.vertx.groovy.core.buffer.Buffer(fcache.gzip))
            } else {
                log.info("filename ${fileName}, contentType ${fcache.contentType} lenght ${fcache.content.length}")
                req.response.putHeader('Content-Length', fcache.content.length + "")
                req.response.write(new org.vertx.groovy.core.buffer.Buffer(fcache.content))
            }
            req.response.end()

        }
    }

    def mime = [:]

    def initMime() {

        mime['woff'] = 'application/x-font-woff'
        mime['ttf'] = 'application/octet-stream'
        mime['svg'] = 'image/svg+xml'
        mime['323'] = 'text/h323'
        mime['*'] = 'application/octet-stream'
        mime['acx'] = 'application/internet-property-stream'
        mime['ai'] = 'application/postscript'
        mime['aif'] = 'audio/x-aiff'
        mime['aifc'] = 'audio/x-aiff'
        mime['aiff'] = 'audio/x-aiff'
        mime['asf'] = 'video/x-ms-asf'
        mime['asr'] = 'video/x-ms-asf'
        mime['asx'] = 'video/x-ms-asf'
        mime['au'] = 'audio/basic'
        mime['avi'] = 'video/x-msvideo'
        mime['axs'] = 'application/olescript'
        mime['bas'] = 'text/plain'
        mime['bcpio'] = 'application/x-bcpio'
        mime['bin'] = 'application/octet-stream'
        mime['bmp'] = 'image/bmp'
        mime['c'] = 'text/plain'
        mime['cat'] = 'application/vnd.ms-pkiseccat'
        mime['cdf'] = 'application/x-cdf'
        mime['cdf'] = 'application/x-netcdf'
        mime['cer'] = 'application/x-x509-ca-cert'
        mime['class'] = 'application/octet-stream'
        mime['clp'] = 'application/x-msclip'
        mime['cmx'] = 'image/x-cmx'
        mime['cod'] = 'image/cis-cod'
        mime['cpio'] = 'application/x-cpio'
        mime['crd'] = 'application/x-mscardfile'
        mime['crl'] = 'application/pkix-crl'
        mime['crt'] = 'application/x-x509-ca-cert'
        mime['csh'] = 'application/x-csh'
        mime['css'] = 'text/css'
        mime['dcr'] = 'application/x-director'
        mime['der'] = 'application/x-x509-ca-cert'
        mime['dir'] = 'application/x-director'
        mime['dll'] = 'application/x-msdownload'
        mime['dms'] = 'application/octet-stream'
        mime['doc'] = 'application/msword'
        mime['dot'] = 'application/msword'
        mime['dvi'] = 'application/x-dvi'
        mime['dxr'] = 'application/x-director'
        mime['eps'] = 'application/postscript'
        mime['etx'] = 'text/x-setext'
        mime['evy'] = 'application/envoy'
        mime['exe'] = 'application/octet-stream'
        mime['fif'] = 'application/fractals'
        mime['flr'] = 'x-world/x-vrml'
        mime['gif'] = 'image/gif'
        mime['gtar'] = 'application/x-gtar'
        mime['gz'] = 'application/x-gzip'
        mime['h'] = 'text/plain'
        mime['hdf'] = 'application/x-hdf'
        mime['hlp'] = 'application/winhlp'
        mime['hqx'] = 'application/mac-binhex40'
        mime['hta'] = 'application/hta'
        mime['htc'] = 'text/x-component'
        mime['htm'] = 'text/html'
        mime['html'] = 'text/html'
        mime['htt'] = 'text/webviewhtml'
        mime['ico'] = 'image/x-icon'
        mime['ief'] = 'image/ief'
        mime['iii'] = 'application/x-iphone'
        mime['ins'] = 'application/x-internet-signup'
        mime['isp'] = 'application/x-internet-signup'
        mime['jfif'] = 'image/pipeg'
        mime['jpe'] = 'image/jpeg'
        mime['jpeg'] = 'image/jpeg'
        mime['jpg'] = 'image/jpeg'
        mime['png'] = 'image/png'
        mime['js'] = 'application/x-javascript'
        mime['latex'] = 'application/x-latex'
        mime['lha'] = 'application/octet-stream'
        mime['lsf'] = 'video/x-la-asf'
        mime['lsx'] = 'video/x-la-asf'
        mime['lzh'] = 'application/octet-stream'
        mime['m13'] = 'application/x-msmediaview'
        mime['m14'] = 'application/x-msmediaview'
        mime['m3u'] = 'audio/x-mpegurl'
        mime['man'] = 'application/x-troff-man'
        mime['mdb'] = 'application/x-msaccess'
        mime['me'] = 'application/x-troff-me'
        mime['mht'] = 'message/rfc822'
        mime['mhtml'] = 'message/rfc822'
        mime['mid'] = 'audio/mid'
        mime['mny'] = 'application/x-msmoney'
        mime['mov'] = 'video/quicktime'
        mime['movie'] = 'video/x-sgi-movie'
        mime['mp2'] = 'video/mpeg'
        mime['mp3'] = 'audio/mpeg'
        mime['mpa'] = 'video/mpeg'
        mime['mpe'] = 'video/mpeg'
        mime['mpeg'] = 'video/mpeg'
        mime['mpg'] = 'video/mpeg'
        mime['mpp'] = 'application/vnd.ms-project'
        mime['mpv2'] = 'video/mpeg'
        mime['ms'] = 'application/x-troff-ms'
        mime['msg'] = 'application/vnd.ms-outlook'
        mime['mvb'] = 'application/x-msmediaview'
        mime['nc'] = 'application/x-netcdf'
        mime['nws'] = 'message/rfc822'
        mime['oda'] = 'application/oda'
        mime['p10'] = 'application/pkcs10'
        mime['p12'] = 'application/x-pkcs12'
        mime['p7b'] = 'application/x-pkcs7-certificates'
        mime['p7c'] = 'application/x-pkcs7-mime'
        mime['p7m'] = 'application/x-pkcs7-mime'
        mime['p7r'] = 'application/x-pkcs7-certreqresp'
        mime['p7s'] = 'application/x-pkcs7-signature'
        mime['pbm'] = 'image/x-portable-bitmap'
        mime['pdf'] = 'application/pdf'
        mime['pfx'] = 'application/x-pkcs12'
        mime['pgm'] = 'image/x-portable-graymap'
        mime['pko'] = 'application/ynd.ms-pkipko'
        mime['pma'] = 'application/x-perfmon'
        mime['pmc'] = 'application/x-perfmon'
        mime['pml'] = 'application/x-perfmon'
        mime['pmr'] = 'application/x-perfmon'
        mime['pmw'] = 'application/x-perfmon'
        mime['pnm'] = 'image/x-portable-anymap'
        mime['pot'] = 'application/vnd.ms-powerpoint'
        mime['ppm'] = 'image/x-portable-pixmap'
        mime['pps'] = 'application/vnd.ms-powerpoint'
        mime['ppt'] = 'application/vnd.ms-powerpoint'
        mime['prf'] = 'application/pics-rules'
        mime['ps'] = 'application/postscript'
        mime['pub'] = 'application/x-mspublisher'
        mime['qt'] = 'video/quicktime'
        mime['ra'] = 'audio/x-pn-realaudio'
        mime['ram'] = 'audio/x-pn-realaudio'
        mime['ras'] = 'image/x-cmu-raster'
        mime['rgb'] = 'image/x-rgb'
        mime['rmi'] = 'audio/mid'
        mime['roff'] = 'application/x-troff'
        mime['rtf'] = 'application/rtf'
        mime['rtx'] = 'text/richtext'
        mime['scd'] = 'application/x-msschedule'
        mime['sct'] = 'text/scriptlet'
        mime['setpay'] = 'application/set-payment-initiation'
        mime['setreg'] = 'application/set-registration-initiation'
        mime['sh'] = 'application/x-sh'
        mime['shar'] = 'application/x-shar'
        mime['sit'] = 'application/x-stuffit'
        mime['snd'] = 'audio/basic'
        mime['spc'] = 'application/x-pkcs7-certificates'
        mime['spl'] = 'application/futuresplash'
        mime['src'] = 'application/x-wais-source'
        mime['sst'] = 'application/vnd.ms-pkicertstore'
        mime['stl'] = 'application/vnd.ms-pkistl'
        mime['stm'] = 'text/html'
        mime['svg'] = 'image/svg+xml'
        mime['swf'] = 'application/x-shockwave-flash'
        mime['t'] = 'application/x-troff'
        mime['tar'] = 'application/x-tar'
        mime['tcl'] = 'application/x-tcl'
        mime['tex'] = 'application/x-tex'
        mime['texi'] = 'application/x-texinfo'
        mime['tgz'] = 'application/x-compressed'
        mime['tif'] = 'image/tiff'
        mime['tiff'] = 'image/tiff'
        mime['tr'] = 'application/x-troff'
        mime['trm'] = 'application/x-msterminal'
        mime['tsv'] = 'text/tab-separated-values'
        mime['txt'] = 'text/plain'
        mime['uls'] = 'text/iuls'
        mime['ustar'] = 'application/x-ustar'
        mime['vcf'] = 'text/x-vcard'
        mime['vrml'] = 'x-world/x-vrml'
        mime['wav'] = 'audio/x-wav'
        mime['wcm'] = 'application/vnd.ms-works'
        mime['wdb'] = 'application/vnd.ms-works'
        mime['wks'] = 'application/vnd.ms-works'
        mime['wmf'] = 'application/x-msmetafile'
        mime['wps'] = 'application/vnd.ms-works'
        mime['wri'] = 'application/x-mswrite'
        mime['wrl'] = 'x-world/x-vrml'
        mime['wrz'] = 'x-world/x-vrml'
        mime['xaf'] = 'x-world/x-vrml'
        mime['xbm'] = 'image/x-xbitmap'
        mime['xla'] = 'application/vnd.ms-excel'
        mime['xlc'] = 'application/vnd.ms-excel'
        mime['xlm'] = 'application/vnd.ms-excel'
        mime['xls'] = 'application/vnd.ms-excel'
        mime['xlt'] = 'application/vnd.ms-excel'
        mime['xlw'] = 'application/vnd.ms-excel'
        mime['xof'] = 'x-world/x-vrml'
        mime['xpm'] = 'image/x-xpixmap'
        mime['xwd'] = 'image/x-xwindowdump'
        mime['z'] = 'application/x-compress'
        mime['zip'] = 'application/zip'
    }

}
