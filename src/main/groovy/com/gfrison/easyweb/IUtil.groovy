package com.gfrison.easyweb

import org.vertx.java.core.json.impl.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat

/**
 * User: gfrison
 */
public interface IUtil {


    def notFound = { req ->
        req.response.statusCode = 404
        req.response.end()
    }
    def format = { Date date ->
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        return sdf.format(date);
    }
    def parse = { str ->
        return Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", str)
    }
    def b64SignHmacSha1 = { awsSecret, canonicalString ->
        SecretKeySpec signingKey = new SecretKeySpec(awsSecret.getBytes(),
                "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        return Base64.encodeBytes(mac.doFinal(canonicalString.getBytes()));
    }

    def s3Authorization = { method, bucket, key, headers ->
        def df = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
        String xamzdate = df.format(new Date())
        headers["X-Amz-Date"] = xamzdate
        headers['x-amz-acl'] = 'public-read'

        String canonicalizedAmzHeaders = "x-amz-acl:public-read\nx-amz-date:" + xamzdate + "\n"
        String canonicalizedResource = "/" + bucket + "/" + key;

        String toSign = method + "\n\n\n\n" + canonicalizedAmzHeaders + canonicalizedResource;

        String signature;
        try {
            signature = b64SignHmacSha1(container.config.awsSecret, toSign);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            signature = "ERRORSIGNATURE";
            // This will totally fail,
            // but downstream users can handle it
            logger.error("Failed to sign S3 request due to " + e);
        }
        String authorization = "AWS" + " " + container.config.awsAccess + ":" + signature;

        // Put that nasty auth string in the headers and let vert.x deal
        headers["Authorization"] = authorization

    }
    def query = { req ->
        req.query.tokenize('&').inject([:]) { map, e -> def t = e.tokenize('='); map << [(t[0]): (t[1])] }
    }

    def metaProperties = { ga ->
        ga.entrySet().findAll { it.key.startsWith('_') }.collectEntries()
    }
}