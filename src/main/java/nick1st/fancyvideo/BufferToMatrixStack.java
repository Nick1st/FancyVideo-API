package nick1st.fancyvideo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class BufferToMatrixStack {
    public Matrix4f matrix4f;
    public BufferBuilder bb;

    public BufferToMatrixStack(MatrixStack matrix) {
        matrix4f = matrix.last().pose(); //getLast().getMatrix();
        bb = Tessellator.getInstance().getBuilder();
        RenderSystem.disableTexture();
        // Required for transparency
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        bb.begin(7, DefaultVertexFormats.POSITION_COLOR);
    }

    public BufferToMatrixStack(MatrixStack matrix, Tessellator tessellator) {
        matrix4f = matrix.last().pose();
        bb = tessellator.getBuilder();
        bb.begin(7, DefaultVertexFormats.POSITION_COLOR);
    }

    public static int[] extractBytes(String imageName) throws IOException {
        // open image
        File imgPath = new File(imageName);
        BufferedImage bufferedImage = ImageIO.read(imgPath);

        int[] image = new int[bufferedImage.getHeight() * bufferedImage.getWidth()];
        int k = 0;
        for (int i = 0; i < bufferedImage.getHeight(); i++) {
            for (int j = 0; j < bufferedImage.getWidth(); j++) {
                image[k] = bufferedImage.getRGB(j, i);
                k++;
            }
        }
        return image;
    }

    public BufferToMatrixStack set(float minX, float minY, float maxX, float maxY, int color, float opacity) {
        if (minX < maxX) {
            float i = minX;
            minX = maxX;
            maxX = i;
        }
        if (minY < maxY) {
            float j = minY;
            minY = maxY;
            maxY = j;
        }

        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        a = a * opacity;

        bb.vertex(matrix4f, minX, maxY, 0.0F).color(r, g, b, a).endVertex();
        bb.vertex(matrix4f, maxX, maxY, 0.0F).color(r, g, b, a).endVertex();
        bb.vertex(matrix4f, maxX, minY, 0.0F).color(r, g, b, a).endVertex();
        bb.vertex(matrix4f, minX, minY, 0.0F).color(r, g, b, a).endVertex();

        return this;
    }

    public void set(int x, int y, int color) {

        // Create color
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        bb.vertex(matrix4f, x, y + 1, 0.0F).color(r, g, b, 1F).endVertex();
        bb.vertex(matrix4f, x + 1, y + 1, 0.0F).color(r, g, b, 1F).endVertex();
        bb.vertex(matrix4f, x + 1, y, 0.0F).color(r, g, b, 1F).endVertex();
        bb.vertex(matrix4f, x, y, 0.0F).color(r, g, b, 1F).endVertex();
    }

    public void finishDrawing() {
        bb.end(); //finishDrawing();
        WorldVertexBufferUploader.end(bb);
        RenderSystem.enableTexture();
        // Required for transparency
        RenderSystem.disableBlend();
    }

    public void finishDrawingTest() {
        bb.end();
        WorldVertexBufferUploader.end(bb);
    }
}
