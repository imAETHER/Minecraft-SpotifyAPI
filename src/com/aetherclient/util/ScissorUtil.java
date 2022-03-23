package com.aetherclient.util;

/**
 * @author cedo
 * Aug 22, 2021
 */

public class ScissorUtil {

    public static void scissor(double x, double y, double width, double height) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        final double scale = sr.getScaleFactor();
        y = sr.getScaledHeight() - y;
        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;
        glScissor((int) x, (int) (y - height), (int) width, (int) height);
    }

}
