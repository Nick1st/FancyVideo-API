package nick1st.fancyvideo.test;

import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import nick1st.fancyvideo.api.MediaPlayer;
import nick1st.fancyvideo.api.MediaPlayers;

public class MatrixStackRenderTest {
    int id;
    boolean init = false;
    int frameNumb = 0;

    public void init() {
        id = MediaPlayer.newMediaPlayer();
        //MediaPlayers.getPlayer(id).prepare("DarkDays.mov");
        MediaPlayers.getPlayer(id).prepare("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        MediaPlayers.getPlayer(id).volume(200);
    }

    @SubscribeEvent
    public void drawBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        Screen gui = e.getGui();
        if (gui instanceof RealmsGenericErrorScreen) {
            if (!init) {
                MediaPlayers.getPlayer(0).playPrepared();
            }
            if (frameNumb != 0 && 0 == frameNumb % 10000) {
                MediaPlayers.getPlayer(0).pause();
            }
            frameNumb++;
            MediaPlayers.getPlayer(id).render(e.getMatrixStack(), 0, 0);
        }
    }
}
