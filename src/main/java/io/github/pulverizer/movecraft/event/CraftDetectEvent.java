package io.github.pulverizer.movecraft.event;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.text.Text;

public class CraftDetectEvent extends AbstractCraftEvent {

    private final Cause cause;
    private final Player player;

    public CraftDetectEvent(Craft craft, Player player) {
        super(craft);
        this.player = player;

        cause = Cause.builder()
                .append(this)
                .append(player)
                .build(EventContext.empty());

        //TODO - Add more details - eg. is it a Subcraft?
        player.sendMessage(Text.of("Successfully commandeered " + craft.getType().getSetting(Defaults.Name.class).get().getValue() + " Size: " + craft
                .getHitBox().size()));
        Movecraft.getInstance().getLogger()
                .info("New Craft Detected! Commandeered By: " + player.getName() + " CraftType: " + craft.getType().getSetting(
                        Defaults.Name.class).get().getValue() + " Size: " + craft.getHitBox().size() + " Location: " + craft.getHitBox().getMidPoint()
                        .toString());
    }

    @Override
    public Cause getCause() {
        return cause;
    }

    public Player getPlayer() {
        return player;
    }
}