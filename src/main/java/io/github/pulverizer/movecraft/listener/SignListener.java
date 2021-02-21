package io.github.pulverizer.movecraft.listener;

import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.event.CraftDetectEvent;
import io.github.pulverizer.movecraft.event.SignTranslateEvent;
import io.github.pulverizer.movecraft.sign.AntiAircraftDirectorSign;
import io.github.pulverizer.movecraft.sign.AscendSign;
import io.github.pulverizer.movecraft.sign.CannonDirectorSign;
import io.github.pulverizer.movecraft.sign.CommanderSign;
import io.github.pulverizer.movecraft.sign.ContactsSign;
import io.github.pulverizer.movecraft.sign.CraftSign;
import io.github.pulverizer.movecraft.sign.CrewSign;
import io.github.pulverizer.movecraft.sign.CruiseSign;
import io.github.pulverizer.movecraft.sign.DescendSign;
import io.github.pulverizer.movecraft.sign.HelmSign;
import io.github.pulverizer.movecraft.sign.LoaderSign;
import io.github.pulverizer.movecraft.sign.PilotSign;
import io.github.pulverizer.movecraft.sign.RelativeMoveSign;
import io.github.pulverizer.movecraft.sign.ReleaseSign;
import io.github.pulverizer.movecraft.sign.RemoteSign;
import io.github.pulverizer.movecraft.sign.RepairmanSign;
import io.github.pulverizer.movecraft.sign.SpeedSign;
import io.github.pulverizer.movecraft.sign.StaticMoveSign;
import io.github.pulverizer.movecraft.sign.StatusSign;
import io.github.pulverizer.movecraft.sign.SubcraftRotateSign;
import io.github.pulverizer.movecraft.sign.TeleportSign;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.world.World;

public class SignListener {

    @Listener
    @Include({InteractBlockEvent.Primary.class, InteractBlockEvent.Secondary.MainHand.class})
    public final void onSignClick(InteractBlockEvent event, @Root Player player) {

        BlockSnapshot block = event.getTargetBlock();
        if (block.getState().getType() != BlockTypes.STANDING_SIGN && block.getState().getType() != BlockTypes.WALL_SIGN) {
            return;
        }

        AntiAircraftDirectorSign.onSignClick(event, player, block);
        CannonDirectorSign.onSignClick(event, player, block);
        HelmSign.onSignClick(event, player, block);
        PilotSign.onSignClick(event, player, block);
        RemoteSign.onSignClick(event, player, block);
        SubcraftRotateSign.onSignClick(event, player, block);
        RepairmanSign.onSignClick(event, player, block);
        LoaderSign.onSignClick(event, player, block);

        if (event instanceof InteractBlockEvent.Secondary.MainHand) {

            InteractBlockEvent.Secondary.MainHand rightClickEvent = (InteractBlockEvent.Secondary.MainHand) event;

            AscendSign.onSignClick(rightClickEvent, player, block);
            CraftSign.onSignClick(rightClickEvent, player, block);
            CruiseSign.onSignClick(rightClickEvent, player, block);
            DescendSign.onSignClick(rightClickEvent, player, block);
            StaticMoveSign.onSignClick(rightClickEvent, player, block);
            RelativeMoveSign.onSignClick(rightClickEvent, player, block);
            ReleaseSign.onSignClick(rightClickEvent, player, block);
            TeleportSign.onSignClick(rightClickEvent, player, block);

        }

    }

    @Listener
    public final void craftDetection(CraftDetectEvent event) {
        World world = event.getCraft().getWorld();
        HashHitBox hitBox = event.getCraft().getHitBox();

        StatusSign.onCraftDetect(event, world, hitBox);
        SpeedSign.onCraftDetect(event, world, hitBox);
        DescendSign.onCraftDetect(event, world, hitBox);
        AscendSign.onCraftDetect(event, world, hitBox);
        CruiseSign.onCraftDetect(event, world, hitBox);
        ContactsSign.onCraftDetect(event, world, hitBox);

    }

    @Listener
    public final void onSignChange(ChangeSignEvent event, @Root Player player) {

        String header = event.getText().lines().get(0).toPlain().toLowerCase();

        switch (header) {

            case "[helm]":
                HelmSign.onSignChange(event, player);
                break;

            case "commander:":
                CommanderSign.onSignChange(event, player);
                break;

            case "crew:":
                CrewSign.onSignChange(event, player);
                break;

            case "cruise:":
            case "cruise: off":
            case "cruise: on":
                CruiseSign.onSignChange(event, player);
                break;

            case "subcraft rotate":
                SubcraftRotateSign.onSignChange(event, player);
                break;

            case "contacts:":
                ContactsSign.onSignChange(event, player);
                break;

            case "status:":
                StatusSign.onSignChange(event, player);
                break;

            case "speed:":
                SpeedSign.onSignChange(event, player);
                break;

            case "remote sign":
                RemoteSign.onSignChange(event, player);
                break;

            case "move:":
                StaticMoveSign.onSignChange(event, player);
                break;

            case "rmove:":
                RelativeMoveSign.onSignChange(event, player);
                break;

            case "teleport:":
                TeleportSign.onSignChange(event, player);
                break;

            case "ascend:":
            case "ascend: off":
            case "ascend: on":
                AscendSign.onSignChange(event, player);
                break;

            case "descend:":
            case "descend: off":
            case "descend: on":
                DescendSign.onSignChange(event, player);
                break;

            default:
                if (CraftManager.getInstance().getCraftTypeFromString(header) != null) {
                    CraftSign.onSignChange(event, player, header);
                }

                break;
        }
    }

    @Listener
    public final void onSignTranslate(SignTranslateEvent event) {

        Craft craft = event.getCraft();

        if (!event.getWorld().getTileEntity(event.getBlockPosition()).isPresent()) {
            return;
        }

        Sign sign = (Sign) event.getWorld().getTileEntity(event.getBlockPosition()).get();

        String header = sign.lines().get(0).toPlain().toLowerCase();

        switch (header) {

            case "crew:":
                CrewSign.onSignTranslate(event.getWorld(), event.getBlockPosition(), sign);
                break;

            case "status:":
                StatusSign.onSignTranslate(craft, sign);
                break;

            case "speed:":
                SpeedSign.onSignTranslate(craft, sign);
                break;

            case "contacts:":
                ContactsSign.onSignTranslateEvent(craft, sign);
                break;
        }
    }
}