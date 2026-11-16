package quickeat.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the mod see (and swallow) fresh key presses without consuming them. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int quickeat$getClickCount();

    @Accessor("clickCount")
    void quickeat$setClickCount(int value);
}
