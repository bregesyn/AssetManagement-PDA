package com.ruoyi.asset.pda.core.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * 持久化 OkHttp Cookie，并使用 Android Keystore 保护落盘内容。
 */
public final class SessionCookieJar implements CookieJar {
    private static final Type COOKIE_LIST_TYPE = new TypeToken<List<StoredCookie>>() {
    }.getType();

    private final CookieStorage storage;
    private final Gson gson;
    private final Map<String, Cookie> cookies = new LinkedHashMap<>();
    private long generation;

    public SessionCookieJar(Context context) {
        this(new EncryptedCookieStorage(context.getApplicationContext()), new Gson());
    }

    SessionCookieJar(CookieStorage storage) {
        this(storage, new Gson());
    }

    SessionCookieJar(CookieStorage storage, Gson gson) {
        if (storage == null || gson == null) {
            throw new IllegalArgumentException("Cookie 存储组件不能为空");
        }
        this.storage = storage;
        this.gson = gson;
        restore();
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> responseCookies) {
        long now = System.currentTimeMillis();
        boolean identityChanged = removeExpired(now);
        for (Cookie cookie : responseCookies) {
            String key = cookieKey(cookie);
            Cookie previous = cookies.get(key);
            if (cookie.expiresAt() <= now) {
                identityChanged |= cookies.remove(key) != null;
            } else {
                cookies.put(key, cookie);
                identityChanged |= previous == null || !previous.value().equals(cookie.value());
            }
        }
        if (identityChanged) {
            generation++;
        }
        persist();
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        boolean changed = removeExpired(System.currentTimeMillis());
        List<Cookie> matched = new ArrayList<>();
        for (Cookie cookie : cookies.values()) {
            if (cookie.matches(url)) {
                matched.add(cookie);
            }
        }
        if (changed) {
            generation++;
            persist();
        }
        return matched;
    }

    public synchronized boolean isEmpty() {
        if (removeExpired(System.currentTimeMillis())) {
            generation++;
            persist();
        }
        return cookies.isEmpty();
    }

    public synchronized void clear() {
        if (!cookies.isEmpty()) {
            generation++;
        }
        cookies.clear();
        storage.clear();
    }

    /**
     * 会话代次只随实际 Cookie 身份变化，重复确认同一登录态不会制造新代次。
     */
    public synchronized long getGeneration() {
        if (removeExpired(System.currentTimeMillis())) {
            generation++;
            persist();
        }
        return generation;
    }

    /**
     * 比较与清理必须在同一把锁内完成，避免旧 401 检查后误清除刚写入的新 Cookie。
     */
    public synchronized boolean clearIfGeneration(long expectedGeneration) {
        if (removeExpired(System.currentTimeMillis())) {
            generation++;
            persist();
        }
        if (generation != expectedGeneration && !cookies.isEmpty()) {
            return false;
        }
        clear();
        return true;
    }

    private void restore() {
        String value = storage.read();
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            List<StoredCookie> storedCookies = gson.fromJson(value, COOKIE_LIST_TYPE);
            if (storedCookies == null) {
                return;
            }
            long now = System.currentTimeMillis();
            for (StoredCookie storedCookie : storedCookies) {
                Cookie cookie = storedCookie.toCookie();
                if (cookie.expiresAt() > now) {
                    cookies.put(cookieKey(cookie), cookie);
                }
            }
            if (cookies.size() != storedCookies.size()) {
                persist();
            }
        } catch (JsonParseException | IllegalArgumentException exception) {
            // 密文损坏或旧格式不再可信时只丢弃会话，不能回退为明文或让应用崩溃。
            cookies.clear();
            storage.clear();
        }
    }

    private boolean removeExpired(long now) {
        boolean changed = false;
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, Cookie> entry : cookies.entrySet()) {
            if (entry.getValue().expiresAt() <= now) {
                expiredKeys.add(entry.getKey());
            }
        }
        for (String key : expiredKeys) {
            cookies.remove(key);
            changed = true;
        }
        return changed;
    }

    private void persist() {
        if (cookies.isEmpty()) {
            storage.clear();
            return;
        }
        List<StoredCookie> storedCookies = new ArrayList<>();
        for (Cookie cookie : cookies.values()) {
            storedCookies.add(StoredCookie.from(cookie));
        }
        storage.write(gson.toJson(storedCookies));
    }

    private static String cookieKey(Cookie cookie) {
        return cookie.domain() + '\n' + cookie.path() + '\n' + cookie.name();
    }

    interface CookieStorage {
        String read();

        void write(String value);

        void clear();
    }

    private static final class StoredCookie {
        private String name;
        private String value;
        private long expiresAt;
        private String domain;
        private String path;
        private boolean secure;
        private boolean httpOnly;
        private boolean hostOnly;
        private boolean persistent;

        private static StoredCookie from(Cookie cookie) {
            StoredCookie storedCookie = new StoredCookie();
            storedCookie.name = cookie.name();
            storedCookie.value = cookie.value();
            storedCookie.expiresAt = cookie.expiresAt();
            storedCookie.domain = cookie.domain();
            storedCookie.path = cookie.path();
            storedCookie.secure = cookie.secure();
            storedCookie.httpOnly = cookie.httpOnly();
            storedCookie.hostOnly = cookie.hostOnly();
            storedCookie.persistent = cookie.persistent();
            return storedCookie;
        }

        private Cookie toCookie() {
            Cookie.Builder builder = new Cookie.Builder()
                    .name(name)
                    .value(value)
                    .path(path);
            if (hostOnly) {
                builder.hostOnlyDomain(domain);
            } else {
                builder.domain(domain);
            }
            if (persistent) {
                builder.expiresAt(expiresAt);
            }
            if (secure) {
                builder.secure();
            }
            if (httpOnly) {
                builder.httpOnly();
            }
            return builder.build();
        }
    }

    private static final class EncryptedCookieStorage implements CookieStorage {
        private static final String PREFERENCES_NAME = "pda_session_secure";
        private static final String VALUE_KEY = "cookie_payload";
        private static final String IV_KEY = "cookie_iv";
        private static final String KEY_ALIAS = "asset_pda_session_cookie_key";
        private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
        private static final String TRANSFORMATION = "AES/GCM/NoPadding";

        private final SharedPreferences preferences;

        private EncryptedCookieStorage(Context context) {
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

        @Override
        public String read() {
            String encryptedValue = preferences.getString(VALUE_KEY, null);
            String encodedIv = preferences.getString(IV_KEY, null);
            if (encryptedValue == null && encodedIv == null) {
                return null;
            }
            if (encryptedValue == null || encodedIv == null) {
                clear();
                return null;
            }
            try {
                byte[] iv = Base64.decode(encodedIv, Base64.NO_WRAP);
                byte[] ciphertext = Base64.decode(encryptedValue, Base64.NO_WRAP);
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
            } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
                // Keystore 被重置或密文损坏时，本地 Session 已不可恢复，安全退出即可。
                clear();
                return null;
            }
        }

        @Override
        @SuppressLint("ApplySharedPref")
        public void write(String value) {
            try {
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
                byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
                boolean saved = preferences.edit()
                        .putString(VALUE_KEY, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                        .putString(IV_KEY, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                        .commit();
                if (!saved) {
                    throw new IllegalStateException("无法安全保存会话信息");
                }
            } catch (GeneralSecurityException | IOException exception) {
                throw new IllegalStateException("无法安全保存会话信息", exception);
            }
        }

        @Override
        @SuppressLint("ApplySharedPref")
        public void clear() {
            // Session 写入和退出清理都必须在返回前持久化，避免进程紧接着退出时恢复旧会话。
            if (!preferences.edit().clear().commit()) {
                throw new IllegalStateException("无法安全清除会话信息");
            }
        }

        private SecretKey getOrCreateSecretKey() throws GeneralSecurityException, IOException {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            }
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            keyGenerator.init(keySpec);
            return keyGenerator.generateKey();
        }
    }
}
