package nick1st.vlcjtest;

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
        matrix4f = matrix.getLast().getMatrix();
        bb = Tessellator.getInstance().getBuffer();
        RenderSystem.disableTexture();
        // Required for transparency
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        bb.begin(7, DefaultVertexFormats.POSITION_COLOR);
    }

    public static int[][] extractBytes (String ImageName) throws IOException {
        // open image
        File imgPath = new File(ImageName);
        BufferedImage bufferedImage = ImageIO.read(imgPath);

        int[][] matrix = new int[bufferedImage.getHeight()][bufferedImage.getWidth()];
        for (int i = 0; i < bufferedImage.getHeight(); i++) {
            for (int j = 0; j < bufferedImage.getWidth(); j++) {
                matrix[i][j] = bufferedImage.getRGB(j, i);
            }
        }

        return matrix;
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

        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        float a = (float)(color >> 24 & 255) / 255.0F;

        a = a * opacity;

        bb.pos(matrix4f, minX, maxY, 0.0F).color(r, g, b, a).endVertex();
        bb.pos(matrix4f, maxX, maxY, 0.0F).color(r, g, b, a).endVertex();
        bb.pos(matrix4f, maxX, minY, 0.0F).color(r, g, b, a).endVertex();
        bb.pos(matrix4f, minX, minY, 0.0F).color(r, g, b, a).endVertex();

        return this;
    }

    public void set(float x, float y, int color) {

        // Create color
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        bb.pos(matrix4f, x, y+1, 0.0F).color(r, g, b, 1F).endVertex();
        bb.pos(matrix4f, x+1, y+1, 0.0F).color(r, g, b, 1F).endVertex();
        bb.pos(matrix4f, x+1, y, 0.0F).color(r, g, b, 1F).endVertex();
        bb.pos(matrix4f, x, y, 0.0F).color(r, g, b, 1F).endVertex();
    }

    public void finishDrawing() {
        bb.finishDrawing();
        WorldVertexBufferUploader.draw(bb);
        RenderSystem.enableTexture();
        // Required for transparency
        RenderSystem.disableBlend();
    }
}
