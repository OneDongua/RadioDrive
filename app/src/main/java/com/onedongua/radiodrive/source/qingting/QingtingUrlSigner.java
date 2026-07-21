package com.onedongua.radiodrive.source.qingting;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Builds signed live-stream URLs required by Qingting FM.
 */
public final class QingtingUrlSigner {
    private static final String APP_ID = "web";
    private static final String SECRET = "Lwrpu$K5oP";
    private static final long TIMESTAMP_OFFSET_SECONDS = 3600L;
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private QingtingUrlSigner() {
    }

    /**
     * Builds a signed URL using the current Unix timestamp.
     */
    public static String buildLiveUrl(String channelId) {
        //signed
        //long nowSeconds = System.currentTimeMillis() / 1000L;
        //return buildLiveUrl(channelId, nowSeconds);
        return QingtingModels.PLAY_URL_PREFIX + channelId + QingtingModels.PLAY_URL_SUFFIX;
    }

    /**
     * Visible for deterministic tests: {@code nowSeconds} is Unix time in seconds.
     */
    static String buildLiveUrl(String channelId, long nowSeconds) {
        if (channelId == null || channelId.length() == 0) {
            return null;
        }

        String path = "/live/" + channelId + QingtingModels.PLAY_URL_SUFFIX;
        String ts = Long.toHexString(nowSeconds + TIMESTAMP_OFFSET_SECONDS);
        String encodedPath;
        try {
            // Match JavaScript's encodeURIComponent exactly. In particular, the
            // percent escapes are upper-case (%2F), which is part of the HMAC input.
            encodedPath = URLEncoder.encode(path, UTF_8.name());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode Qingting stream path", e);
        }

        String signInput = "app_id=" + APP_ID + "&path=" + encodedPath + "&ts=" + ts;
        String sign = hmacMd5(signInput, SECRET);
        return QingtingModels.PLAY_URL_PREFIX + channelId + QingtingModels.PLAY_URL_SUFFIX
                + "?app_id=" + APP_ID + "&ts=" + ts + "&sign=" + sign;
    }

    private static String hmacMd5(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacMD5"));
            byte[] digest = mac.doFinal(value.getBytes(UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format(Locale.US, "%02x", b & 0xff));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-MD5 is unavailable", e);
        }
    }
}
