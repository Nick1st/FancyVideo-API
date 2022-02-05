package nick1st.fancyvideo.api;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureUtil;

public class SelfCleaningDynamicTexture extends DynamicTexture {
    public SelfCleaningDynamicTexture(NativeImage nativeImage) {
        super(nativeImage);
    }

    public SelfCleaningDynamicTexture(int p_i48125_1_, int p_i48125_2_, boolean p_i48125_3_) {
        super(p_i48125_1_, p_i48125_2_, p_i48125_3_);
    }

    @Override
    public void setPixels(NativeImage nativeImage) {
        super.setPixels(nativeImage);
        TextureUtil.prepareImage(this.getId(), this.getPixels().getWidth(), this.getPixels().getHeight());
        this.upload();
    }

}
