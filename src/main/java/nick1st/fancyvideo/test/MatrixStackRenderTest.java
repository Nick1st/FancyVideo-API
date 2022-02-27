package nick1st.fancyvideo.test;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import nick1st.fancyvideo.FancyVideoAPI;
import nick1st.fancyvideo.api.MediaPlayer;
import nick1st.fancyvideo.api.MediaPlayers;

public class MatrixStackRenderTest {
    int id;
    boolean init = false;
    int frameNumb = 0;

    public void init() {
        FancyVideoAPI.LOGGER.info("Setting up test media player");
        id = MediaPlayer.getNew();
        //MediaPlayers.getPlayer(id).prepare("DarkDays.mov");
        MediaPlayers.getPlayer(id).prepare("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        //MediaPlayers.getPlayer(id).prepare("ColorTest.mov");
        MediaPlayers.getPlayer(id).volume(200);
    }

    @SubscribeEvent
    public void drawBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        if (e.getGui() instanceof RealmsScreen) {
            if (!init) {
                MediaPlayers.getPlayer(id).playPrepared();
                init = true;
            }
            if (frameNumb == 100) {
                MediaPlayers.removePlayer(id);
                //MediaPlayers.shutdown();
            }
            frameNumb++;
/*            if (frameNumb > 100) {
                return;
            }*/

            // Generic Render Code for Screens
            int width = Minecraft.getInstance().screen.width;
            int height = Minecraft.getInstance().screen.height;

            MediaPlayers.getPlayer(id).bindFrame();
            RenderSystem.enableBlend();
            RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
            AbstractGui.blit(e.getMatrixStack(), 0, 0/*x, y*/, 0.0F, 0.0F, width, height, width, height);
            RenderSystem.disableBlend();
            try {
                MediaPlayers.getPlayer(id).getTrueMediaPlayer();
            } catch (NullPointerException ignored) {

            }
        }
    }
}
