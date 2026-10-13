package decade.items.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class StallLookTest {
    @Test
    void roundTrips() {
        StallLook look = new StallLook("triangle", "tartan", "5", "yellow", "check", "blue", true);
        CompoundTag tag = new CompoundTag();
        look.write(tag);
        assertEquals(look, StallLook.read(tag));
    }

    @Test
    void noTagIsAnUnclaimedStall() {
        assertEquals(StallLook.DEFAULT, StallLook.read(new CompoundTag()));
    }

    @Test
    void namesThatCannotBeAPathFallBack() {
        CompoundTag t = new CompoundTag();
        t.putString("roof", "../../evil");
        t.putString("cloth", "Red");
        t.putString("set", "");
        t.putString("carpet", "plain");
        CompoundTag tag = new CompoundTag();
        tag.put(StallLook.TAG, t);
        StallLook look = StallLook.read(tag);
        assertEquals(StallLook.DEFAULT.roof(), look.roof());
        assertEquals(StallLook.DEFAULT.cloth(), look.cloth());
        assertEquals(StallLook.DEFAULT.set(), look.set());
        assertEquals("plain", look.carpet());
    }
}
