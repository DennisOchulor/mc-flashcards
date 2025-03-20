package io.github.dennisochulor.flashcards;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.*;

public final class ImageUtils {
    private ImageUtils() {}

    public static final FileNameExtensionFilter FILE_NAME_EXTENSION_FILTER = new FileNameExtensionFilter("JPG/JPEG/PNG image files","jpg","png","jpeg");

    public record ImagePackage(Identifier id, int width, int height, float widthScaler, float heightScaler) {}

    /**
     * @param file The image file
     * @return an {@link ImagePackage} for the image. This returns null if the image file does not exist or
     * if the image file is using an unsupported filename extension according to {@link ImageUtils#FILE_NAME_EXTENSION_FILTER}
     */
    public static ImagePackage getImageId(File file) {
        try {
            if(!file.exists()) return null;
            if(!FILE_NAME_EXTENSION_FILTER.accept(file)) return null;

            NativeImage img = null;
            if(file.getName().endsWith(".png")) {
                img = NativeImage.read(new FileInputStream(file));
            }
            else if(file.getName().endsWith(".jpeg") || file.getName().endsWith(".jpg")) {
                BufferedImage bufferedImage = ImageIO.read(file);
                img = new NativeImage(bufferedImage.getWidth(),bufferedImage.getHeight(),false);
                for(int y=0;y<bufferedImage.getHeight();y++) {
                    for(int x=0;x<bufferedImage.getWidth();x++) {
                        int argb = bufferedImage.getRGB(x,y);
                        img.setColorArgb(x,y,argb);
                    }
                }
            }

            NativeImageBackedTexture texture = new NativeImageBackedTexture(file::getName, img);
            float greaterDimension = Math.max(img.getHeight(),img.getWidth());
            Identifier id = Identifier.of("flashcards", "theimage");
            MinecraftClient.getInstance().getTextureManager().registerTexture(id,texture);
            return new ImagePackage(id,img.getWidth(),img.getHeight(),img.getWidth()/greaterDimension,img.getHeight()/greaterDimension);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

