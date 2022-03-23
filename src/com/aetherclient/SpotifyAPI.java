package com.aetherclient;

import com.sun.net.httpserver.HttpServer;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERequest;
import com.wrapper.spotify.requests.data.player.GetInformationAboutUsersCurrentPlaybackRequest;
import com.wrapper.spotify.requests.data.tracks.GetTrackRequest;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

interface SpotifyCallback {
    void codeCallback(final String code);
}

/**
 * @author cedo & aether
 * Aug 22, 2021
 */
public class SpotifyAPI {

    /**
     * <h2>Building the Spotify API<h2>
     * <p>For this you're going need a "ClientId", you can get one by going to:
     * <a href="https://developer.spotify.com/dashboard/applications">Spotify Developer Application</a>
     * <p>
     * Visit the website and create an application, then copy the client id and paste it below.
     * The localhost is for authorization, simulates a website where your app should
     * redirect to once OAuth is complete.
     * <p>
     */
    public final SpotifyApi SPOTIFY = new SpotifyApi.Builder()
            .setClientId("CLIENT-ID-HERE")
            .setRedirectUri(SpotifyHttpManager.makeUri("http://localhost:4030"))
            .build();

    /**
     * According to Spotify's Developer Guide, you need to generate these two
     * randomly, CODE_VERIFIER by making a random string, and the CHALLENGE by hashing the VERIFIER
     * using SHA-256 and then encoding it with Base64 Url encoding.
     */
    private final String CODE_VERIFIER = randomString();
    public final String CODE_CHALLENGE = getChallengeHash(CODE_VERIFIER);
    public AuthorizationCodeUriRequest AUTH_CODE_URI = SPOTIFY.authorizationCodePKCEUri(CODE_CHALLENGE)
            .scope("user-read-playback-state user-read-playback-position user-modify-playback-state user-read-currently-playing")
            .build();
    private final String PREFIX = "\2472[\247aSpotify\2472]\247a";
    private final Minecraft mc = Minecraft.getMinecraft();
    public Track currentTrack;
    public CurrentlyPlayingContext currentPlayingContext;
    public boolean authenticated;
    private HttpServer callbackServer;
    private int tokenRefreshInterval = 2;

    /**
     * This is where current track info & token updates take place
     * If you want to know how this works in depth, refer to Spotify's Docs:
     * https://developer.spotify.com/documentation/general/guides/authorization-guide/
     */
    private final SpotifyCallback callback = code -> {
        mc.thePlayer.addChatComponentMessage(new ChatComponentText(PREFIX + " Connecting..."));
        AuthorizationCodePKCERequest authCodePKCERequest = SPOTIFY.authorizationCodePKCE(code, CODE_VERIFIER).build();
        try {
            final AuthorizationCodeCredentials authCredentials = authCodePKCERequest.execute();
            SPOTIFY.setAccessToken(authCredentials.getAccessToken());
            SPOTIFY.setRefreshToken(authCredentials.getRefreshToken());
            tokenRefreshInterval = authCredentials.getExpiresIn();
            authenticated = true;
            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        TimeUnit.SECONDS.sleep(tokenRefreshInterval - 2);
                        final AuthorizationCodeCredentials refreshRequest = SPOTIFY.authorizationCodePKCERefresh().build().execute();
                        SPOTIFY.setAccessToken(refreshRequest.getAccessToken());
                        SPOTIFY.setRefreshToken(refreshRequest.getRefreshToken());
                        tokenRefreshInterval = refreshRequest.getExpiresIn();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                        final GetInformationAboutUsersCurrentPlaybackRequest getCurrentPlaybackInfo = SPOTIFY.getInformationAboutUsersCurrentPlayback().build();
                        final CurrentlyPlayingContext currentlyPlayingContext = getCurrentPlaybackInfo.execute();
                        final String currentTrackId = currentlyPlayingContext.getItem().getId();
                        GetTrackRequest getTrackRequest = SPOTIFY.getTrack(currentTrackId).build();
                        this.currentTrack = getTrackRequest.execute();
                        this.currentPlayingContext = currentlyPlayingContext;
                    } catch (Exception ignored) {
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    private String getChallengeHash(String codeVerifier) {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().encodeToString(digest.digest(bytes)).replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String randomString() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 43; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));

        return sb.toString();
    }

    public void init() {
        if (!authenticated) {
            try {
                //Open the authorization window on the default browser
                Desktop.getDesktop().browse(AUTH_CODE_URI.execute());
                Client.INSTANCE.getExecutorService().submit(() -> {
                    try {
                        if (callbackServer != null) {
                            // Close the server if the module was disabled and re-enabled, to prevent already bound exception.
                            callbackServer.stop(0);
                        }
                        mc.thePlayer.addChatComponentMessage(new ChatComponentText(PREFIX + " Waiting for Spotify confirmation..."));
                        callbackServer = HttpServer.create(new InetSocketAddress(4030), 0);
                        callbackServer.createContext("/", context -> {
                            callback.codeCallback(context.getRequestURI().getQuery().split("=")[1]);
                            final String infoMessage = context.getRequestURI().getQuery().toString().contains("code")
                                    ? "<h1>Successfully authorized.\nYou can now close this window, have fun!</h1>"
                                    : "<h1>Unable to Authorize client, re-toggle the module.</h1>";
                            context.sendResponseHeaders(200, infoMessage.length());
                            OutputStream out = context.getResponseBody();
                            out.write(infoMessage.getBytes());
                            out.close();
                            callbackServer.stop(0);
                        });
                        callbackServer.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
