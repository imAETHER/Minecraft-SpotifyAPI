package com.aetherclient.module;

import com.aetherclient.SpotifyAPI;
import com.aetherclient.util.ColorUtil;
import com.aetherclient.util.ScissorUtil;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Track;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/**
 * @author cedo & aether
 * Aug 22, 2021
 */
public class SpotifyMod {
// Add your module initializers and stuff

    /**
     * PosX and PosY that will allow dragging & other stuff
     */
    private int posX = 50, posY = 50;

    private boolean downloadedCover;
    private int imageColor = -1;
    private ResourceLocation currentAlbumCover;

    private final SpotifyAPI spotifyAPI = new SpotifyAPI();
    private Track currentTrack;
    private CurrentlyPlayingContext currentPlayingContext;

    // Call this on your render event
    public void renderEvent() {

        //If the user is not playing anything or if the user is not authenticated yet
        if (mc.thePlayer == null || spotifyAPI.currentTrack == null || spotifyAPI.currentPlayingContext == null) {
            return;
        }
        //If the current track does not equal the track that is playing on spotify then it sets the variable to the current track
        if (currentTrack != spotifyAPI.currentTrack || currentPlayingContext != spotifyAPI.currentPlayingContext) {
            this.currentTrack = spotifyAPI.currentTrack;
            this.currentPlayingContext = spotifyAPI.currentPlayingContext;
        }

        // You can make these two customizable.
        final int albumCoverSize = 55;
        final int playerWidth = 150;

        final int diff = currentTrack.getDurationMs() - currentPlayingContext.getProgress_ms();
        final long diffSeconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
        final long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
        final String trackRemaining = String.format("-%s:%s", diffMinutes < 10 ? "0" + diffMinutes : diffMinutes, diffSeconds < 10 ? "0" + diffSeconds : diffSeconds);

        try {
            // The rect methods that have WH at the end means they use width & height instead of x2 and y2

            //Gradient Rect behind the text
            Gui.drawGradientRectWH(posX + albumCoverSize, posY, playerWidth, albumCoverSize, imageColor, new Color(20, 20, 20).getRGB());

            //We scissor the text to be inside the box
            ScissorUtil.scissor(posX + albumCoverSize, posY, playerWidth, albumCoverSize);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            // Display the current track name
            // TODO: make the text of the current track and artist scroll back and forth, with a pause at each end.
            mc.fontRendererObj.drawString("§l" + currentTrack.getName(), posX + albumCoverSize + 4, posY + 6, -1);

            /*For every artist, append them to a string builder to make them into a single string
            They are separated by commas unless there is only one Or if its the last one, then its a dot.*/
            final StringBuilder artistsDisplay = new StringBuilder();
            for (int artistIndex = 0; artistIndex < currentTrack.getArtists().length; artistIndex++) {
                final ArtistSimplified artist = currentTrack.getArtists()[artistIndex];
                artistsDisplay.append(artist.getName()).append(artistIndex + 1 == currentTrack.getArtists().length ? '.' : ", ");
            }

            mc.fontRendererObj.drawString(artistsDisplay.toString(), posX + albumCoverSize + 4, posY + 17, -1);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            // Draw how much time until the track ends
            mc.fontRendererObj.drawString(trackRemaining, posX + playerWidth + 8, posY + albumCoverSize - 11, -1);

            //This is where we draw the progress bar
            final int progressBarWidth = ((playerWidth - albumCoverSize) * currentPlayingContext.getProgress_ms()) / currentTrack.getDurationMs();
            Gui.drawRectWH(posX + albumCoverSize + 5, posY + albumCoverSize - 9, playerWidth - albumCoverSize, 4, new Color(50, 50, 50).getRGB());
            Gui.drawRectWH(posX + albumCoverSize + 5, posY + albumCoverSize - 9, progressBarWidth, 4, new Color(20, 200, 10).getRGB());

            if (currentAlbumCover != null && downloadedCover) {
                mc.getTextureManager().bindTexture(currentAlbumCover);
                GlStateManager.color(1,1,1);
                Gui.drawModalRectWithCustomSizedTexture(posX, posY, 0, 0, albumCoverSize, albumCoverSize, albumCoverSize, albumCoverSize);
            }
            if ((currentAlbumCover == null || !currentAlbumCover.getResourcePath().contains(currentTrack.getAlbum().getId()))) {
                downloadedCover = false;
                final ThreadDownloadImageData albumCover = new ThreadDownloadImageData(null, currentTrack.getAlbum().getImages()[1].getUrl(), null, new IImageBuffer() {
                    @Override
                    public BufferedImage parseUserSkin(BufferedImage image) {
                        imageColor = ColorUtil.averageColor(image, image.getWidth(), image.getHeight(), 1).getRGB();
                        downloadedCover = true;
                        return image;
                    }

                    @Override
                    public void skinAvailable() {
                    }
                });
                GlStateManager.color(1, 1, 1);
                mc.getTextureManager().loadTexture(currentAlbumCover = new ResourceLocation("spotifyAlbums/" + currentTrack.getAlbum().getId()), albumCover);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) {
            toggle();
            return;
        }
        spotifyAPI.init();
        super.onEnable();
    }
}
