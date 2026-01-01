package io.github.dennisochulor.flashcards;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;
import org.jspecify.annotations.Nullable;

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
    @Nullable
    public static ImagePackage getImagePackage(File file) {
        try {
            if(!file.exists()) return null;
            if(!FILE_NAME_EXTENSION_FILTER.accept(file)) return null;

            NativeImage img;
            if(file.getName().endsWith(".png")) {
                img = NativeImage.read(new FileInputStream(file));
            }
            else if(file.getName().endsWith(".jpeg") || file.getName().endsWith(".jpg")) {
                BufferedImage bufferedImage = ImageIO.read(file);
                img = new NativeImage(bufferedImage.getWidth(),bufferedImage.getHeight(),false);
                for(int y=0;y<bufferedImage.getHeight();y++) {
                    for(int x=0;x<bufferedImage.getWidth();x++) {
                        int argb = bufferedImage.getRGB(x,y);
                        img.setPixel(x,y,argb);
                    }
                }
            }
            else {
                throw new IllegalStateException("File bypassed extension filter: " + file.getPath());
            }

            DynamicTexture texture = new DynamicTexture(file::getName, img);
            float greaterDimension = Math.max(img.getHeight(),img.getWidth());
            Identifier id = Identifier.fromNamespaceAndPath("flashcards", "theimage");
            Minecraft.getInstance().getTextureManager().register(id,texture);
            return new ImagePackage(id,img.getWidth(),img.getHeight(),img.getWidth()/greaterDimension,img.getHeight()/greaterDimension);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

