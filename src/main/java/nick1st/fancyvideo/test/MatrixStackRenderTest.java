/*
 * This file is part of the FancyVideo-API.
 *
 * The FancyVideo-API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The FancyVideo-API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * The FancyVideo-API uses VLCJ, Copyright 2009-2021 Caprica Software Limited,
 * licensed under the GNU General Public License.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You should have received a copy of the GNU General Public License
 * along with FancyVideo-API.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2022 Nick1st.
 */

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

    public void init() {
        FancyVideoAPI.LOGGER.info("Setting up test media player");
        id = MediaPlayer.getNew();
        MediaPlayers.getPlayer(id).prepare("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        MediaPlayers.getPlayer(id).volume(200);
    }

    @SubscribeEvent
    public void drawBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        if (e.getGui() instanceof RealmsScreen) {
            if (!init) {
                MediaPlayers.getPlayer(id).playPrepared();
                init = true;
            }
            // Generic Render Code for Screens
            int width = Minecraft.getInstance().screen.width;
            int height = Minecraft.getInstance().screen.height;

            MediaPlayers.getPlayer(id).bindFrame();
            RenderSystem.enableBlend();
            RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
            AbstractGui.blit(e.getMatrixStack(), 0, 0/*x, y*/, 0.0F, 0.0F, width, height, width, height);
            RenderSystem.disableBlend();
        }
    }
}
