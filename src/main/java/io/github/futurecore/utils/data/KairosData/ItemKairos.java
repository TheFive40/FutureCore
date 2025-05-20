package io.github.futurecore.utils.data.KairosData;

import io.github.futurecore.utils.data.KairosDataHandler.KairosDataHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ItemKairos {
    private String displayName;
    private List<String> lore;
    private int id;

    public ItemKairos ( String displayName, List<String> lore, int id, int key ) {
        this.id = id;
        this.lore = lore;
        this.displayName = displayName;
        if (!KairosDataHandler.itemsKairos.contains ( this )) {
            KairosDataHandler.itemsKairos.put ( key, this );
        }
    }

    @Override
    public boolean equals ( Object object ) {
        if (this == object) return true;
        if (!(object instanceof ItemKairos)) return false;
        ItemKairos that = (ItemKairos) object;
        return getId ( ) == that.getId ( ) && Objects.equals ( getDisplayName ( ), that.getDisplayName ( ) ) && Objects.equals ( getLore ( ), that.getLore ( ) );
    }

    @Override
    public int hashCode () {
        return Objects.hash ( getDisplayName ( ), getLore ( ), getId ( ) );
    }
}
